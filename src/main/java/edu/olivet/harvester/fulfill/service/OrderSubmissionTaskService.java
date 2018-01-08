package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.ui.panel.TasksAndProgressPanel;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/16/2017 7:38 AM
 */
@Singleton
public class OrderSubmissionTaskService {
    @Inject
    DBManager dbManager;

    public List<OrderSubmissionTask> todayTasks() {
        return dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .asc("dateCreated"));
    }

    public List<OrderSubmissionTask> todayScheduledTasks() {
        return dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .and("status", "=", OrderTaskStatus.Scheduled.name())
                        .asc("dateCreated"));
    }

    public List<OrderSubmissionTask> listAllTasks() {
        return dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", "NOT IS", null)
                        .desc("dateCreated"));
    }

    public void saveTask(OrderSubmissionTask task) {
        if (StringUtils.isBlank(task.getId())) {
            task.setDateCreated(new Date());
            task.setId(DigestUtils.sha256Hex(task.toString()));
        }

        dbManager.insertOrUpdate(task, OrderSubmissionTask.class);
        TasksAndProgressPanel.getInstance().loadTasksToTable();
    }

    @Inject
    OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;

    public void deleteTask(OrderSubmissionTask task) {
        task.setStatus(OrderTaskStatus.Deleted.name());
        saveTask(task);
        //delete buyer tasks as well
        orderSubmissionBuyerTaskService.deleteByTaskId(task.getId());
    }

    public void startTask(OrderSubmissionTask task) {
        task.setStatus(OrderTaskStatus.Processing.name());
        task.setDateStarted(new Date());
        saveTask(task);
    }

    public void stopTask(OrderSubmissionTask task) {
        task.setStatus(OrderTaskStatus.Stopped.name());
        task.setDateEnded(new Date());
        saveTask(task);

        orderSubmissionBuyerTaskService.stopByTaskId(task.getId());
    }

    public void completed(OrderSubmissionTask task) {
        task.setStatus(OrderTaskStatus.Completed.name());
        task.setDateEnded(new Date());
        saveTask(task);
    }

}
