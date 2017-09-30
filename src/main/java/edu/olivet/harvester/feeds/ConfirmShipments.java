package edu.olivet.harvester.feeds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.FeedUploader;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.feeds.helper.ConfirmShipmentEmailSender;
import edu.olivet.harvester.feeds.helper.FeedGenerator;
import edu.olivet.harvester.feeds.helper.ShipmentOrderFilter;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.model.feeds.OrderConfirmationLog;
import edu.olivet.harvester.service.Carrier;
import edu.olivet.harvester.service.OrderItemTypeHelper;
import edu.olivet.harvester.spreadsheet.*;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.File;
import java.text.DateFormat;
import java.util.*;


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
    private FeedUploader feedUploader;

    @Inject
    private ConfirmShipmentEmailSender confirmShipmentEmailSender;

    @Inject
    private  ErrorAlertService errorAlertService;

    @Inject
    private ShipmentOrderFilter shipmentOrderFilter;

    @Inject
    private DBManager dbManager;

    @Setter
    private int lastOrderRowNo = 3;
    /**
     * confirm shipments by worksheet. Errors handled by each confirmShipmentForWorksheet call separately.
     *
     * @param worksheets
     */
    public void confirmShipmentForWorksheets(List<Worksheet> worksheets) {
        for (Worksheet worksheet : worksheets) {
            long start = System.currentTimeMillis();
            LOGGER.info("Starting confirming shipments for spreadsheet {} at {}.", worksheet.toString(), start);
            confirmShipmentForWorksheet(worksheet);
            LOGGER.info("Confirm shipments for spreadsheet {} in {}.", worksheet.toString(), Strings.formatElapsedTime(start));
        }

    }


    /**
     * @param spreadsheetId
     */
    private void confirmShipmentForSpreadsheetId(String spreadsheetId, String sheetName) {

        //set spreadsheet id, check if the given spreadsheet id is valid
        Spreadsheet workingSpreadsheet;
        try {
            workingSpreadsheet = appScript.getSpreadsheet(spreadsheetId);
        } catch (Exception e) {
            LOGGER.error("No google spread sheet found for id {}. {}", spreadsheetId, e.getMessage());
            throw new BusinessException(
                    "No google spread sheet found for id " + spreadsheetId + ". Please make sure the correct order update google sheet id is entered."
            );
        }

        Worksheet selectedWorksheet = new Worksheet(workingSpreadsheet, sheetName);

        confirmShipmentForWorksheet(selectedWorksheet);


    }


    private void confirmShipmentForWorksheet(Worksheet worksheet) {


        if (worksheet.getSpreadsheet().getSpreadsheetCountry() == Country.JP) {
            messagePanel.wrapLineMsg("It is not allowed to confirm shipment for Japan account yet.",InformationLevel.Negative);
            return;
        }

        long start = System.currentTimeMillis();
        messagePanel.addMsgSeparator();
        messagePanel.displayMsg(String.format("Starting confirming shipments for worksheet %s at %s", worksheet.toString(), Dates.now()),LOGGER);

        //get orders from google spreadsheet, all errors are handled.
        List<Order> orders = getOrdersFromWorksheet(worksheet);
        orders.sort(Comparator.comparing(Order::getRow));

        lastOrderRowNo = getLastOrderRow(orders);


        messagePanel.displayMsg(orders.size() + "  order(s) found on the worksheet. ");

        //filter orders
        orders = shipmentOrderFilter.filterOrders(orders, worksheet);

        if (orders.isEmpty()) {

            messagePanel.displayMsg("No  orders need to be confirmed for sheet "+ worksheet.toString(),LOGGER, InformationLevel.Positive);

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("No  orders need to be confirmed for sheet %s", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        subject,
                        worksheet.getSpreadsheet().getSpreadsheetCountry());
            }

            return;
        }

        messagePanel.displayMsg(orders.size() + "  order(s) found to be confirmed. ", InformationLevel.Important);



        //create feed file
        File feedFile;
        try {
            feedFile = generateFeedFile(worksheet, orders);
            messagePanel.wrapLineMsg("Feed file generated at " + feedFile.getAbsolutePath(), InformationLevel.Important);
            messagePanel.wrapLineMsg(Tools.readFileToString(feedFile));
        } catch (Exception e) {
            messagePanel.displayMsg("Error when generating feed file. " + e.getMessage(),LOGGER, InformationLevel.Negative);
            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("Error when generating feed file for sheet %s", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Error when generating feed file. " + e.getMessage(),
                        worksheet.getSpreadsheet().getSpreadsheetCountry());
            }

            return;
        }


        //submit feed to amazon via MWS Feed API
        String result;
        try {
            result = submitFeed(feedFile, worksheet);

            insertToLocalDbLog(feedFile,worksheet.getSpreadsheet().getSpreadsheetCountry(),result);

            //write log to worksheet
            writeLogToWorksheet(worksheet,result);

            //send email
            confirmShipmentEmailSender.sendSuccessEmail(result, feedFile, worksheet.getSpreadsheet().getSpreadsheetCountry());
        } catch (Exception e) {

            errorAlertService.sendMessage("Error when submitting order confirmation feed file via MWS.",e.getMessage(),feedFile);

            messagePanel.displayMsg("Error when submitting feed file. " + e.getMessage(),LOGGER, InformationLevel.Negative);
            messagePanel.displayMsg("Please try to submit the feed file via Amazon Seller Center.");
            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("Error when submitting feed file for sheet %s, feed file %s", worksheet.toString(), feedFile.getName());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Error when submitting feed file. " + e.getMessage(),
                        worksheet.getSpreadsheet().getSpreadsheetCountry());
            }




        }





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

        //start from row 3
        if (orders.isEmpty()) {
            return 3;
        }

        orders.sort(Comparator.comparing(Order::getRow));
        return orders.get(orders.size()-1).row;
    }


    public void writeLogToWorksheet(Worksheet worksheet, String result) {
        int[] counts = ServiceUtils.parseFeedSubmissionResult(result);

        String log = String.format("auto-confirmed; Total %s, Succeed %s, Failed %s",counts[0],counts[1],counts[2]);
        String now = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(System.currentTimeMillis());
        while (true) {
            try {
                appScript.commitShippingConfirmationLog(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName(), lastOrderRowNo + 1, log + "\n"+now);
                break;
            } catch (BusinessException e) {
                if (e.getMessage().contains("is not blank")) {
                    LOGGER.warn("Row {} is not blank, try to add to next row.",lastOrderRowNo);
                    lastOrderRowNo++;
                } else {
                    LOGGER.error(e.getMessage());
                    break;
                }

            }
        }




    }


    /**
     *
     * @param feedFile feedfile
     * @param worksheet worksheet
     * @return
     */
    public String submitFeed(File feedFile, Worksheet worksheet) {

        messagePanel.displayMsg("Feed submitted to Amazon... It may take few minutes for Amazon to process.");

        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(worksheet.getSpreadsheet().getSpreadsheetCountry()).getMwsCredential();

        String result = feedUploader.execute(feedFile, FeedGenerator.BatchFileType.ShippingConfirmation.feedType(), credential, 1);

        messagePanel.wrapLineMsg("Feed has been submitted successfully. " + result, LOGGER, InformationLevel.Important);

        return result;


    }

    /**
     * get orders from given worksheet. .
     *
     * @param worksheet
     * @return List<Order> orders
     */
    protected List<Order> getOrdersFromWorksheet(Worksheet worksheet) {

        List<Order> orders = new ArrayList<>();

        try {
            orders = appScript.getOrdersFromWorksheet(worksheet);
        } catch (NoWorksheetFoundException e) {

            LOGGER.error("No worksheet {} found in spreadsheet {}. {}", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle(), e.getMessage());

            messagePanel.displayMsg(String.format("No worksheet %s found in spreadsheet %s", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle()),
                    InformationLevel.Negative);

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("No worksheet %s found in spreadsheet %s", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Please check if orders have been exported correctly", worksheet.getSpreadsheet().getSpreadsheetCountry());
            }


        } catch (NoOrdersFoundInWorksheetException e) {
            messagePanel.displayMsg("No order data found on worksheet " + worksheet.getSheetName() + " in spreadsheet " + worksheet.getSpreadsheet().getTitle(),
                    InformationLevel.Negative);
            LOGGER.error("No order data found on worksheet {}  in spreadsheet {}. {}", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle(), e.getMessage());

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("No order data found on worksheet %s in spreadsheet %s", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Please check if orders have been exported correctly", worksheet.getSpreadsheet().getSpreadsheetCountry());
            }



        } catch (BusinessException e) {
            messagePanel.displayMsg("Failed to read order data from " + worksheet.toString() + ". Please try again later.",
                    InformationLevel.Negative);
            LOGGER.error("Failed to read order data {} - {}", worksheet.toString(), e.getMessage());

            if (messagePanel instanceof VirtualMessagePanel) {
                String subject = String.format("Failed to read order data from %s", worksheet.toString());
                confirmShipmentEmailSender.sendErrorFoundEmail(subject,
                        "Please check if orders have been exported correctly. \t " + e.getMessage(),
                        worksheet.getSpreadsheet().getSpreadsheetCountry());
            }


        }


        return orders;
    }


    public File generateFeedFile(Worksheet worksheet, List<Order> orders) {

        String defaultShipDate;
        try {
            defaultShipDate = worksheet.getOrderConfirmationDate();
        } catch (Exception e) {
            //todo
            throw new BusinessException("Order confirmation for non mm/dd sheet is currently not supported.");
//            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            LocalDate localDate = LocalDate.now();
//            defaultShipDate =  df.format(localDate);
//            LOGGER.warn("Sheet with name " + worksheet.getSheetName() + " cant convert to date format.");
        }

        //prepare data
        List<String[]> ordersToBeConfirmed = new ArrayList<>();

        for (Order order : orders) {

            OrderEnums.OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);

            String carrierCode = carrierHelper.getCarrierCodeByCountryAndType(Country.fromSalesChanel(order.sales_chanel), orderItemType);


            String[] row = {order.order_id, carrierCode, defaultShipDate};
            ordersToBeConfirmed.add(row);
        }


        //create feed file
        return this.feedGenerator.generateConfirmShipmentFeedFromRows(ordersToBeConfirmed, worksheet.getSpreadsheet().getSpreadsheetCountry(), worksheet.getSpreadsheet().getSpreadsheetType());

    }


    public String getSheetNameByDate(long millis) {
        return FastDateFormat.getInstance("MM/dd").format(millis);
    }

    public void execute() {


        //get all order update sheets for account
        List<String> spreadIds;
        try {
            spreadIds = Settings.load().listAllSpreadsheets();
        } catch (InvalidStateException e) {
            LOGGER.error("No configuration file found. {}", e.getMessage());
            throw new BusinessException("No configuration file found.");
        }

        //default to select today's sheet
        String sheetName = getSheetNameByDate(System.currentTimeMillis());

        //then confirm shipment for each spreadsheet
        for (String spreadId : spreadIds) {
            try {
                confirmShipmentForSpreadsheetId(spreadId, sheetName);
            } catch (Exception e) {
                LOGGER.error("Error when confirm shipment for sheet {} {}", spreadId, e.getMessage());
            }
        }


    }


    public void setMessagePanel(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
        shipmentOrderFilter.setMessagePanel(messagePanel);
    }

    public static void main(String[] args) {
        UITools.setTheme();

        ApplicationContext.getBean(ConfirmShipments.class).execute();

    }


}
