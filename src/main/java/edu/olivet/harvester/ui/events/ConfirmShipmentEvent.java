package edu.olivet.harvester.ui.events;

import com.amazonaws.mws.model.FeedSubmissionInfo;
import com.google.inject.Inject;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.ChooseSheetDialog;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class ConfirmShipmentEvent implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmShipmentEvent.class);

    @Inject
    private ConfirmShipments confirmShipments;
    @Inject
    private AppScript appScript;

    public void excute() {
        long start = System.currentTimeMillis();

        LOGGER.info("Confirm shipment button clicked");

        List<String> spreadsheetIds = Settings.load().listAllSpreadsheets();
        List<edu.olivet.harvester.spreadsheet.Spreadsheet> spreadsheets = new ArrayList<>();

        StringBuilder spreadsheetIdError = new StringBuilder();
        for (String spreadsheetId : spreadsheetIds) {
            try {
                Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
                spreadsheets.add(spreadsheet);
            } catch (Exception e) {
                LOGGER.error("{} is invalid. {}", spreadsheetId, e.getMessage());
                spreadsheetIdError.append(String.format("%s is not a valid spreadsheet id, or it's not shared to %s \n",
                        spreadsheetId, Constants.RND_EMAIL));
            }
        }

        if (!spreadsheetIdError.toString().isEmpty()) {
            UITools.error(spreadsheetIdError.toString(), "Error");
        }

        LOGGER.info("All spreadsheets loaded in {}", Strings.formatElapsedTime(start));

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));

        if (dialog.isOk()) {


            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());

            try {
                List<FeedSubmissionInfo> submissionInfo = confirmShipments.getUnprocessedFeedSubmission(selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetCountry());


                if (submissionInfo.size() > 0) {
                    StringBuilder submissions = new StringBuilder();
                    submissionInfo.forEach(it -> submissions.append(String.format("FeedSubmissionId %s submitted at %s, current status %s \n", it.getFeedSubmissionId(), it.getSubmittedDate(), it.getFeedProcessingStatus())));
                    String msg = String.format("Unprocessed/processing order confirmation feed(s) found.  \n\n %s \n\n" +
                            " Are you sure to submit again?", submissions.toString());
                    if (!UITools.confirmed(msg)) {
                        return;
                    }

                }
            } catch (Exception e) {
                LOGGER.error("Failed to load unprocessed feed submissions for {} - {}", selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetCountry(), e.getMessage());
            }

            confirmShipments.setMessagePanel(new ProgressDetail(Actions.ConfirmShipment));


            confirmShipments.getMessagePanel().displayMsg(
                    selectedWorksheets.size() + " worksheets from " + selectedWorksheets.get(0).getSpreadsheet().getTitle() +
                            " selected to confirm shipments - " +
                            String.join(",", sheetNames), LOGGER, InformationLevel.Information);


            confirmShipments.confirmShipmentForWorksheets(selectedWorksheets);
        }
    }
}
