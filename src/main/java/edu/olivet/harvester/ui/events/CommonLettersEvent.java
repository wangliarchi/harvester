package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.letters.CommonLetterSender;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:15 AM
 */
public class CommonLettersEvent extends Observable implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonLettersEvent.class);

    @Inject private
    CommonLetterSender mailer;

    @Inject
    private AppScript appScript;

    public void execute() {
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

            mailer.executeForWorksheets(selectedWorksheets);
        }
    }


}
