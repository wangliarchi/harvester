package edu.olivet.harvester.fulfill.service;


import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.SubmitResult;
import edu.olivet.harvester.fulfill.model.SubmitResult.ReturnCode;
import edu.olivet.harvester.fulfill.service.flowcontrol.OrderFlowEngine;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.ui.panel.TasksAndProgressPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.common.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/9/18 3:15 PM
 */
class BuyerPanelOrderWorker extends SwingWorker<Void, SubmitResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyerPanelOrderWorker.class);
    private BuyerPanel buyerPanel = null;
    private final OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;
    private final OrderSubmissionTaskService orderSubmissionTaskService;
    private final MessageListener messageListener;
    private final OrderValidator orderValidator;
    private final OrderFlowEngine orderFlowEngine;
    private final SheetService sheetService;
    private final BlockingQueue<OrderSubmissionBuyerAccountTask> tasks;
    private final List<CountDownLatch> latches;
    private final Country country;
    private final Account buyer;
    private final DailyBudgetHelper dailyBudgetHelper;

    public BuyerPanelOrderWorker(Country country, Account buyer) {
        super();
        this.country = country;
        this.buyer = buyer;
        orderSubmissionBuyerTaskService = ApplicationContext.getBean(OrderSubmissionBuyerTaskService.class);
        orderSubmissionTaskService = ApplicationContext.getBean(OrderSubmissionTaskService.class);
        messageListener = ApplicationContext.getBean(MessageListener.class);
        orderValidator = ApplicationContext.getBean(OrderValidator.class);
        orderFlowEngine = ApplicationContext.getBean(OrderFlowEngine.class);
        sheetService = ApplicationContext.getBean(SheetService.class);
        dailyBudgetHelper = ApplicationContext.getBean(DailyBudgetHelper.class);
        tasks = new LinkedBlockingDeque<>();
        latches = new ArrayList<>();
    }


    /**
     * 获取当前消息队列的概况
     */
    private String status() {
        int totalOrders = tasks.stream().mapToInt(OrderSubmissionBuyerAccountTask::getTotalOrders).sum();
        return tasks.size() == 0 ? "none" :
                String.format("%s task%s, %s order%s", tasks.size(), tasks.size() == 1 ? "" : "s",
                        totalOrders, totalOrders == 1 ? "" : "s");
    }

    public boolean isRunning() {
        return latches.size() > 0;
    }

    public void addTask(OrderSubmissionBuyerAccountTask buyerAccountTask, CountDownLatch latch) {
        try {
            tasks.add(buyerAccountTask);
            latches.add(latch);
            if (buyerPanel != null) {
                buyerPanel.updateTasksInfo(status());
            }
        } catch (Exception e) {
            //UITools.error("加入任务队列过程中出现其他异常: " + e.getMessage());
            LOGGER.warn("加入任务队列过程中出现其他异常:{}", e);
            //tasks.clear();
        }
    }

    @Override
    protected void process(final List<SubmitResult> chunks) {
        chunks.forEach(result -> {
            if (result.getOrder() != null) {
                messageListener.addMsg(result.getOrder(), result.getResult(), result.getCode() == ReturnCode.SUCCESS ? null : InformationLevel.Negative);
                if (result.getCode() == ReturnCode.SUCCESS) {
                    orderSubmissionTaskService.saveSuccess(result.getOrder().getTask());
                } else {
                    orderSubmissionTaskService.saveFailed(result.getOrder().getTask());
                }
            }
        });
        TasksAndProgressPanel.getInstance().loadTasksToTable();
    }

    protected void done() {
        TasksAndProgressPanel.getInstance().loadTasksToTable();
        //
    }

    protected void taskDone() {
        Iterator<CountDownLatch> i = latches.iterator();
        CountDownLatch latch = i.next();
        latch.countDown();
        if (latch.getCount() == 0) {
            i.remove();
        }
    }

    @Override
    protected Void doInBackground() {
        if (buyerPanel == null) {
            buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        }

        Thread.currentThread().setName("ProcessingOrderWithBuyerPanel" + buyerPanel.getKey());


        //noinspection InfiniteLoopStatement
        while (true) {
            WaitTime.Short.execute();
            if (tasks.isEmpty()) {
                return null;
            }

            OrderSubmissionBuyerAccountTask buyerAccountTask;
            try {
                buyerAccountTask = tasks.take();
            } catch (Exception e) {
                LOGGER.warn("从任务队列中获取记录时出现异常:", e);
                taskDone();
                continue;
            }

            buyerPanel.updateTasksInfo(status());


            OrderSubmissionTask task = orderSubmissionTaskService.get(buyerAccountTask.getTaskId());
            dailyBudgetHelper.addRuntimePanelObserver(task.getSpreadsheetId(), buyerPanel);
            //if task stopped
            if (task.stopped() || PSEventListener.stopped()) {
                LOGGER.error("Task stopped");
                taskDone();
                continue;
            }

            orderSubmissionBuyerTaskService.startTask(buyerAccountTask);
            publish(new SubmitResult(null, "Task " + buyerAccountTask.getId() + "started", ReturnCode.SUCCESS));

            TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);
            if (!PSEventListener.isRunning()) {
                PSEventListener.start();
            }

            try {
                submitOrders(buyerAccountTask);
            } catch (Exception e) {
                LOGGER.error("error processing orders", e);
            }
            TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
            taskDone();
        }
    }


    private void submitOrders(OrderSubmissionBuyerAccountTask buyerAccountTask) {
        List<Order> orders = buyerAccountTask.getOrderList();

        buyerPanel.initProgressBar(orders.size());
        orderSubmissionBuyerTaskService.startTask(buyerAccountTask);

        for (Order order : orders) {

            OrderSubmissionTask task = orderSubmissionTaskService.get(buyerAccountTask.getTaskId());
            //if task stopped

            if (PSEventListener.stopped()) {
                buyerPanel.stop();
                orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                publish(new SubmitResult(null, "Task " + buyerAccountTask.getId() + " stopped", ReturnCode.FAILURE));
                break;
            }

            if (task.stopped()) {
                orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                buyerPanel.taskStopped();
                publish(new SubmitResult(null, "Task " + buyerAccountTask.getId() + " stopped", ReturnCode.FAILURE));
                break;
            }

            while (PSEventListener.paused()) {
                buyerPanel.paused();
                WaitTime.Short.execute();
            }

            long start = System.currentTimeMillis();

            order.setTask(task);
            try {
                submit(order);

                if (StringUtils.isNotBlank(order.order_number)) {
                    publish(new SubmitResult(order, "order fulfilled successfully. " + order.basicSuccessRecord() + ", took " + Strings.formatElapsedTime(start), ReturnCode.SUCCESS));
                } else {
                    publish(new SubmitResult(order, "order submission failed. " + " - took " + Strings.formatElapsedTime(start), ReturnCode.FAILURE));
                }

            } catch (Exception e) {
                LOGGER.error("Error submit order {}", order.order_id, e);
                String msg = Strings.parseErrorMsg(e.getMessage());
                publish(new SubmitResult(order, msg + " - took " + Strings.formatElapsedTime(start), ReturnCode.FAILURE));
                try {
                    sheetService.fillUnsuccessfulMsg(order.spreadsheetId, order, msg);
                } catch (Exception ex) {
                    LOGGER.error("Fail to update error message for {} {} {} {}", order.spreadsheetId, order.order_id, order.row, msg);
                }

                if (e instanceof OutOfBudgetException || e instanceof BuyerAccountAuthenticationException) {
                    //UITools.error("No more money to spend :(");
                    //messageListener.addMsg(order, "No more money to spend :(", InformationLevel.Negative);
                    orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                    break;
                }
            }
        }
    }


    /**
     * Submit a single order
     *
     * @param order the order to be submitted
     */

    private void submit(Order order) {
        buyerPanel.setOrder(order);
        try {
            //validate again!
            String error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);
            if (StringUtils.isNotBlank(error)) {
                if (messageListener != null) {
                    messageListener.addMsg(order, error, InformationLevel.Negative);
                }
                return;
            }

            //enable pause button
            buyerPanel.enablePauseButton();

            //start submission process
            orderFlowEngine.process(order, buyerPanel);


        } catch (Exception e) {
            LOGGER.error("", e);
            throw e;
        } finally {
            if (StringUtils.isNotBlank(order.order_number)) {
                ProgressUpdater.success();
                buyerPanel.updateSuccess();
            } else {
                ProgressUpdater.failed();
                buyerPanel.updateFailed();
            }
            buyerPanel.disablePauseButton();
        }
    }
}
