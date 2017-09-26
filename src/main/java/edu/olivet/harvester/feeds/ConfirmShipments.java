package edu.olivet.harvester.feeds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.FeedUploader;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.service.FeedGenerator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.model.service.Carrier;
import edu.olivet.harvester.model.service.OrderItemTypeHelper;
import edu.olivet.harvester.model.service.mws.OrderClient;
import edu.olivet.harvester.spreadsheet.*;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.UIHarvester;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Singleton
public class ConfirmShipments {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmShipments.class);

    @Inject
    private FeedGenerator feedGenerator;

    @Inject
    private OrderClient mwsOrderClient;

    @Inject
    private Carrier carrierHelper;

    @Inject
    private OrderItemTypeHelper orderItemTypeHelper;

    @Inject
    private UIHarvester uiHarvester;

    @Inject
    private AppScript appScript;

    @Inject
    private FeedUploader feedUploader;


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
    public void confirmShipmentForSpreadsheetId(String spreadsheetId, String sheetName) {

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


    public void confirmShipmentForWorksheet(Worksheet worksheet) {


        uiHarvester.getSuccessLogTextArea().append("Starting confirming shipments for worksheet " + worksheet.toString() + Constants.NEW_LINE);

        //get orders from google spreadsheet
        List<Order> orders = new ArrayList<>();
        try {
            orders = getOrdersFromWorksheet(worksheet);
        } catch (NoWorksheetFoundException e) {
            uiHarvester.getSuccessLogTextArea().append("No worksheet " + worksheet.getSheetName() + " found in spreadsheet " + worksheet.getSpreadsheet().getTitle() + Constants.NEW_LINE);
            LOGGER.error("No worksheet {} found in spreadsheet {}. {}", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle(), e.getMessage());
            return;
        } catch (NoOrdersFoundInWorksheetException e) {
            uiHarvester.getSuccessLogTextArea().append("No order data found on worksheet " + worksheet.getSheetName() + " in spreadsheet " + worksheet.getSpreadsheet().getTitle() + Constants.NEW_LINE);
            LOGGER.error("No order data found on worksheet {}  in spreadsheet {}. {}", worksheet.getSheetName(), worksheet.getSpreadsheet().getTitle(), e.getMessage());
            return;
        } catch (BusinessException e) {
            uiHarvester.getSuccessLogTextArea().append("Failed to read order data from " + worksheet.toString() + ". Please try again later." + Constants.NEW_LINE);
            LOGGER.error("Failed to read order data {} - {}", worksheet.toString(), e.getMessage());
            return;
        }


        uiHarvester.getSuccessLogTextArea().append(orders.size() + "  order(s) found on the worksheet. " + Constants.NEW_LINE);
        //filter orders
        orders = filterOrders(orders, worksheet);

        if (orders.isEmpty()) {
            LOGGER.error("No  orders need to be confirmed for sheet {}.", worksheet.toString());
            uiHarvester.getSuccessLogTextArea().append("No  orders need to be confirmed." + Constants.NEW_LINE);

            return;
        }

        uiHarvester.getSuccessLogTextArea().append(orders.size() + "  order(s) found to be confirmed. " + Constants.NEW_LINE);

        //create feed file
        try {
            File feedFile = generateFeedFile(worksheet, orders);

            MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(worksheet.getSpreadsheet().getSpreadsheetCountry()).getMwsCredential();

//            feedUploader.execute(feedFile, FeedUploader.FeedType.FULFILLMENT.code(), credential, 1);
        } catch (Exception e) {
            throw e;
        }


    }

    /**
     * get orders from given worksheet. exceptions will be handled by method caller.
     *
     * @param worksheet
     * @return List<Order> orders
     */
    protected List<Order> getOrdersFromWorksheet(Worksheet worksheet) {

        List<Order> orders = new ArrayList<>();
        try {
            orders = appScript.getOrdersFromWorksheet(worksheet);
        } catch (NoWorksheetFoundException e) {
            throw e;
        } catch (NoOrdersFoundInWorksheetException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        }

        return orders;
    }


    /**
     * @param orders
     * @return
     */
    private List<Order> filterOrders(List<Order> orders, Worksheet worksheet) {
        //remove duplicated orders. we only need unique AmazonOrderId here.
        Map<String, Order> filteredOrders = removeDulicatedOrders(orders);

        //wc code gray label orders do not need to confirm
        filteredOrders = removeWCGrayLabelOrders(filteredOrders);

        //check order status via MWS, only unshipped orders need to be confirmed
        filteredOrders = removeNotUnshippedOrders(filteredOrders, worksheet.getSpreadsheet().getSpreadsheetCountry());

        //return List
        List<Order> filteredList = new ArrayList<>();
        filteredOrders.forEach((orderId, order) -> {
            filteredList.add(order);
        });
        return filteredList;
    }

    /**
     * remove duplicated orders. we only need unique AmazonOrderId here.
     *
     * @param orders
     * @return
     */
    public Map<String, Order> removeDulicatedOrders(List<Order> orders) {
        Map<String, Order> filtered = new HashMap<String, Order>();

        for (Order order : orders) {
            if (!filtered.containsKey(order.order_id)) {
                filtered.put(order.order_id, order);
            } else {
                uiHarvester.getSuccessLogTextArea().append("Row " + order.getRow() + " " + order.order_id + " ignored since each order id only need to be confirmed once. " + Constants.NEW_LINE);
            }
        }

        return filtered;
    }

    /**
     * wc code gray label orders do not need to confirm
     *
     * @return
     */
    public Map<String, Order> removeWCGrayLabelOrders(Map<String, Order> orders) {

        Map<String, Order> filtered = new HashMap<String, Order>();

        orders.forEach((orderId, order) -> {
            if (!order.status.toLowerCase().equals(OrderEnums.Status.WaitCancel.value().toLowerCase())) {
                filtered.put(orderId, order);
            } else {
                uiHarvester.getSuccessLogTextArea().append("Row " + order.getRow() + " " + order.order_id + " ignored as it's marcked WC gray order. " + Constants.NEW_LINE);
            }
        });

        return filtered;
    }


    public Map<String, Order> removeNotUnshippedOrders(Map<String, Order> orders, Country country) {

        List<String> amazonOrderIds = new ArrayList<String>(orders.keySet());

        //todo: MWS API may not activated.
        List<com.amazonservices.mws.orders._2013_09_01.model.Order> amazonOrders = mwsOrderClient.getOrders(country, amazonOrderIds);

        Map<String, com.amazonservices.mws.orders._2013_09_01.model.Order> orderMap = new HashMap<String, com.amazonservices.mws.orders._2013_09_01.model.Order>();
        for (com.amazonservices.mws.orders._2013_09_01.model.Order order : amazonOrders) {
            orderMap.put(order.getAmazonOrderId(),order);
        }


        Map<String, Order> filtered = new HashMap<>(orders);

        orders.forEach((orderId,order) ->{
            if(orderMap.containsKey(orderId)) {
                com.amazonservices.mws.orders._2013_09_01.model.Order amzOrder = orderMap.get(orderId);

                if (!amzOrder.getOrderStatus().equals("Unshipped") && !amzOrder.getOrderStatus().equals("PartiallyShipped")) {
                    filtered.remove(orderId);

                    uiHarvester.getSuccessLogTextArea().append(
                        "Row " + order.getRow() + " " + order.order_id + " ignored as it's order status is " + amzOrder.getOrderStatus()
                                + Constants.NEW_LINE
                    );
                }
            }
        });

        return filtered;


        //            if (order.getOrderStatus().equals("Unshipped") || order.getOrderStatus().equals("PartiallyShipped")) {
//                filtered.put(order.getAmazonOrderId(), orders.get(order.getAmazonOrderId()));
//            } else {
//                uiHarvester.getSuccessLogTextArea().append(
//                        "Row " + orders.get(order.getAmazonOrderId()).getRow() + " " + order.getAmazonOrderId() + " ignored as it's order status is " + order.getOrderStatus()
//                                + Constants.NEW_LINE
//                );
//            }



        //return orders;

    }


    public File generateFeedFile(Worksheet worksheet, List<Order> orders) {

        String defaultShipDate = null;
        try{
            defaultShipDate = worksheet.getOrderConfirmationDate();
        }catch (Exception e) {
            //todo
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.now();
            defaultShipDate =  df.format(localDate);
            LOGGER.warn("Sheet with name " + worksheet.getSheetName() + " cant convert to date format.");
        }

        //prepare data
        List<String[]> ordersToBeConfirmed = new ArrayList<>();

        for (Order order : orders) {

            OrderEnums.OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
            String carrierCode = carrierHelper.getCarrierCodeByCountryAndType(Country.fromSalesChanel(order.sales_chanel), orderItemType);

//            String shipDate = defaultShipDate;
//            if(shipDate == null) {
//                shipDate = order.expectedShipDate();
//            }


            String[] row = {order.order_id, carrierCode, defaultShipDate};
            ordersToBeConfirmed.add(row);
        }


        //create feed file
        File feedFile = this.feedGenerator.generateConfirmShipmentFeedFromRows(ordersToBeConfirmed, worksheet.getSpreadsheet().getSpreadsheetCountry(), worksheet.getSpreadsheet().getSpreadsheetType());

        System.out.println(feedFile.getAbsolutePath());

        return feedFile;
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
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/d");
        LocalDate localDate = LocalDate.now();
        String sheetName = df.format(localDate);

        //then confirm shipment for each spreadsheet
        for (String spreadId : spreadIds) {
            try {
                confirmShipmentForSpreadsheetId(spreadId, sheetName);
            } catch (Exception e) {
                LOGGER.error("Error when confirm shipment for sheet {} {}", spreadId, e.getMessage());
            }
        }


    }

    public static void main(String[] args) {
        UITools.setTheme();
        ApplicationContext.getBean(ConfirmShipments.class).execute();
    }


}
