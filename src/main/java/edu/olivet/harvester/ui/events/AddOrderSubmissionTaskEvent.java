package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTaskHandler;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.ui.dialog.AddOrderSubmissionTaskDialog;
import edu.olivet.harvester.ui.panel.TasksAndProgressPanel;
import edu.olivet.harvester.utils.BuyerUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/12/17 12:14 PM
 */
public class AddOrderSubmissionTaskEvent extends Observable implements HarvesterUIEvent, OrderSubmissionTaskHandler {
    //private static final Logger LOGGER = LoggerFactory.getLogger(AddOrderSubmissionTaskEvent.class);

    @Inject private
    OrderSubmissionTaskService orderSubmissionTaskService;

    @Override
    public void execute() {
        UITools.setDialogAttr(new AddOrderSubmissionTaskDialog(this));
    }

    @Override
    public void saveTasks(List<OrderSubmissionTask> tasks) {

        List<Account> checkedAccounts = new ArrayList<>();
        //check prime account
        for (OrderSubmissionTask task : tasks) {
            Account primeBuyerAccount = BuyerAccountSettingUtils.load().getByEmail(task.getPrimeBuyerAccount()).getBuyerAccount();
            if(!checkedAccounts.contains(primeBuyerAccount)) {
                checkedAccounts.add(primeBuyerAccount);
                if (!BuyerUtils.isValidPrime(Country.fromCode(task.getMarketplaceName()), primeBuyerAccount)) {
                    if (!UITools.confirmed("Buyer account " + task.getPrimeBuyerAccount() + " is not a valid prime account. Are you sure to proceed?")) {
                        return;
                    }
                }
            }
        }

        tasks.forEach(it -> orderSubmissionTaskService.saveTask(it));

        UITools.info(tasks.size() + " task(s) been created. Please wait a moment for title check.");
        if (CollectionUtils.isNotEmpty(tasks)) {
            orderSubmissionTaskService.checkTitle(tasks);
        }

        TasksAndProgressPanel.getInstance().loadTasksToTable();

    }


}
