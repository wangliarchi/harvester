package edu.olivet.harvester.fulfill.service;


import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.FailedBuyerAccountAuthenticationException;
import edu.olivet.harvester.fulfill.exception.Exceptions.OutOfBudgetException;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.service.flowcontrol.OrderFlowEngine;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.common.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/9/18 3:15 PM
 */
public class BuyerPanelOrderWorker extends SwingWorker<Void, Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyerPanelOrderWorker.class);
    private final BuyerPanel buyerPanel;
    private final OrderSubmissionTaskService orderSubmissionTaskService;
    private final OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;
    private final Map<String, List<Order>> ordersToProcess = new HashMap<>();
    private final MessageListener messageListener;
    private final OrderValidator orderValidator;
    private final OrderFlowEngine orderFlowEngine;
    private final SheetService sheetService;
    private boolean running = false;


    public BuyerPanelOrderWorker(BuyerPanel buyerPanel) {
        super();
        this.buyerPanel = buyerPanel;
        this.orderSubmissionTaskService = ApplicationContext.getBean(OrderSubmissionTaskService.class);
        this.orderSubmissionBuyerTaskService = ApplicationContext.getBean(OrderSubmissionBuyerTaskService.class);
        messageListener = ApplicationContext.getBean(MessageListener.class);
        orderValidator = ApplicationContext.getBean(OrderValidator.class);
        orderFlowEngine = ApplicationContext.getBean(OrderFlowEngine.class);
        sheetService = ApplicationContext.getBean(SheetService.class);
    }


    private final BlockingQueue<OrderSubmissionBuyerAccountTask> tasks = new ArrayBlockingQueue<>(1);

    /**
     * 获取当前消息队列的概况
     */
    public String status() {
        String status = String.format("Tasks: %s tasks, %s orders", tasks.size(), tasks.stream().mapToInt(it -> it.getTotalOrders()).sum());
        return status;
    }

    public boolean isRunning() {
        return running;
    }

    public void addTask(OrderSubmissionBuyerAccountTask task) {
        try {
            tasks.add(task);
        } catch (IllegalStateException e) {
            //tasks.clear();
        } catch (Exception e) {
            LOGGER.warn("加入任务队列过程中出现其他异常:{}", e.getMessage());
            //tasks.clear();
        }
    }

    @Override
    protected Void doInBackground() {
        Thread.currentThread().setName("ProcessingOrderWithBuyerPanel" + buyerPanel.getKey());
        while (!PSEventListener.stopped()) {
            submitOrders();
        }
        return null;
    }


    private void submitOrders() {
        try {
            //get task from queue
            OrderSubmissionBuyerAccountTask buyerAccountTask = tasks.take();
            running = true;
            List<Order> orders = buyerAccountTask.getOrderList();

            buyerPanel.initProgressBar(orders.size());
            orderSubmissionBuyerTaskService.startTask(buyerAccountTask);
            buyerPanel.updateTasksInfo(status());
            for (Order order : orders) {

                if (PSEventListener.stopped()) {
                    buyerPanel.stop();
                    orderSubmissionBuyerTaskService.stopTask(buyerAccountTask);
                    break;
                }

                while (PSEventListener.paused()) {
                    buyerPanel.paused();
                    WaitTime.Short.execute();
                    continue;
                }

                long start = System.currentTimeMillis();

                try {
                    submit(order);
                    orderSubmissionBuyerTaskService.saveSuccess(buyerAccountTask);
                    messageListener.addMsg(order, "order fulfilled successfully. " + order.basicSuccessRecord() + ", took " + Strings.formatElapsedTime(start));
                } catch (Exception e) {
                    LOGGER.error("Error submit order {}", order.order_id, e);

                    String msg = Strings.parseErrorMsg(e.getMessage());
                    messageListener.addMsg(order, msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
                    sheetService.fillUnsuccessfulMsg(order.spreadsheetId, order, msg);


                    orderSubmissionBuyerTaskService.saveFailed(buyerAccountTask);

                    if (e instanceof OutOfBudgetException) {
                        UITools.error("No more money to spend :(");
                        break;
                    } else if (e instanceof FailedBuyerAccountAuthenticationException) {
                        UITools.error(e.getMessage());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("从任务队列中获取记录时出现异常:", e);
        }

        running = false;
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

            //done
            if (StringUtils.isBlank(order.order_number)) {
                throw new BusinessException("Order number not fouond, reason unknown error");
            }
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
