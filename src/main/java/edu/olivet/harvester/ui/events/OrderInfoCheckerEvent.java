package edu.olivet.harvester.ui.events;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;

import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.export.model.OrderExportParams;

import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.ChooseMarketplaceDialog;

import edu.olivet.harvester.utils.Settings;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;

import java.time.LocalDate;
import java.time.LocalTime;

import java.util.Date;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class OrderInfoCheckerEvent implements HarvesterUIEvent {

    @Inject DBManager dbManager;
    @Inject private OrderService orderService;
    @Inject private SheetAPI sheetAPI;
    @Inject SheetService sheetService;

    public void execute() {

        ChooseMarketplaceDialog dialog = new ChooseMarketplaceDialog();
        final LocalDate today = LocalDate.now();
        dialog.getFromDateTimePicker().datePicker.setDate(today.minusDays(45));
        dialog.getFromDateTimePicker().timePicker.setTime(LocalTime.of(0, 0));
        dialog.getToDateTimePicker().datePicker.setDate(today);
        dialog.getToDateTimePicker().timePicker.setTime(LocalTime.of(0, 0));
        UITools.setDialogAttr(dialog);

        if (dialog.isOk()) {
            MessagePanel messagePanel = new ProgressDetail(Actions.TitleChecker);

            OrderExportParams orderExportParams = dialog.getOrderExportParams();

            orderExportParams.getMarketplaces().forEach(country -> {
                long start = System.currentTimeMillis();
                messagePanel.displayMsg("Start to check orders from " + country);
                List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();
                spreadsheetIds.forEach(spreadsheetId -> {
                    Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(spreadsheetId);
                    messagePanel.displayMsg("Start to check orders from " + spreadsheet.getProperties().getTitle());
                    List<Order> orders = orderService.fetchOrders(spreadsheet, Range.between(orderExportParams.getFromDate(), orderExportParams.getToDate()));

                    if (CollectionUtils.isEmpty(orders)) {
                        messagePanel.displayMsg("No orders found from " + spreadsheet.getProperties().getTitle(), InformationLevel.Negative);
                        return;
                    }

                    orders.forEach(order -> {
                        try {
                            Date orderDate = order.getPurchaseDate();
                            if (orderDate.before(orderExportParams.getFromDate()) || orderDate.after(orderExportParams.getToDate())) {
                                //messagePanel.displayMsg(order.sheetName + " row " + order.row + " order " + order.order_id + "  order date " + order.purchase_date + " not in requested date range",
                                //        InformationLevel.Negative);
                                return;
                            }
                        } catch (Exception e) {
                            messagePanel.displayMsg(order.sheetName + " row " + order.row + " order " + order.order_id + " unknown order date " + order.purchase_date,
                                    InformationLevel.Negative);
                            return;
                        }

                        if (fulfillmentInfoCorrupted(order)) {
                            messagePanel.displayMsg(order.sheetName + " row " + order.row + " order " + order.order_id + " fulfillment info corrupted. " +
                                            "status: " + order.status + ", " + "cost: " + order.cost + ", " + "order number: " + order.order_number + ", ",
                                    InformationLevel.Negative);

                            List<OrderFulfillmentRecord> list = dbManager.query(OrderFulfillmentRecord.class,
                                    Cnd.where("orderId", "=", order.order_id)
                                            .and("sku", "=", order.sku)
                                            .and("seller", "=", order.seller)
                                            .and("quantityPurchased", "=", order.quantity_purchased));
                            if (CollectionUtils.isNotEmpty(list)) {
                                OrderFulfillmentRecord record = list.get(0);
                                if (StringUtils.isBlank(order.cost)) {
                                    order.cost = record.getCost();
                                }
                                if (StringUtils.isBlank(order.order_number)) {
                                    order.order_number = record.getOrderNumber();
                                }
                                sheetService.fillFulfillmentOrderInfo(spreadsheetId, order);

                                messagePanel.displayMsg("order info updated " +
                                                "status: " + order.status + ", " + "cost: " + order.cost + ", " + "order number: " + order.order_number + ", ",
                                        InformationLevel.Positive);
                            }
                        }
                    });

                    messagePanel.displayMsg("Finished checking orders from " + spreadsheet.getProperties().getTitle() +
                            ", took " + Strings.formatElapsedTime(start));

                });

            });

        }
    }

    public boolean fulfillmentInfoCorrupted(Order order) {
        if (order.colorIsGray()) {
            return false;
        }

        if (order.selfBuy()) {
            return false;
        }
        if (Strings.containsAnyIgnoreCase(order.remark, "canceled", "cancelled", "emailed","ebay","refund") ||
                Strings.containsAnyIgnoreCase(order.cost, "canceled", "cancelled", "emailed","refund") ||
                Strings.containsAnyIgnoreCase(order.order_number, "canceled", "cancelled", "emailed","refund")) {
            return false;
        }

        return StringUtils.containsIgnoreCase(order.status, "fi") &&
                !StringUtils.containsIgnoreCase(order.status, "移表") &&
                (StringUtils.isBlank(order.cost) || StringUtils.isBlank(order.order_number));
    }
}
