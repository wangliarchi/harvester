package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class TitleCheckerEvent implements HarvesterUIEvent {

    @Inject
    private AppScript appScript;
    @Inject private OrderService orderService;

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
            MessagePanel messagePanel = new ProgressDetail(Actions.TitleChecker);
            long start = System.currentTimeMillis();
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            String spreadsheetId = selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetId();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());

            messagePanel.displayMsg("Start to check titles for sheets " + sheetNames + " from " + selectedWorksheets.get(0).getSpreadsheet().getTitle());
            List<Order> orders = orderService.fetchOrders(spreadsheetId, sheetNames);
            messagePanel.displayMsg(orders.size() + " orders found.");
            if (CollectionUtils.isNotEmpty(orders)) {
                messagePanel.addMsgSeparator();
                List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(orders);
                for (ItemCompareResult result : results) {
                    messagePanel.displayMsg(result.desc(), result.isPreCheckPass() ? InformationLevel.Information : InformationLevel.Negative);
                }

            }
            messagePanel.addMsgSeparator();
            messagePanel.wrapLineMsg("Finished title check for " + orders.size() + " items, took " + Strings.formatElapsedTime(start));
        }
    }
}
