package edu.olivet.harvester.fulfill.service;


import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/9/18 3:15 PM
 */
class BuyerPanelOrderWorker extends SwingWorker<Void, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyerPanelOrderWorker.class);
    private final BuyerPanel buyerPanel;
    private final OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;
    private final OrderSubmissionTaskService orderSubmissionTaskService;
    private final MessageListener messageListener;
    private final OrderValidator orderValidator;
    private final OrderFlowEngine orderFlowEngine;
    private final SheetService sheetService;
    private BlockingQueue<OrderSubmissionBuyerAccountTask> tasks;


    public BuyerPanelOrderWorker(BuyerPanel buyerPanel) {
        super();
        this.buyerPanel = buyerPanel;
        orderSubmissionBuyerTaskService = ApplicationContext.getBean(OrderSubmissionBuyerTaskService.class);
        orderSubmissionTaskService = ApplicationContext.getBean(OrderSubmissionTaskService.class);
        messageListener = ApplicationContext.getBean(MessageListener.class);
        orderValidator = ApplicationContext.getBean(OrderValidator.class);
        orderFlowEngine = ApplicationContext.getBean(OrderFlowEngine.class);
        sheetService = ApplicationContext.getBean(SheetService.class);
        tasks = new LinkedBlockingDeque<>();
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
        return tasks.size() > 0 || running;
    }

    public void addTask(OrderSubmissionBuyerAccountTask buyerAccountTask) {
        try {
            tasks.add(buyerAccountTask);
            buyerPanel.updateTasksInfo(status());
        } catch (Exception e) {
            //UITools.error("加入任务队列过程中出现其他异常: " + e.getMessage());
            LOGGER.warn("加入任务队列过程中出现其他异常:{}", e);
            //tasks.clear();
        }
    }

    private boolean running = false;

    @Override
    protected void process(final List<String> chunks) {
        TasksAndProgressPanel.getInstance().loadTasksToTable();
    }

    @Override
    protected Void doInBackground() {
        Thread.currentThread().setName("ProcessingOrderWithBuyerPanel" + buyerPanel.getKey());
        //noinspection InfiniteLoopStatement
        while (true) {
            WaitTime.Short.execute();

            OrderSubmissionBuyerAccountTask buyerAccountTask;
            try {
                buyerAccountTask = tasks.take();
            } catch (Exception e) {
                LOGGER.warn("从任务队列中获取记录时出现异常:", e);
                continue;
            }

            buyerPanel.updateTasksInfo(status());

            OrderSubmissionTask task = orderSubmissionTaskService.get(buyerAccountTask.getTaskId());

            //if task stopped
            if (task.stopped() || PSEventListener.stopped()) {
                LOGGER.error("Task stopped");
                continue;
            }

            orderSubmissionBuyerTaskService.startTask(buyerAccountTask);
            publish("Task " + buyerAccountTask.getId() + "started");
            TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);

            running = true;
            try {
                submitOrders(buyerAccountTask);
            } catch (Exception e) {
                LOGGER.error("error processing orders", e);
            }
            TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
            running = false;
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
                publish("Task stopped");
                break;
            }

            if (task.stopped()) {
                orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                buyerPanel.taskStopped();
                publish("Task stopped");
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
            } catch (Exception e) {
                LOGGER.error("Error submit order {}", order.order_id, e);

                String msg = Strings.parseErrorMsg(e.getMessage());
                try {
                    messageListener.addMsg(order, msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
                    sheetService.fillUnsuccessfulMsg(order.spreadsheetId, order, msg);
                } catch (Exception ex) {
                    LOGGER.error("Fail to update error message for {} {} {} {}", order.spreadsheetId, order.order_id, order.row, msg);
                }

                if (e instanceof OutOfBudgetException) {
                    //UITools.error("No more money to spend :(");
                    messageListener.addMsg(order, "No more money to spend :(", InformationLevel.Negative);
                    orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                    break;
                } else if (e instanceof BuyerAccountAuthenticationException) {
                    messageListener.addMsg(order, e.getMessage(), InformationLevel.Negative);
                    orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                    break;
                }
            } finally {
                if (StringUtils.isNotBlank(order.order_number)) {
                    orderSubmissionBuyerTaskService.saveSuccess(buyerAccountTask);
                    messageListener.addMsg(order,
                            "order fulfilled successfully. " + order.basicSuccessRecord() + ", took " + Strings.formatElapsedTime(start));
                } else {
                    orderSubmissionBuyerTaskService.saveFailed(buyerAccountTask);
                }
                publish(order.order_id + " - " + order.sheetName + " " + order.row + " done");
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
