package edu.olivet.harvester.letters;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.Status;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.fulfill.service.AmazonOrderService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.HuntService;
import edu.olivet.harvester.hunt.service.SheetService;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.letters.model.GrayEnums.GrayLetterType;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.letters.service.*;
import edu.olivet.harvester.letters.service.GrayLetterRule.GrayRule;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:08 AM
 */
public class CommonLetterSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonLetterSender.class);

    @Inject SheetAPI sheetAPI;
    @Inject OrderService orderService;
    @Inject AppScript appScript;
    @Inject ASCLetterSender ascLetterSender;
    @Inject GmailLetterSender gmailLetterSender;
    @Inject LetterTemplateService letterTemplateService;
    @Inject LetterSheetService letterSheetService;
    @Inject AmazonOrderService amazonOrderService;

    @Inject private MessageListener messageListener;

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    public void execute() {
        messagePanel = messageListener;
        //get all order update sheets for account
        List<String> spreadIds;
        try {
            spreadIds = Settings.load().listAllSpreadsheets();
        } catch (BusinessException e) {
            LOGGER.error("No configuration file found. {}", e);
            return;
        }

        //process for each spreadsheet
        for (String spreadId : spreadIds) {
            try {
                execute(spreadId);
            } catch (Exception e) {
                LOGGER.error("Error when sending common letters for sheet {} {}", spreadId, e);
            }
        }
    }

    public void execute(String spreadsheetId) {
        Date minDate = DateUtils.addDays(new Date(), -SystemSettings.load().getGrayLabelLetterMaxDays() - 1);
        Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(spreadsheetId);

        List<Order> orders = orderService.fetchOrders(spreadsheet, minDate);
        LOGGER.info("{} orders found from {}", orders.size(), spreadsheet.getProperties().getTitle());
        handleOrders(orders, true, spreadsheet.getProperties().getTitle());
    }


    //manually triggered
    public void executeForWorksheets(List<Worksheet> worksheets) {
        if (PSEventListener.isRunning()) {
            UITools.error("Other task is running, please try later.");
            return;
        }

        messagePanel = new ProgressDetail(Actions.CommonLetters);
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
        long start = System.currentTimeMillis();
        messagePanel.addMsgSeparator();
        messagePanel.displayMsg("Send gray label letters for " + worksheet, LOGGER);
        List<Order> orders = appScript.readOrders(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName());

        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No orders found.", LOGGER, InformationLevel.Negative);
            return;
        }

        handleOrders(orders, false, worksheet.toString());
        messagePanel.displayMsg("Done sending gray label letters for " + worksheet + ", took " + Strings.formatElapsedTime(start), LOGGER, InformationLevel.Positive);
    }


    public void handleOrders(List<Order> orders, boolean findSupplier, String title) {
        while (PSEventListener.isRunning()) {
            WaitTime.Short.execute();
        }
        //暂时不处理特殊sheet 里面的order

        List<Order> grayOrders = orders.stream()
                .filter(it -> RegexUtils.Regex.COMMON_ORDER_SHEET_NAME.isMatched(it.sheetName) &&
                        GrayLetterRule.getGrayRule(it, findSupplier) != GrayRule.None)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(grayOrders)) {
            messagePanel.displayMsg("No orders need to send gray letters for " + title, LOGGER, InformationLevel.Negative);
            return;
        }

        messagePanel.displayMsg(grayOrders.size() + " order(s) to be processed for " + title, LOGGER);

        letterSheetService.updateLastCode(orders.get(0).spreadsheetId, orders);

        if (findSupplier) {
            List<Order> ordersToFindSupplier = grayOrders.stream().filter(GrayLetterRule::needFindSupplier).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ordersToFindSupplier)) {
                messagePanel.displayMsg("Trying to find suppliers for " + ordersToFindSupplier.size() + " order(s).", LOGGER);
                for (Order order : ordersToFindSupplier) {
                    try {
                        findSupplier(order);
                    } catch (Exception e) {
                        messagePanel.displayMsg(msgPrefix(order) + "failed to find seller - " + Strings.getExceptionMsg(e), LOGGER, InformationLevel.Negative);
                    }
                }
                messagePanel.addMsgSeparator();
            }
        }


        List<Order> ordersToSendLetters = grayOrders.stream().filter(it -> GrayLetterRule.needSendLetter(it, findSupplier)).collect(Collectors.toList());
        messagePanel.displayMsg(ordersToSendLetters.size() + " order(s) to send letters. Messages will be sent via " + SystemSettings.load().getGrayLabelLetterSendingMethod(), LOGGER);

        if (CollectionUtils.isNotEmpty(ordersToSendLetters)) {

            List<String> whiteOrders = orders.stream().filter(it -> !it.colorIsGray()).map(it -> it.order_id).collect(Collectors.toList());
            Map<String, List<Order>> ordersToSendLettersMap = new HashMap<>();

            for (Order order : ordersToSendLetters) {
                //如果发现有重复的白条订单（可做），将此单号发给客服(seller gmail)去处理，并在A列标记cs，
                //只针对灰条，lw之类白条订单不考虑
                if (whiteOrders.contains(order.order_id) && order.colorIsGray()) {
                    try {
                        sendLetterToCS(order);
                        messagePanel.displayMsg(msgPrefix(order) + "sent msg to cs successfully.", LOGGER, InformationLevel.Information);
                    } catch (Exception e) {
                        LOGGER.error("", e);
                        messagePanel.displayMsg(msgPrefix(order) + "Failed to send msg to cs.", LOGGER, InformationLevel.Negative);
                    }
                    continue;
                }

                //organize orders by order id,如果发现有重复的单号，并且都是灰条，重复的单号只发一次灰条信，都标记发信完成。
                List<Order> list = ordersToSendLettersMap.getOrDefault(order.order_id, new ArrayList<>());
                list.add(order);
                ordersToSendLettersMap.put(order.order_id, list);
            }

            //发信前，在当天表格里面搜索一下要发灰条信的订单号，如果发现有重复的单号，并且都是灰条，重复的单号只发一次灰条信，都标记发信完成。
            ordersToSendLettersMap.forEach((orderId, os) -> {
                Order order = os.get(0);
                try {
                    sendLetter(order);
                    if (os.size() > 1) {
                        for (Order o : os) {
                            if (!o.equals(order)) {
                                letterSheetService.fillSuccessInfo(o);
                            }
                        }
                    }
                } catch (Exception e) {
                    messagePanel.displayMsg(msgPrefix(order) + Strings.getExceptionMsg(e), LOGGER, InformationLevel.Negative);
                }
            });
        }
    }


    public void sendLetterToCS(Order order) {
        Letter letter = new Letter();
        letter.setSubject("Hi cs, please help connect customer with the order " + order.order_id);
        letter.setBody(String.format("Hi cs, please help connect customer with the order %s\n\n" +
                "the customer had ordered more than one products, but some of them are grey label.", order.order_id));
        //
        gmailLetterSender.sendMessageToCS(order, letter);
        order.status = "cs";
        letterSheetService.fillSuccessInfo(order);
    }

    public void sendLetter(Order order) {

        GrayLetterType type = GrayLetterType.getTypeFromStatus(order.status);
        if (type == null) {
            messagePanel.displayMsg(msgPrefix(order) + "Order status [" + order.status + "] not valid.", LOGGER, InformationLevel.Negative);
            return;
        }
        AmazonOrder amazonOrder;
        try {
            amazonOrder = amazonOrderService.loadOrder(order);
            if (type.isShouldBeShipped() && !amazonOrder.isShipped()) {
                messagePanel.displayMsg(msgPrefix(order) + "Order not shipped yet, gray type [" + type.name() + "].", LOGGER, InformationLevel.Negative);
                return;
            }

            if (!type.isShouldBeShipped() && amazonOrder.isShipped()) {
                messagePanel.displayMsg(msgPrefix(order) + "Order already shipped, gray type [" + type.name() + "].", LOGGER, InformationLevel.Negative);
                return;
            }
        } catch (Exception e) {
            //LOGGER.error("Failed to load order info from Amazon via API.", e);
            amazonOrder = amazonOrderService.loadFromLocal(order.order_id, order.sku);
        }


        SystemSettings systemSettings = SystemSettings.reload();
        Letter letter = letterTemplateService.getLetter(order);

        boolean finished = false;
        if (systemSettings.sendGrayLabelLettersViaASC()) {
            try {
                ascLetterSender.sendForOrder(order, letter);
                messagePanel.displayMsg(msgPrefix(order) + "Message sent via ASC successfully.", LOGGER, InformationLevel.Information);
                finished = true;
            } catch (Exception e) {
                LOGGER.error("Failed sending message via ASC ", e);
                messagePanel.displayMsg(msgPrefix(order) + "Failed via ASC", LOGGER, InformationLevel.Negative);
                letterSheetService.fillFailedInfo(order, "Failed via ASC");
            }
        }

        if (systemSettings.sendGrayLabelLettersViaEmail()) {
            if (systemSettings.isOrderSubmissionDebugModel()) {
                order.buyer_email = Constants.RND_EMAIL;
            } else {
                if (amazonOrder == null) {
                    messagePanel.displayMsg(msgPrefix(order) + "Cant find order email address", LOGGER, InformationLevel.Negative);
                    return;
                }
                order.buyer_email = amazonOrder.getEmail();
            }

            try {
                gmailLetterSender.sendForOrder(order, letter);
                finished = true;
                messagePanel.displayMsg(msgPrefix(order) + "Message sent via email successfully.", LOGGER, InformationLevel.Information);
            } catch (Exception e) {
                LOGGER.error("", e);
                messagePanel.displayMsg(msgPrefix(order) + "Failed via Email", LOGGER, InformationLevel.Negative);
                letterSheetService.fillFailedInfo(order, "Failed via Email");
            }
        }

        if (finished) {
            order.status = Status.Finish.value();
            letterSheetService.fillSuccessInfo(order);
        } else {
            throw new BusinessException("Failed to send message");
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
            LOGGER.error("Error hunting seller for {}", msgPrefix(order), e);
            messagePanel.displayMsg(msgPrefix(order) + "failed to find seller - " + Strings.getExceptionMsg(e), LOGGER, InformationLevel.Negative);
            return;
        }

        try {
            SellerHuntUtils.setSellerDataForOrder(order, seller);
            sheetService.fillSellerInfo(order);
            messagePanel.displayMsg(msgPrefix(order) + "find seller  - " + seller.toSimpleString(), LOGGER, InformationLevel.Information);
        } catch (Exception e) {
            LOGGER.error("Error hunting seller for {}", msgPrefix(order), e);
            messagePanel.displayMsg(msgPrefix(order) + "failed to write seller info to sheet - " + Strings.getExceptionMsg(e), LOGGER, InformationLevel.Negative);
        }
    }

    private String msgPrefix(Order order) {
        Country country = Settings.load().getSpreadsheetCountry(order.spreadsheetId);
        return String.format("%s %s - %s - row %d - %s - ",
                country != null ? (country.europe() ? "EU" : country.name()) : order.getSpreadsheetId(), order.type().name(),
                order.sheetName, order.row, order.order_id);
    }
}
