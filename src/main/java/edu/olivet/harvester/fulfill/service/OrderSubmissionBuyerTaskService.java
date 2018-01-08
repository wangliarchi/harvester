package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/16/2017 7:38 AM
 */
@Singleton
public class OrderSubmissionBuyerTaskService {
    @Inject
    DBManager dbManager;

    public List<OrderSubmissionBuyerAccountTask> todayTasks() {
        return dbManager.query(OrderSubmissionBuyerAccountTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .asc("dateCreated"));
    }

    public List<OrderSubmissionBuyerAccountTask> todayScheduledTasks() {
        return dbManager.query(OrderSubmissionBuyerAccountTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .and("status", "=", OrderTaskStatus.Scheduled.name())
                        .asc("dateCreated"));
    }

    public List<OrderSubmissionBuyerAccountTask> listAllTasks() {
        return dbManager.query(OrderSubmissionBuyerAccountTask.class,
                Cnd.where("dateCreated", "NOT IS", null)
                        .desc("dateCreated"));
    }

    public void saveTask(OrderSubmissionBuyerAccountTask task) {
        if (StringUtils.isBlank(task.getId())) {
            task.setDateCreated(new Date());
            task.setId(task.getTaskId() + "-" + task.getFulfillmentCountry() + "-" + task.getBuyerAccount());
            task.setStatus(OrderTaskStatus.Scheduled.name());
        }

        dbManager.insertOrUpdate(task, OrderSubmissionBuyerAccountTask.class);
        //TasksAndProgressPanel.getInstance().loadTasksToTable();
    }

    public void startTask(OrderSubmissionBuyerAccountTask task) {
        task.setStatus(OrderTaskStatus.Processing.name());
        task.setDateStarted(new Date());
        saveTask(task);
    }

    public void stopTask(OrderSubmissionBuyerAccountTask task) {
        task.setStatus(OrderTaskStatus.Stopped.name());
        task.setDateEnded(new Date());
        saveTask(task);
    }

    public void completed(OrderSubmissionBuyerAccountTask task) {
        task.setStatus(OrderTaskStatus.Completed.name());
        task.setDateEnded(new Date());
        saveTask(task);
    }

    public List<OrderSubmissionBuyerAccountTask> getTasksById(String taskId) {
        return dbManager.query(OrderSubmissionBuyerAccountTask.class,
                Cnd.where("taskID", "=", taskId)
                        .asc("dateCreated"));
    }

    public void deleteByTaskId(String taskId) {
        getTasksById(taskId).forEach(task -> {
            task.setStatus(OrderTaskStatus.Deleted.name());
            dbManager.insertOrUpdate(task, OrderSubmissionBuyerAccountTask.class);
        });
    }

    public void stopByTaskId(String taskId) {
        getTasksById(taskId).forEach(task -> {
            task.setStatus(OrderTaskStatus.Stopped.name());
            dbManager.insertOrUpdate(task, OrderSubmissionBuyerAccountTask.class);
        });
    }

}
