package edu.olivet.harvester.ui.events;

import com.amazonaws.mws.model.FeedSubmissionInfo;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.MarkStatusService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.utils.validation.RuntimeSettingsValidator;
import edu.olivet.harvester.hunt.Hunter;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:15 AM
 */
public class HuntSuppliersEvent extends Observable implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuntSuppliersEvent.class);

    @Inject private
    RuntimeSettingsValidator validator;
    @Inject private
    Hunter hunter;

    @Inject
    private AppScript appScript;

    public void execute() {
        if (PSEventListener.isRunning()) {
            UITools.error("Other task is running!");
            return;
        }

        //validate runtime setting
        RuntimeSettings settings = RuntimeSettings.load();
        RuntimeSettingsValidator.CheckResult result = validator.validate(settings, FulfillmentEnum.Action.HuntSupplier);

        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());

        List<String> messages = result.getErrors();
        if (CollectionUtils.isNotEmpty(messages)) {
            UITools.error(StringUtils.join(messages, StringUtils.LF), UIText.title("title.conf_error"));
            return;
        }
        hunter.execute(settings);
    }


    public void run() {

        if (PSEventListener.isRunning()) {
            UITools.error("Other task is running!");
            return;
        }

        long start = System.currentTimeMillis();

        List<Spreadsheet> spreadsheets = new ArrayList<>();

        for (Country country : Settings.load().listAllCountries()) {
            spreadsheets.addAll(Settings.load().listSpreadsheets(country, appScript));
        }

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("No order update sheet found. Please make sure it's configured and shared with " + Constants.RND_EMAIL, "Error");
        }

        LOGGER.info("All spreadsheets loaded in {}", Strings.formatElapsedTime(start));

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));

        if (dialog.isOk()) {
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            Worksheet worksheet = selectedWorksheets.get(0);

            SimpleOrderSubmissionRuntimePanel simpleOrderSubmissionRuntimePanel = SimpleOrderSubmissionRuntimePanel.getInstance();
            simpleOrderSubmissionRuntimePanel.updateSettings(worksheet.getSpreadsheet().getSpreadsheetCountry(), worksheet);

            hunter.huntForWorksheets(selectedWorksheets);
        }
    }


}
