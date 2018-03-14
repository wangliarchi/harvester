package edu.olivet.harvester.letters;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.fulfill.service.AmazonOrderService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.HuntService;
import edu.olivet.harvester.hunt.service.SheetService;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.letters.service.*;
import edu.olivet.harvester.letters.service.GrayLetterRule.GrayRule;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.utils.MessageListener;
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
    @Inject GmailLetterSender gmailLetterSender;
    @Inject LetterTemplateService letterTemplateService;
    @Inject LetterSheetService letterSheetService;
    @Inject AmazonOrderService amazonOrderService;

    @Inject private MessageListener messageListener;

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
        messageListener.addMsgSeparator();
        messageListener.addMsg("Send gray label letters for " + worksheet);
        List<Order> orders = appScript.readOrders(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName());

        if (CollectionUtils.isEmpty(orders)) {
            messageListener.displayMsg("No orders found.", InformationLevel.Negative);
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
            messageListener.addMsg("No orders need to send gray letters", InformationLevel.Negative);
            return;
        }

        messageListener.displayMsg(orders.size() + " order(s) to be processed.");

        List<Order> ordersToFindSupplier = orders.stream().filter(it -> GrayLetterRule.needFindSupplier(it)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ordersToFindSupplier)) {
            messageListener.addMsg("Trying to find suppliers for " + ordersToFindSupplier.size() + " order(s).");
            for (Order order : ordersToFindSupplier) {
                try {
                    findSupplier(order);
                } catch (Exception e) {
                    //
                }
            }
            messageListener.addMsgSeparator();
        }

        List<Order> ordersToSendLetters = orders.stream().filter(it -> GrayLetterRule.needSendLetter(it)).collect(Collectors.toList());
        messageListener.addMsg(ordersToSendLetters.size() + " order(s) to send letters.");
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

        AmazonOrder amazonOrder;
        try {
            amazonOrder = amazonOrderService.loadOrder(order);
            if (!"Shipped".equalsIgnoreCase(amazonOrder.getOrderStatus())) {
                messageListener.addMsg(order, "Order not shipped yet.", InformationLevel.Negative);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load order info from Amazon via API.");
            amazonOrder = amazonOrderService.loadFromLocal(order.order_id, order.sku);
        }


        SystemSettings systemSettings = SystemSettings.load();

        Letter letter = letterTemplateService.getLetter(order);

        boolean finished = false;
        if (systemSettings.sendGrayLabelLettersViaASC()) {
            try {
                ascLetterSender.sendForOrder(order, letter);
                finished = true;
            } catch (Exception e) {
                LOGGER.error("", e);
                letterSheetService.fillFailedInfo(order, "Failed via ASC");
            }
        }

        if (systemSettings.sendGrayLabelLettersViaEmail()) {
            if (amazonOrder == null) {
                return;
            }

            order.buyer_email = amazonOrder.getEmail();
            try {
                gmailLetterSender.sendForOrder(order, letter);
                finished = true;
            } catch (Exception e) {
                LOGGER.error("", e);
                letterSheetService.fillFailedInfo(order, "Failed via Email");
            }
        }

        if (finished) {
            letterSheetService.fillSuccessInfo(order);
        }
    }

    @Inject HuntService huntService;
    @Inject SheetService sheetService;

    public void findSupplier(Order order) {
        //find seller
        Seller seller;
        try {
            seller = huntService.huntForOrder(order);
        } catch (Exception e) {
            LOGGER.error("", e);
            messageListener.addMsg(order, "failed to find seller - " + Strings.getExceptionMsg(e), InformationLevel.Negative);
            return;
        }

        try {
            SellerHuntUtils.setSellerDataForOrder(order, seller);
            sheetService.fillSellerInfo(order);
            messageListener.addMsg(order, "find seller  - " + seller.toSimpleString());
        } catch (Exception e) {
            LOGGER.error("", e);
            messageListener.addMsg(order, "failed to write seller info to sheet - " + Strings.getExceptionMsg(e), InformationLevel.Negative);
        }
    }
}
