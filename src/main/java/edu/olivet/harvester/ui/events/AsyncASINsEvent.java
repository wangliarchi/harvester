package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.feeds.StockUpdator;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class AsyncASINsEvent implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncASINsEvent.class);

    @Inject
    private StockUpdator stockUpdator;
    @Inject
    private AppScript appScript;

    public void execute() {


        List<Spreadsheet> spreadsheets = new ArrayList<>();

        for (Country country : Settings.load().listAllCountries()) {
            spreadsheets.addAll(Settings.load().listSpreadsheets(country, appScript));
        }

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("No order update sheet found. Please make sure it's configured and shared with " + Constants.RND_EMAIL, "Error");
        }

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));

        if (dialog.isOk()) {

            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());

            stockUpdator.setMessagePanel(new ProgressDetail(Actions.SyncASINs));

            stockUpdator.getMessagePanel().displayMsg(
                    selectedWorksheets.size() + " worksheet(s) from " + selectedWorksheets.get(0).getSpreadsheet().getTitle() +
                            " selected to async asins - " +
                            String.join(",", sheetNames), LOGGER, InformationLevel.Information);

            stockUpdator.asyncASINsForWorksheets(selectedWorksheets);
        }
    }
}
