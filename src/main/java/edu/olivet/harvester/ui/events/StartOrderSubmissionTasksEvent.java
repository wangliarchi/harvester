package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.OrderSubmitter;
import edu.olivet.harvester.fulfill.exception.Exceptions.NoBudgetException;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.service.OrderDispatcher;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.ui.panel.TasksAndProgressPanel;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/12/18 1:51 PM
 */
public class StartOrderSubmissionTasksEvent implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartOrderSubmissionTasksEvent.class);
    @Inject private OrderSubmissionTaskService orderSubmissionTaskService;
    @Inject private OrderDispatcher orderDispatcher;

    @Override
    public void execute() {
        TasksAndProgressPanel tasksAndProgressPanel = TasksAndProgressPanel.getInstance();
        new Thread(() -> {
            if (PSEventListener.isRunning()) {
                UITools.error("Other task is running!");
                return;
            }

            PSEventListener.reset(tasksAndProgressPanel);
            //PSEventListener.start();
            //stay listening until it's stopped by user
            while (!PSEventListener.stopped() || orderDispatcher.hasJobRunning()) {
                try {
                    List<OrderSubmissionTask> scheduledTasks = orderSubmissionTaskService.todayScheduledTasks();
                    if (CollectionUtils.isNotEmpty(scheduledTasks)) {
                        OrderSubmissionTask task = scheduledTasks.get(0);
                        ApplicationContext.getBean(OrderSubmitter.class).execute(task);
                    } else if (!orderDispatcher.hasJobRunning()) {
                        break;
                    }
                    WaitTime.Normal.execute();
                } catch (NoBudgetException e) {
                    LOGGER.error("", e);
                    UITools.error(Strings.getExceptionMsg(e));
                    if (e.getTask() != null) {
                        e.getTask().setStatus(OrderTaskStatus.Scheduled.name());
                        orderSubmissionTaskService.saveTask(e.getTask());
                    }
                    break;
                } catch (Exception e) {
                    LOGGER.error("", e);
                    UITools.error(Strings.getExceptionMsg(e));
                    break;
                }
            }

            if (PSEventListener.stopped()) {
                orderSubmissionTaskService.cleanUp();
            }
            WaitTime.Short.execute();
            TasksAndProgressPanel.getInstance().loadTasksToTable();
            PSEventListener.end();
        }).start();
    }


}
