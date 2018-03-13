package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.fulfill.OrderSubmitter;
import edu.olivet.harvester.fulfill.exception.Exceptions.NoBudgetException;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.BuyerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:15 AM
 */
public class SubmitOrdersEvent extends Observable implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitOrdersEvent.class);


    @Inject private
    OrderSubmitter orderSubmitter;

    @Inject DailyBudgetHelper dailyBudgetHelper;
    @Inject OrderSubmissionTaskService orderSubmissionTaskService;
    @Inject OrderDispatcher orderDispatcher;

    public void execute() {

        if (PSEventListener.isRunning()) {
            UITools.error("Other task is running. please resubmit later.");
            return;
        }

        RuntimeSettings settings = RuntimeSettings.load();
        //check prime account
        Account primeBuyerAccount = BuyerAccountSettingUtils.load().getByEmail(settings.getPrimeBuyerAccount()).getBuyerAccount();
        if (!BuyerUtils.isValidPrime(Country.fromCode(settings.getMarketplaceName()), primeBuyerAccount)) {
            if (!UITools.confirmed("Buyer account " + settings.getPrimeBuyerAccount() + " is not a valid prime account. Are you sure to proceed?")) {
                return;
            }
        }

        OrderSubmissionTask task = orderSubmissionTaskService.createFromRuntimeSettings(settings);

        //set progress bar
        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
        //set event listener
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());

        dailyBudgetHelper.addRuntimePanelObserver(settings.getSpreadsheetId(), SimpleOrderSubmissionRuntimePanel.getInstance());
        try {
            orderSubmitter.execute(task);
        } catch (NoBudgetException e) {
            UITools.error(Strings.getExceptionMsg(e));
            orderSubmissionTaskService.hardDeleteTask(task);
        } catch (Exception e) {
            UITools.error(Strings.getExceptionMsg(e));
        }

        new Thread(() -> {
            while (true) {
                if (!orderDispatcher.hasJobRunning()) {
                    if (PSEventListener.stopped()) {
                        orderSubmissionTaskService.cleanUp();
                    }

                    PSEventListener.end();
                    break;
                }
            }
        }).start();
    }


}
