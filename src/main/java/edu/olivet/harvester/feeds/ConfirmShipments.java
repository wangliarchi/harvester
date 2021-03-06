package edu.olivet.harvester.feeds;

import com.amazonaws.mws.model.FeedSubmissionInfo;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.feeds.helper.ConfirmShipmentEmailSender;
import edu.olivet.harvester.feeds.helper.FeedGenerator;
import edu.olivet.harvester.feeds.helper.FeedGenerator.BatchFileType;
import edu.olivet.harvester.feeds.helper.ShipDateUtils;
import edu.olivet.harvester.feeds.helper.ShipmentOrderFilter;
import edu.olivet.harvester.feeds.model.OrderConfirmationLog;
import edu.olivet.harvester.feeds.service.ConfirmationFailedLogService;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.common.service.Carrier;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.common.service.mws.FeedSubmissionFetcher;
import edu.olivet.harvester.common.service.mws.OrderClient;
import edu.olivet.harvester.feeds.service.FeedUploadService;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Singleton
public class ConfirmShipments {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmShipments.class);

    @Inject
    private FeedGenerator feedGenerator;

    @Inject
    private Carrier carrierHelper;

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
    @Getter
    @Setter
    private ConfirmShipmentEmailSender confirmShipmentEmailSender;


    @Inject
    private ShipmentOrderFilter shipmentOrderFilter;

    @Inject
    private SheetAPI sheetAPI;

    @Inject
    private DBManager dbManager;

    @Inject private ShipDateUtils shipDateUtils;

    @Setter
    private int lastOrderRowNo = 3;


    /**
     * confirm shipments by worksheet. Errors handled by each confirmShipmentForWorksheet call separately.
     */
    public void confirmShipmentForWorksheets(List<Worksheet> worksheets) {
        for (Worksheet worksheet : worksheets) {
            long start = System.currentTimeMillis();
            LOGGER.info("Starting confirming shipments for spreadsheet {} at {}.", worksheet.toString(), start);
            try {
                confirmShipmentForWorksheet(worksheet);
                LOGGER.info("Confirm shipments for spreadsheet {} in {}.", worksheet.toString(), Strings.formatElapsedTime(start));
            } catch (Exception e) {
                LOGGER.info("Error confirming shipments for spreadsheet {} . ", worksheet.toString(), e);
                Country country = worksheet.getSpreadsheet().getSpreadsheetCountry();
                ConfirmationFailedLogService.logFailed(country, worksheet.getSheetName(), e.getMessage());
            }
        }
    }

    private void confirmShipmentForSpreadsheetId(String spreadsheetId, String sheetName) {
        //set spreadsheet id, check if the given spreadsheet id is valid
        Spreadsheet workingSpreadsheet;
        try {
            workingSpreadsheet = appScript.getSpreadsheet(spreadsheetId);
        } catch (Exception e) {
            LOGGER.error("No google spread sheet found for id {}. {}", spreadsheetId, e);
            throw new BusinessException(
                    String.format(
                            "No google spreadsheet found for %s. Please make sure the correct order update google sheet id is entered.",
                            spreadsheetId)
            );
        }

        Worksheet selectedWorksheet = new Worksheet(workingSpreadsheet, sheetName);

        confirmShipmentForWorksheet(selectedWorksheet);
    }

    private void confirmShipmentForWorksheet(Worksheet worksheet) {
        StringBuilder resultSummary = new StringBuilder();
        StringBuilder resultDetail = new StringBuilder();

        Country country;
        try {
            country = worksheet.getSpreadsheet().getSpreadsheetCountry();
        } catch (Exception e) {
            messagePanel.displayMsg(
                    String.format("Cant load country info for worksheet %s.", worksheet.toString()), LOGGER);
            return;
        }

        if (country == Country.JP) {
            messagePanel.wrapLineMsg("It is not allowed to confirm shipment for Japan account yet.", InformationLevel.Negative);
            return;
        }

        while (true) {
            try {
                List<FeedSubmissionInfo> submissionInfo = getUnprocessedFeedSubmission(country);
                if (submissionInfo.size() > 0) {
                    messagePanel.wrapLineMsg(String.format("%s processing/unporcessed order confirmation found. Wait 5 mins to try again.",
                            submissionInfo.size()), LOGGER, InformationLevel.Information);
                    Tools.sleep(5, TimeUnit.MINUTES);
                } else {
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load unprocessed feed submission. ", e);
                break;
            }
        }

        messagePanel.addMsgSeparator();
        messagePanel.displayMsg(
                String.format("Starting confirming shipments for worksheet %s at %s", worksheet.toString(), Dates.now()), LOGGER);

        //get orders from google spreadsheet, all errors are handled.
        List<Order> orders = getOrdersFromWorksheet(worksheet, country);
        orders.removeIf(it -> !Regex.AMAZON_ORDER_NUMBER.isMatched(it.order_id));
        messagePanel.displayMsg(orders.size() + "  order(s) found on the worksheet. ");

        resultSummary.append("Total ").append(orders.size()).append(" found; ");
        resultDetail.append("Total ").append(orders.size()).append(" orders found.").append("\n");

        if (orders.isEmpty()) {
            return;
        }

        lastOrderRowNo = getLastOrderRow(orders);
        //orderFinderEmail = getOrderFinderEmail(orders);

        mwsOrderClient.getAmazonOrderStatuses(orders, country);
        List<Order> canceledOrders = new ArrayList<>();
        orders.forEach(order -> {
            if ("Canceled".equals(order.getAmazonOrderStatus())) {
                canceledOrders.add(order);
            }
        });

        if (CollectionUtils.isNotEmpty(canceledOrders)) {
            try {
                sheetAPI.markBuyerCancelOrders(canceledOrders, worksheet);
            } catch (Exception e) {
                LOGGER.error("Failed to mark canceled orders {} for {}",
                        worksheet, canceledOrders.stream().map(it -> it.order_id).collect(Collectors.toList()), e);
            }
        }

        //filter orders
        orders = shipmentOrderFilter.filterOrders(orders, worksheet, resultSummary, resultDetail);

        if (orders.isEmpty()) {
            messagePanel.displayMsg("No orders need to be confirmed for sheet " + worksheet.toString(), LOGGER, InformationLevel.Positive);

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("No  orders need to be confirmed for sheet %s", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject, subject, country);
            }

            return;
        }

        messagePanel.displayMsg(orders.size() + "  order(s) found to be confirmed. ", InformationLevel.Important);
        resultDetail.append(orders.size()).append(" submitted.").append("\n");
        resultSummary.append(orders.size()).append(" submitted. ");

        //create feed file
        File feedFile;
        try {
            feedFile = generateFeedFile(worksheet, orders);
            messagePanel.wrapLineMsg("Feed file generated at " + feedFile.getAbsolutePath(), InformationLevel.Important);
            messagePanel.wrapLineMsg(Tools.readFileToString(feedFile));
        } catch (Exception e) {
            messagePanel.displayMsg("Error when generating feed file. " + e.getMessage(), InformationLevel.Negative);
            LOGGER.error("Error when generating feed file. " + e);

            ConfirmationFailedLogService.logFailed(country, worksheet.getSheetName(), e.getMessage());

            return;
        }


        //submit feed to amazon via MWS Feed API
        String result;

        try {
            result = feedUploader.submitFeed(feedFile, BatchFileType.ShippingConfirmation, country);
            if (StringUtils.isBlank(result)) {
                throw new BusinessException("No result returned.");
            }
        } catch (Exception e) {
            ConfirmationFailedLogService.logFailed(country, worksheet.getSheetName(), e.getMessage());
            messagePanel.displayMsg("Error when submitting feed file. " + e.getMessage(), InformationLevel.Negative);
            LOGGER.error("Error when submitting feed file. ", e);
            messagePanel.displayMsg("Please try to submit the feed file via Amazon Seller Center.");
            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("Error when submitting file for sheet %s, feed file %s",
                        worksheet.toString(), feedFile.getName());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Error when submitting feed file. " + e.getMessage(), country);
            }
            return;
        }

        try {
            recordConfirmationLog(country, worksheet, resultSummary, resultDetail, result, feedFile);
        } catch (Exception e) {
            LOGGER.error("Fail to log confirmation ", e);
        }

    }

    @Repeat
    public void recordConfirmationLog(Country country, Worksheet worksheet, StringBuilder resultSummary,
                                      StringBuilder resultDetail, String result, File feedFile) {
        //write log to worksheet
        writeLogToWorksheet(worksheet, result, resultSummary.toString());

        result = resultDetail.toString() + "\n" + result;

        insertToLocalDbLog(feedFile, country, result);

        //send email
        //if (messagePanel instanceof VirtualMessagePanel) {
        confirmShipmentEmailSender.sendSuccessEmail(result, feedFile, country);
        //}

    }

    public void insertToLocalDbLog(File feedFile, Country country, String result) {
        OrderConfirmationLog log = new OrderConfirmationLog();
        log.setId(FilenameUtils.getBaseName(feedFile.getName()));
        log.setContext(Settings.load().getConfigByCountry(country).getAccountCode());
        log.setUploadTime(new Date());

        log.setResult(result.replace(StringUtils.LF, StringUtils.SPACE));
        dbManager.insert(log, OrderConfirmationLog.class);
    }

    public int getLastOrderRow(List<Order> orders) {
        //noinspection ConstantConditions
        return CollectionUtils.isEmpty(orders) ? 3 : orders.stream().mapToInt(Order::getRow).max().getAsInt();
    }

    public @Nullable String getOrderFinderEmail(List<Order> orders) {
        for (Order order : orders) {
            if (StringUtils.isNotEmpty(order.code)) {
                String[] parts = StringUtils.split(order.code, "/");
                for (String part : parts) {
                    if (RegexUtils.isEmail(part.trim())) {
                        return part.trim();
                    }
                }
            }
        }

        return null;
    }

    public void writeLogToWorksheet(Worksheet worksheet, String result, String summary) {
        int[] counts = ServiceUtils.parseFeedSubmissionResult(result);

        String log = String.format("auto-confirmed; %s Process summary: Total submitted %s, Succeed %s, Failed %s",
                summary, counts[0], counts[1], counts[2]);
        String now = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(System.currentTimeMillis());
        while (true) {
            try {
                String spreadsheetId = worksheet.getSpreadsheet().getSpreadsheetId();
                appScript.commitShippingConfirmationLog(spreadsheetId, worksheet.getSheetName(), lastOrderRowNo + 1, log + "\n" + now);
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


    /**
     * get orders from given worksheet. .
     */
    List<Order> getOrdersFromWorksheet(Worksheet worksheet) {
        Country country;
        try {
            country = worksheet.getSpreadsheet().getSpreadsheetCountry();
        } catch (Exception e) {
            LOGGER.error("Cant load country info for worksheet {}. {}", worksheet.toString(), e);
            throw e;
        }

        return getOrdersFromWorksheet(worksheet, country);
    }

    /**
     * get orders from given worksheet. .
     */
    public List<Order> getOrdersFromWorksheet(Worksheet worksheet, Country country) {

        List<Order> orders = new ArrayList<>();

        try {
            orders = appScript.getOrdersFromWorksheet(worksheet);
        } catch (NoWorksheetFoundException e) {

            LOGGER.error("No worksheet {} found. {}", worksheet.toString(), e);

            messagePanel.displayMsg(String.format("No worksheet %s found", worksheet.toString()), InformationLevel.Negative);

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("No worksheet %s found.", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Please check if orders have been exported correctly", country);
            }


        } catch (NoOrdersFoundInWorksheetException e) {
            messagePanel.displayMsg("No order data found on worksheet " + worksheet.toString(),
                    InformationLevel.Negative);
            LOGGER.error("No order data found on worksheet {}.", worksheet.toString(), e);

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("No order data found on worksheet %s ", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Please check if orders have been exported correctly", country);
            }


        } catch (BusinessException e) {
            messagePanel.displayMsg("Failed to read order data from " + worksheet.toString() + ". Please try again later.",
                    InformationLevel.Negative);
            LOGGER.error("Failed to read order data {} - {}", worksheet.toString(), e);

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("Failed to read order data from %s", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Please check if orders have been exported correctly. \t " + e.getMessage(), country);
            }


        }

        return orders;
    }

    private File generateFeedFile(Worksheet worksheet, List<Order> orders) {

        Date defaultShipDate;
        try {
            defaultShipDate = Dates.parseDate(worksheet.getOrderConfirmationDate());
        } catch (Exception e) {
            //todo
            throw new BusinessException("Order confirmation for non mm/dd sheet is currently not supported.");
        }

        //prepare data
        List<String[]> ordersToBeConfirmed = new ArrayList<>();

        for (Order order : orders) {

            OrderEnums.OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);

            //sales channel data may be missed, or not accurate.
            Country country;
            try {
                country = Country.fromSalesChanel(order.sales_chanel);
            } catch (Exception e) {
                country = worksheet.getSpreadsheet().getSpreadsheetCountry();
            }

            String carrierCode = carrierHelper.getCarrierCodeByCountryAndType(country, orderItemType);
            String[] codes = StringUtils.split(carrierCode, ",");
            String carrierName = "";
            if (codes.length == 2) {
                carrierCode = codes[0];
                carrierName = codes[1];
            }

            String shipDate = shipDateUtils.getShipDateString(order, defaultShipDate);
            String[] row = {order.order_id, carrierCode, carrierName, shipDate};
            ordersToBeConfirmed.add(row);
        }


        //create feed file
        return this.feedGenerator.generateConfirmShipmentFeedFromRows(ordersToBeConfirmed,
                worksheet.getSpreadsheet().getSpreadsheetCountry(), worksheet.getSpreadsheet().getSpreadsheetType());

    }


    @Inject
    @Setter
    private OrderClient mwsOrderClient;

    public void notConfirmedOrderNotification() {
        List<Country> countries = Settings.load().listAllCountries();


        Date createBefore = DateUtils.addDays(new Date(), -1);
        Date createAfter = DateUtils.addDays(new Date(), -10);
        Date now = new Date();

        countries.forEach(country -> {
            try {

                LOGGER.info("Load Unshipped or PartiallyShipped orders between {} and {}", createAfter, createBefore);

                List<com.amazonservices.mws.orders._2013_09_01.model.Order> orders =
                        mwsOrderClient.listUnshippedOrders(country, createBefore, createAfter);

                //todo timezone? should be fine since there should be at lest one day between EarliestShipDate and LatestShipDate
                orders.removeIf(order -> order.getEarliestShipDate().toGregorianCalendar().getTime().after(now));
                LOGGER.info("{} unshipped or partiallyShipped order(s) founded  between {} and {}", createAfter, createBefore);


                if (orders.size() > 0) {
                    //send email
                    String subject = String.format("Alert: %d %s created before %s %s not been confirmed.",
                            orders.size(), orders.size() == 1 ? "order" : "orders",
                            edu.olivet.harvester.utils.common.DateFormat.DATE_TIME.format(createBefore),
                            orders.size() == 1 ? "has" : "have");

                    StringBuilder content = new StringBuilder(subject).append("\n\n");
                    for (com.amazonservices.mws.orders._2013_09_01.model.Order order : orders) {
                        content.append(order.getAmazonOrderId()).append("\t")
                                .append(order.getOrderStatus())
                                .append("\t")
                                .append(order.getPurchaseDate().toString())
                                .append("\n");
                    }

                    confirmShipmentEmailSender.sendErrorFoundEmail(subject, content.toString(), country);

                    messagePanel.wrapLineMsg(content.toString(), LOGGER, InformationLevel.Negative);

                }
            } catch (Exception e) {
                messagePanel.wrapLineMsg("Error read unshipped orders via MWS. " + e.getMessage(), InformationLevel.Negative);
                LOGGER.error("Error read unshipped orders via MWS. ", e);
            }


        });
    }


    @Inject private
    FeedSubmissionFetcher feedSubmissionFetcher;

    public List<FeedSubmissionInfo> getUnprocessedFeedSubmission(Country country) {
        return feedSubmissionFetcher.getActiveShipmentConfirmationSubmissionList(country);
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

        //default to select today's sheet
        String sheetName = shipDateUtils.getSheetNameByDate(System.currentTimeMillis());

        //then confirm shipment for each spreadsheet
        for (String spreadId : spreadIds) {
            try {
                confirmShipmentForSpreadsheetId(spreadId, sheetName);
            } catch (Exception e) {
                LOGGER.error("Error when confirm shipment for sheet {} {}", spreadId, e);
            }
        }

        //wait for 2 minutes, then check unshipped orders.
        Tools.sleep(2, TimeUnit.MINUTES);
        notConfirmedOrderNotification();

    }

    public void setMessagePanel(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
        shipmentOrderFilter.setMessagePanel(messagePanel);
        feedUploader.setMessagePanel(messagePanel);
    }


    public static void main(String[] args) {
        UITools.setTheme();

        ApplicationContext.getBean(ConfirmShipments.class).execute();

    }


}
