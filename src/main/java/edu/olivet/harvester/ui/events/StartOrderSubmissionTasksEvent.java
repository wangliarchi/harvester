package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.OrderSubmitter;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.service.OrderDispatcher;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.ui.panel.ProgressBarPanel;
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
    @Inject OrderSubmissionTaskService orderSubmissionTaskService;

    @Override
    public void execute() {
        TasksAndProgressPanel tasksAndProgressPanel = TasksAndProgressPanel.getInstance();
        new Thread(() -> {
            if (PSEventListener.isRunning()) {
                UITools.error("Other task is running!");
                return;
            }

            tasksAndProgressPanel.disableStartButton();
            PSEventListener.reset(tasksAndProgressPanel);
            PSEventListener.start();
            ProgressUpdater.setProgressBarComponent(ProgressBarPanel.getInstance().progressBar,
                ProgressBarPanel.getInstance().progressTextLabel);

            while (!PSEventListener.stopped()) {
                try {
                    List<OrderSubmissionTask> scheduledTasks = orderSubmissionTaskService.todayScheduledTasks();
                    if (CollectionUtils.isEmpty(scheduledTasks) && !OrderDispatcher.getInstance().hasJobRunning()) {
                        //WaitTime.Short.execute();
                        break;
                    }

                    if (CollectionUtils.isNotEmpty(scheduledTasks)) {
                        OrderSubmissionTask task = scheduledTasks.get(0);
                        ApplicationContext.getBean(OrderSubmitter.class).execute(task);
                    }
                } catch (Exception e) {
                    LOGGER.error("", e);
                    UITools.error(e.getMessage());
                    break;
                }
            }

            tasksAndProgressPanel.enableStartButton();
            if (!PSEventListener.stopped()) {
                PSEventListener.end();
            }
        }).start();
    }


}
