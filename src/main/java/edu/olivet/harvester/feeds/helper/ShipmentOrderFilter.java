package edu.olivet.harvester.feeds.helper;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.service.mws.OrderClient;
import edu.olivet.harvester.spreadsheet.Worksheet;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ShipmentOrderFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentOrderFilter.class);
    @Inject
    @Setter
    private OrderClient mwsOrderClient;

    @Setter
    @Getter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    /**
     * @param orders list of orders
     * @return orders fitlered orders
     */
    public List<Order> filterOrders(List<Order> orders, Worksheet worksheet, StringBuilder resultSummary) {


        //if order purchased date is over maxDaysBack days, ignore???
        Date minDate =  DateUtils.addDays(new Date(), -30);
        orders.removeIf(it -> {
            try {
                return it.getPurchaseDate().before(minDate);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
            return false;
        });

        //remove duplicated orders. we only need unique AmazonOrderId here.
        Map<String, Order> filteredOrders = removeDuplicatedOrders(orders, resultSummary);

        //wc code gray label orders do not need to confirm
        filteredOrders = removeWCGrayLabelOrders(filteredOrders, resultSummary);

        //check order status via MWS, only unshipped orders need to be confirmed
        filteredOrders = removeNotUnshippedOrders(filteredOrders, worksheet.getSpreadsheet().getSpreadsheetCountry(), resultSummary);

        //return List
        List<Order> filteredList = new ArrayList<>();
        filteredOrders.forEach((orderId, order) -> filteredList.add(order));

        filteredList.sort(Comparator.comparing(Order::getRow));

        return filteredList;
    }

    /**
     * remove duplicated orders. we only need unique AmazonOrderId here.
     */
    public Map<String, Order> removeDuplicatedOrders(List<Order> orders, StringBuilder resultSummary) {
        Map<String, Order> filtered = new LinkedHashMap<>();

        for (Order order : orders) {
            if (!filtered.containsKey(order.order_id)) {
                filtered.put(order.order_id, order);
            } else {

                messagePanel.displayMsg(
                        "Row " + order.getRow() + " " + order.order_id + " ignored since each order id only need to be confirmed once. ",
                        LOGGER, InformationLevel.Negative);
            }
        }

        if (orders.size() - filtered.size() > 0) {
            resultSummary.append(String.format("%d duplicated, ", orders.size() - filtered.size()));
        }

        return filtered;
    }

    /**
     * wc code gray label orders do not need to confirm
     */
    public Map<String, Order> removeWCGrayLabelOrders(Map<String, Order> orders, StringBuilder resultSummary) {

        Map<String, Order> filtered = new LinkedHashMap<>();

        orders.forEach((orderId, order) -> {
            if (!OrderEnums.Status.WaitCancel.value().toLowerCase().equals(order.status.toLowerCase())) {
                filtered.put(orderId, order);
            } else {
                messagePanel.displayMsg("Row " + order.getRow() + " " + order.order_id + " ignored as it's marcked WC gray order. ",
                        LOGGER, InformationLevel.Negative);
            }
        });


        if (orders.size() - filtered.size() > 0) {
            resultSummary.append(String.format("%d WC, ", orders.size() - filtered.size()));
        }

        return filtered;
    }


    public Map<String, Order> removeNotUnshippedOrders(Map<String, Order> orders, Country country, StringBuilder resultSummary) {

        List<String> amazonOrderIds = new ArrayList<>(orders.keySet());

        //todo: MWS API may not activated.
        List<com.amazonservices.mws.orders._2013_09_01.model.Order> amazonOrders = mwsOrderClient.getOrders(country, amazonOrderIds);

        Map<String, com.amazonservices.mws.orders._2013_09_01.model.Order> orderMap = new HashMap<>();
        for (com.amazonservices.mws.orders._2013_09_01.model.Order order : amazonOrders) {
            orderMap.put(order.getAmazonOrderId(), order);
        }


        Map<String, Order> filtered = new LinkedHashMap<>(orders);

        List<Order> shipped = new ArrayList<>();
        List<Order> canceled = new ArrayList<>();
        orders.forEach((orderId, order) -> {
            if (orderMap.containsKey(orderId)) {
                com.amazonservices.mws.orders._2013_09_01.model.Order amzOrder = orderMap.get(orderId);

                if (!"Unshipped".equals(amzOrder.getOrderStatus()) && !"PartiallyShipped".equals(amzOrder.getOrderStatus())) {
                    filtered.remove(orderId);

                    messagePanel.displayMsg(
                            "Row " + order.getRow() + " " + order.order_id + " ignored. Order status is " + amzOrder.getOrderStatus(),
                            LOGGER, InformationLevel.Negative
                    );

                    if ("Shipped".equalsIgnoreCase(amzOrder.getOrderStatus())) {
                        shipped.add(order);
                    } else if ("Canceled".equalsIgnoreCase(amzOrder.getOrderStatus())) {
                        canceled.add(order);
                    }
                } else {
                    filtered.get(orderId).setSales_chanel(amzOrder.getSalesChannel());
                }


            }
        });

        if (shipped.size() > 0) {
            resultSummary.append(String.format("%d shipped, ", shipped.size()));
        }

        if (canceled.size() > 0) {
            resultSummary.append(String.format("%d canceled, ", canceled.size()));
        }

        return filtered;


    }
}
