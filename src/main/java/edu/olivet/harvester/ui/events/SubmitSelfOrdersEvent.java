package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.selforder.OrderSubmitter;
import edu.olivet.harvester.selforder.ProductManager;
import edu.olivet.harvester.selforder.StatsManager;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.service.SelfOrderService;
import edu.olivet.harvester.selforder.service.SelfOrderService.OrderAction;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.dialog.SelectSelfOrderDialog;
import edu.olivet.harvester.ui.menu.Actions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class SubmitSelfOrdersEvent implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitSelfOrdersEvent.class);

    @Inject private AppScript appScript;
    @Inject SelfOrderService selfOrderService;
    @Inject OrderSubmitter orderSubmitter;
    @Inject StatsManager statsManager;

    public void postFeedbacks() {
        statsManager.postFeedbacks();
    }

    public void asyncSelfOrderStats() {
        MessagePanel messagePanel = new ProgressDetail(Actions.AsyncSelfOrderStats);
        statsManager.setMessagePanel(messagePanel);
        statsManager.asyncSelfOrderStats();
    }

    public void execute() {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        if (StringUtils.isBlank(spreadsheetId)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }
        List<Spreadsheet> spreadsheets = new ArrayList<>();
        try {
            Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
            spreadsheets.add(spreadsheet);
        } catch (Exception e) {
            UITools.error("Self order spreadsheet is not valid. Please check under Settings->System Settings->Self-Orders");
            return;
        }

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));
        if (dialog.isOk()) {
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());
            spreadsheetId = selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetId();
            List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, sheetNames, OrderAction.Process);

            //remove fulfilled orders
            selfOrders.removeIf(it -> it.fulfilled() ||
                    (StringUtils.isBlank(it.ownerAccountStoreName) && StringUtils.isBlank(it.ownerAccountSellerId)));

            if (CollectionUtils.isEmpty(selfOrders)) {
                UITools.error("No self orders need to be processed.");
                return;
            }

            SelectSelfOrderDialog selectSelfOrderDialog = UITools.setDialogAttr(new SelectSelfOrderDialog(selfOrders), true);

            if (selectSelfOrderDialog.isOk()) {
                selfOrders.removeIf(it -> StringUtils.isEmpty(it.buyerAccountEmail));
                orderSubmitter.submit(selfOrders);
            }
        }
    }

    @Inject ProductManager productManager;

    public void reloadSelfOrderProducts(){
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        if (StringUtils.isBlank(spreadsheetId)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }
        List<Spreadsheet> spreadsheets = new ArrayList<>();
        try {
            Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
            spreadsheets.add(spreadsheet);
        } catch (Exception e) {
            UITools.error("Self order spreadsheet is not valid. Please check under Settings->System Settings->Self-Orders");
            return;
        }

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));
        if (dialog.isOk()) {
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());
            spreadsheetId = selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetId();
            List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, sheetNames, OrderAction.AddProduct);

            if (CollectionUtils.isEmpty(selfOrders)) {
                UITools.error("No self order asins need to be added.");
                return;
            }

            if (UITools.confirmed(selfOrders.size() + " ASIN(s) to be updated. Please confirm to continue.")) {
                productManager.submit(selfOrders);
            }

        }
    }
    public void addProducts() {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        if (StringUtils.isBlank(spreadsheetId)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }
        List<Spreadsheet> spreadsheets = new ArrayList<>();
        try {
            Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
            spreadsheets.add(spreadsheet);
        } catch (Exception e) {
            UITools.error("Self order spreadsheet is not valid. Please check under Settings->System Settings->Self-Orders");
            return;
        }

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));
        if (dialog.isOk()) {
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());
            spreadsheetId = selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetId();
            List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, sheetNames, OrderAction.AddProduct);

            //remove fulfilled orders
            selfOrders.removeIf(SelfOrder::asinAdded);

            if (CollectionUtils.isEmpty(selfOrders)) {
                UITools.error("No self order asins need to be added.");
                return;
            }

            if (UITools.confirmed(selfOrders.size() + " ASIN(s) to be added. Please confirm to continue.")) {
                productManager.submit(selfOrders);
            }

        }
    }
}
