package edu.olivet.harvester.selforder;

import com.amazonaws.mws.model.FeedSubmissionInfo;
import com.amazonservices.mws.orders._2013_09_01.model.OrderItem;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.PrivateBinder;
import com.google.inject.Singleton;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import com.sun.org.apache.xpath.internal.operations.Or;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.OrderFetcher;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.common.model.OrderEnums.OrderColor;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.common.service.mws.OrderClient;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.feeds.helper.FeedGenerator;
import edu.olivet.harvester.feeds.helper.FeedGenerator.BatchFileType;
import edu.olivet.harvester.feeds.service.ConfirmationFailedLogService;
import edu.olivet.harvester.feeds.service.FeedUploadService;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.letters.service.LetterSheetService;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 5/10/2018 4:09 PM
 */
@Singleton
public class RefundOrder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundOrder.class);

    @Inject
    private FeedGenerator feedGenerator;

    @Inject
    private OrderItemTypeHelper orderItemTypeHelper;

    @Getter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    @Inject
    @Setter
    private AppScript appScript;

    @Inject
    private FeedUploadService feedUploader;

    @Inject
    private SheetAPI sheetAPI;

    @Inject
    private DBManager dbManager;

    @Inject
    private SelfOrderFilter selfOrderFilter;

    @Inject private
    OrderFetcher orderFetcher;

    @Setter
    private int lastOrderRowNo = 3;

    @Inject
    LetterSheetService letterSheetService;

    public void refundOrderForWorksheets(List<Worksheet> worksheets) {
        for (Worksheet worksheet : worksheets) {
            long start = System.currentTimeMillis();

            LOGGER.info("Starting refund orders for spreadsheet {} at {}.", worksheet.toString(), start);

            try {
                refundOrderForWorksheet(worksheet);
                LOGGER.info("Refund Orders for spreadsheet {} in {}.", worksheet.toString(), Strings.formatElapsedTime(start));
            } catch (Exception e) {
                LOGGER.info("Error Refund Orders for spreadsheet {} . ", worksheet.toString(), e);
                Country country = worksheet.getSpreadsheet().getSpreadsheetCountry();
                //ConfirmationFailedLogService.logFailed(country, worksheet.getSheetName(), e.getMessage());
            }
        }
    }

    private void refundOrderForWorksheet(Worksheet worksheet) {

        Country country;

        StringBuilder resultSummary = new StringBuilder();
        StringBuilder resultDetail = new StringBuilder();

        try {
            country = worksheet.getSpreadsheet().getSpreadsheetCountry();
        } catch (Exception e) {
            messagePanel.displayMsg(
                    String.format("Cant load country info for worksheet %s.", worksheet.toString()), LOGGER);
            return;
        }

        messagePanel.addMsgSeparator();
        messagePanel.displayMsg(
                String.format("Starting refunding selfOrders for worksheet %s at %s", worksheet.toString(), Dates.now()), LOGGER);

        //get orders from google spreadsheet, all errors are handled.
        List<Order> orders = getOrdersFromWorksheet(worksheet);
        orders.removeIf(it -> !Regex.AMAZON_ORDER_NUMBER.isMatched(it.order_id));
        // filter orders
        orders = selfOrderFilter.filterOrders(orders);

        messagePanel.displayMsg(orders.size() + "  self order(s) found need to refund on the worksheet. ");

        resultSummary.append("Total ").append(orders.size()).append(" found; ");
        resultDetail.append("Total ").append(orders.size()).append(" orders found.").append("\n");

        if (orders.isEmpty()) {
            return;
        }

        // 提交feed成功之后，对每一个self order操作处理。
        dealWithOrder(orders);

        lastOrderRowNo = getLastOrderRow(orders);

        // create feed file
        File feedFile;
        try {
            feedFile = feedGenerator.generateSelfOrdersRefund(prepareFeedData(orders), worksheet.getSpreadsheet().getSpreadsheetCountry(), worksheet.getSpreadsheet().getSpreadsheetType());
            messagePanel.wrapLineMsg("Feed file generated at " + feedFile.getAbsolutePath(), InformationLevel.Important);
            messagePanel.wrapLineMsg(Tools.readFileToString(feedFile));
        } catch (Exception e) {
            messagePanel.displayMsg("Error when generating feed file. " + e.getMessage(), InformationLevel.Negative);
            LOGGER.error("Error when generator feed file. " + e);

            return;
        }

        // submit feed to Amazon via MWS Feed Api
        String result;

        try {
            result = feedUploader.submitFeed(feedFile, BatchFileType.SelfOrderRefund, country);
            if (StringUtils.isBlank(result)) {
                throw new BusinessException("No result returned.");
            }
        } catch (Exception e) {
            messagePanel.displayMsg("Error when submitting feed file. " + e.getMessage(), InformationLevel.Negative);

            LOGGER.error("Error when submitting feed file. ", e);
            messagePanel.displayMsg("Please try to submit the feed file via Amazon Seller Center.");


            return;

        }


        try {
            recordSelfOrderRefundLog(worksheet, resultSummary, result);
        } catch (Exception e) {
            LOGGER.error("Fail to log orderRefund ", e);
        }




    }


    // 对 self order标灰，AD列标 refunded. Status列标注 finish.
    public void dealWithOrder(List<Order> orders){

        orders.forEach(order -> letterSheetService.fillRefundSuccessInfo(order));
        orders.forEach(order -> appScript.markColor(order.spreadsheetId, order.sheetName, order.row, OrderColor.Gray));

    }


    @Repeat
    public void recordSelfOrderRefundLog(Worksheet worksheet, StringBuilder resultSummary, String result) {
        writeLogToWorksheet(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName(), resultSummary.toString(), result);
    }


    public void writeLogToWorksheet(String spreadsheetId, String sheetName, String summary, String result) {

        int[] counts = ServiceUtils.parseFeedSubmissionResult(result);

        String log = String.format("auto-refunded; %s Process summary: Total submitted %s, Succeed %s, Failed %s",
                summary, counts[0], counts[1], counts[2]);

        String now = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(System.currentTimeMillis());

        while (true) {
            try {
                appScript.selfOrderRefundLog(spreadsheetId, sheetName, lastOrderRowNo + 1, log + "\n" + now);
                break;
            } catch (BusinessException e) {
                if (e.getMessage().contains("is not blank")) {
                    LOGGER.warn("Row {} is not blank, try to add to next row.", lastOrderRowNo);
                    lastOrderRowNo++;
                } else {
                    LOGGER.error("", e);
                    break;
                }
            }
        }
    }

    private static final String RETURN_CODE = "CustomerReturn";

    public List<String[]> prepareFeedData(List<Order> orders) {
        //114-8105070-5213810	63926741543306	CustomerReturn	USD	0.26

        List<String[]> feedData = new ArrayList<>();

        MarketWebServiceIdentity mwsCredential = Settings.load().getConfigByCountry(OrderCountryUtils.getMarketplaceCountry(orders.get(0))).getMwsCredential();

        for (Order order : orders) {
            List<OrderItem> items = orderFetcher.readItems(order.order_id, mwsCredential);
            for (OrderItem item : items) {
                String orderItemId = item.getOrderItemId();
                String currencyCode = item.getItemPrice().getCurrencyCode();

                float originalPrice = Float.parseFloat(item.getItemPrice().getAmount());
                //float shippingFee  = Float.parseFloat(item.getShippingPrice().getAmount());
                float promotional = Float.parseFloat(item.getPromotionDiscount().getAmount());
                float price = originalPrice - promotional;

                String[] rowData = new String[] {
                        order.order_id, orderItemId, RETURN_CODE, currencyCode, String.format("%.02f", price)
                };

                feedData.add(rowData);
            }
        }
        return feedData;
    }

    public int getLastOrderRow(List<Order> orders) {
        //noinspection ConstantConditions
        return CollectionUtils.isEmpty(orders) ? 3 : orders.stream().mapToInt(Order::getRow).max().getAsInt();
    }


    public List<Order> getOrdersFromWorksheet(Worksheet worksheet) {

        List<Order> orders = new ArrayList<>();

        try {
            orders = appScript.getOrdersFromWorksheet(worksheet);

        } catch (NoWorksheetFoundException e) {

            LOGGER.error("No worksheet {} found. {}", worksheet.toString(), e);

            messagePanel.displayMsg(String.format("No worksheet %s found", worksheet.toString()), InformationLevel.Negative);

        } catch (NoOrdersFoundInWorksheetException e) {
            messagePanel.displayMsg("No order data found on worksheet " + worksheet.toString(),
                    InformationLevel.Negative);
            LOGGER.error("No order data found on worksheet {}.", worksheet.toString(), e);

        } catch (BusinessException e) {

            messagePanel.displayMsg("Failed to read order data from " + worksheet.toString() + ". Please try again later.",
                    InformationLevel.Negative);
            LOGGER.error("Failed to read order data {} - {}", worksheet.toString(), e);

        }

        return orders;
    }

    public void execute() {

        //this method is for cronjob, keep silent.
        setMessagePanel(new VirtualMessagePanel());

        //get all order update sheets for account
        List<String> spreadIds;
        try {
            spreadIds = Settings.load().listAllSpreadsheets();
        } catch (BusinessException e) {
            LOGGER.error("No configuration file found. {}", e);
            throw new BusinessException("No configuration file found.");
        }



    }



    public void setMessagePanel(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
        //shipmentOrderFilter.setMessagePanel(messagePanel);
        //feedUploader.setMessagePanel(messagePanel);
    }


    public static void main(String[] args) {
        UITools.setTheme();

        //ApplicationContext.getBean(RefundOrder.class).execute();
        RefundOrder refundOrder = ApplicationContext.getBean(RefundOrder.class);

        String spreadsheetId = "1eSECnF7F6hCybrUJWBl9EUzZrw6NnrAWe7h15OyYrjo";
        String sheetName = "02/22";
        String summary = "test test summary";
        String result = "test test result";
        refundOrder.writeLogToWorksheet(spreadsheetId, sheetName, summary, result);



    }

}
