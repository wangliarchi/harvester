package edu.olivet.harvester.letters;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Or;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.Hunter;
import edu.olivet.harvester.hunt.model.HuntResult;
import edu.olivet.harvester.hunt.model.HuntResult.ReturnCode;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.HuntService;
import edu.olivet.harvester.hunt.service.SheetService;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.letters.service.ASCLetterSender;
import edu.olivet.harvester.letters.service.GrayLetterRule;
import edu.olivet.harvester.letters.service.GrayLetterRule.GrayRule;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.Actions;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:08 AM
 */
public class Mailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mailer.class);
    private static final int MAX_DAYS = 7;

    @Inject SheetAPI sheetAPI;
    @Inject OrderService orderService;
    @Inject AppScript appScript;
    @Inject ASCLetterSender ascLetterSender;
    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    public void execute(String spreadsheetId) {
        Date minDate = DateUtils.addDays(new Date(), -MAX_DAYS);
        Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(spreadsheetId);

        List<Order> orders = orderService.fetchOrders(spreadsheet, minDate);
        handleOrders(orders);
    }


    public void executeForWorksheets(List<Worksheet> worksheets) {
        while (PSEventListener.isRunning()) {
            UITools.error("Other task is running, please try later.");
            return;
        }

        messagePanel = new ProgressDetail(Actions.FindSupplier);
        for (Worksheet worksheet : worksheets) {
            try {
                while (PSEventListener.isRunning()) {
                    WaitTime.Short.execute();
                }
                executeForWorksheet(worksheet);
            } catch (Exception e) {
                LOGGER.error("Error when hunting sellers for {} - ", worksheet, e);
            }
        }
    }

    public void executeForWorksheet(Worksheet worksheet) {
        messagePanel.addMsgSeparator();
        messagePanel.displayMsg("Send gray label letters for " + worksheet);
        List<Order> orders = appScript.readOrders(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName());

        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No orders found.", InformationLevel.Negative);
            return;
        }

        handleOrders(orders);
    }


    public void handleOrders(List<Order> orders) {
        while (PSEventListener.isRunning()) {
            WaitTime.Short.execute();
        }

        orders.removeIf(it -> GrayLetterRule.getGrayRule(it) == GrayRule.None);

        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No orders need to send gray letters", InformationLevel.Negative);
            return;
        }

        messagePanel.displayMsg(orders.size() + " order(s) to be processed.");

        List<Order> ordersToFindSupplier = orders.stream().filter(it -> GrayLetterRule.needFindSupplier(it)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ordersToFindSupplier)) {
            messagePanel.displayMsg("Trying to find suppliers for " + ordersToFindSupplier.size() + " order(s).");
            for (Order order : ordersToFindSupplier) {
                try {
                    findSupplier(order);
                } catch (Exception e) {
                    //
                }
            }
            messagePanel.addMsgSeparator();
        }

        List<Order> ordersToSendLetters = orders.stream().filter(it -> GrayLetterRule.needSendLetter(it)).collect(Collectors.toList());
        messagePanel.displayMsg(ordersToSendLetters.size() + " order(s) to send letters.");
        if (CollectionUtils.isNotEmpty(ordersToSendLetters)) {
            for (Order order : ordersToSendLetters) {
                try {
                    sendLetter(order);
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    public void sendLetter(Order order) {
        ascLetterSender.sendForOrder(order);
    }

    @Inject HuntService huntService;
    @Inject SheetService sheetService;

    public void findSupplier(Order order) {
        //find seller
        Country country = OrderCountryUtils.getMarketplaceCountry(order);
        Seller seller;
        try {
            seller = huntService.huntForOrder(order);
        } catch (Exception e) {
            LOGGER.error("", e);

            String msg = String.format("%s %s %s row %d - %s %s",
                    (country.europe() ? "EU" : country.name()), order.type().name().toLowerCase(),
                    order.sheetName, order.row, order.order_id, "failed to find seller - " + Strings.getExceptionMsg(e));

            messagePanel.displayMsg(msg, InformationLevel.Negative);
            return;
        }

        try {
            SellerHuntUtils.setSellerDataForOrder(order, seller);
            sheetService.fillSellerInfo(order);

            String msg = String.format("%s %s %s row %d - %s %s",
                    (country.europe() ? "EU" : country.name()), order.type().name().toLowerCase(),
                    order.sheetName, order.row, order.order_id, "find seller  - " + seller.toSimpleString());

            messagePanel.displayMsg(msg);
        } catch (Exception e) {
            LOGGER.error("", e);
            String msg = String.format("%s %s %s row %d - %s %",
                    (country.europe() ? "EU" : country.name()), order.type().name().toLowerCase(),
                    order.sheetName, order.row, order.order_id, "failed to write seller info to sheet - " + Strings.getExceptionMsg(e));
            messagePanel.displayMsg(msg, InformationLevel.Negative);
        }
    }
}
