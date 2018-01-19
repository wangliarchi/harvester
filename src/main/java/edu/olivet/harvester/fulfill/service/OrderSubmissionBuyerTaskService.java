package edu.olivet.harvester.fulfill.service;

import com.alibaba.fastjson.JSON;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.common.model.Order;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/16/2017 7:38 AM
 */
@Singleton
public class OrderSubmissionBuyerTaskService {
    @Inject private
    DBManager dbManager;

    @Inject private
    OrderSubmissionTaskService orderSubmissionTaskService;


    public void saveTask(OrderSubmissionBuyerAccountTask task) {
        if (StringUtils.isBlank(task.getId())) {
            task.setDateCreated(new Date());
            task.setId(task.getTaskId() + "-" + task.getFulfillmentCountry() + "-" + task.getBuyerAccount());
            task.setTaskStatus(OrderTaskStatus.Scheduled);
        }


        if (task.getTotalOrders() > 0 && task.getSuccess() + task.getFailed() == task.getTotalOrders() &&
                task.taskStatus() != OrderTaskStatus.Completed) {
            task.setTaskStatus(OrderTaskStatus.Completed);
            task.setDateEnded(new Date());
        }


        dbManager.insertOrUpdate(task, OrderSubmissionBuyerAccountTask.class);
        //TasksAndProgressPanel.getInstance().loadTasksToTable();
    }

    public void saveSuccess(OrderSubmissionBuyerAccountTask task) {
        task = get(task.getId());
        task.setSuccess(task.getSuccess() + 1);
        saveTask(task);

        orderSubmissionTaskService.saveSuccess(task.getTaskId());
    }

    public void saveFailed(OrderSubmissionBuyerAccountTask task) {
        task = get(task.getId());
        task.setFailed(task.getFailed() + 1);
        saveTask(task);

        orderSubmissionTaskService.saveFailed(task.getTaskId());
    }

    public OrderSubmissionBuyerAccountTask get(String id) {
        return dbManager.readById(id, OrderSubmissionBuyerAccountTask.class);
    }

    public void startTask(OrderSubmissionBuyerAccountTask task) {
        task.setTaskStatus(OrderTaskStatus.Processing);
        task.setDateStarted(new Date());
        saveTask(task);

        orderSubmissionTaskService.startTask(task.getTaskId());
    }

    public void stopTask(OrderSubmissionBuyerAccountTask task) {
        task.setTaskStatus(OrderTaskStatus.Stopped);
        task.setDateEnded(new Date());
        saveTask(task);

        orderSubmissionTaskService.stopTask(task.getTaskId());
    }

    public void completed(OrderSubmissionBuyerAccountTask task) {
        task.setTaskStatus(OrderTaskStatus.Completed);
        task.setDateEnded(new Date());
        saveTask(task);
    }

    public OrderSubmissionBuyerAccountTask create(Country country, Account buyer, OrderSubmissionTask task, List<Order> orders) {
        OrderSubmissionBuyerAccountTask orderSubmissionBuyerAccountTask = new OrderSubmissionBuyerAccountTask();
        orderSubmissionBuyerAccountTask.setBuyerAccount(buyer.getEmail());
        orderSubmissionBuyerAccountTask.setFulfillmentCountry(country.name());
        orderSubmissionBuyerAccountTask.setTaskId(task.getId());
        orderSubmissionBuyerAccountTask.setMarketplaceName(task.getMarketplaceName());
        orderSubmissionBuyerAccountTask.setSpreadsheetId(task.getSpreadsheetId());
        orderSubmissionBuyerAccountTask.setSpreadsheetName(task.getSpreadsheetName());
        orderSubmissionBuyerAccountTask.setSheetName(task.getOrderRange().getSheetName());
        orderSubmissionBuyerAccountTask.setOrders(JSON.toJSONString(orders));
        orderSubmissionBuyerAccountTask.setTotalOrders(orders.size());
        saveTask(orderSubmissionBuyerAccountTask);

        return orderSubmissionBuyerAccountTask;
    }

    public List<OrderSubmissionBuyerAccountTask> getTasksById(String taskId) {
        return dbManager.query(OrderSubmissionBuyerAccountTask.class,
                Cnd.where("taskID", "=", taskId)
                        .asc("dateCreated"));
    }

    public void deleteByTaskId(String taskId) {
        getTasksById(taskId).forEach(task -> {
            task.setTaskStatus(OrderTaskStatus.Deleted);
            dbManager.insertOrUpdate(task, OrderSubmissionBuyerAccountTask.class);
        });
    }

    public void stopByTaskId(String taskId) {
        getTasksById(taskId).forEach(task -> {
            task.setTaskStatus(OrderTaskStatus.Stopped);
            dbManager.insertOrUpdate(task, OrderSubmissionBuyerAccountTask.class);
        });
    }

}
