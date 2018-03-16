package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.ui.menu.Actions;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 11:05 AM
 */
public class ListOrderSubmissionTasks implements HarvesterUIEvent {

    @Inject
    private OrderSubmissionTaskService orderSubmissionTaskService;

    @Override
    public void execute() {
        List<OrderSubmissionTask> list = orderSubmissionTaskService.listAllTasks();
        ListModel<OrderSubmissionTask> dialog = new ListModel<>(Actions.OrderSubmissionTasks.label(), list, OrderSubmissionTask.COLUMNS, null, OrderSubmissionTask.WIDTHS);
        UITools.displayListDialog(dialog);
    }
}
