package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.finance.model.Refund;
import edu.olivet.harvester.selforder.RefundOrder;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 5/10/2018 3:38 PM
 */
public class RefundOrderEvent implements HarvesterUIEvent{
    private static final Logger LOGGER = LoggerFactory.getLogger(RefundOrderEvent.class);

    @Inject
    private RefundOrder refundOrder;
    @Inject
    private AppScript appScript;

    public void execute(){
        LOGGER.info("Refund Order Button Clicked.");

        List<String> spreadsheetIds = Settings.load().listAllSpreadsheets();
        List<Spreadsheet> spreadsheets = new ArrayList<>();

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

        //LOGGER.info("All spreadsheets loaded in {}", Strings.formatElapsedTime(start));

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));


        if (dialog.isOk()) {

            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());

            refundOrder.setMessagePanel(new ProgressDetail(Actions.RefundOrder));

            refundOrder.getMessagePanel().displayMsg(
                    selectedWorksheets.size() + " worksheets from " + selectedWorksheets.get(0).getSpreadsheet().getTitle() +
                            " selected to refund orders - " +
                            String.join(",", sheetNames), LOGGER, InformationLevel.Information);


            refundOrder.refundOrderForWorksheets(selectedWorksheets);
        }


    }




















}
