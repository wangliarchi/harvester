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

import java.util.*;

public class ShipmentOrderFilter {

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
    public List<Order> filterOrders(List<Order> orders, Worksheet worksheet) {
        //remove duplicated orders. we only need unique AmazonOrderId here.
        Map<String, Order> filteredOrders = removeDuplicatedOrders(orders);

        //wc code gray label orders do not need to confirm
        filteredOrders = removeWCGrayLabelOrders(filteredOrders);

        //check order status via MWS, only unshipped orders need to be confirmed
        filteredOrders = removeNotUnshippedOrders(filteredOrders, worksheet.getSpreadsheet().getSpreadsheetCountry());

        //return List
        List<Order> filteredList = new ArrayList<>();
        filteredOrders.forEach((orderId, order) -> filteredList.add(order));

        filteredList.sort(Comparator.comparing(Order::getRow));

        return filteredList;
    }

    /**
     * remove duplicated orders. we only need unique AmazonOrderId here.
     */
    public Map<String, Order> removeDuplicatedOrders(List<Order> orders) {
        Map<String, Order> filtered = new LinkedHashMap<>();

        for (Order order : orders) {
            if (!filtered.containsKey(order.order_id)) {
                filtered.put(order.order_id, order);
            } else {
                messagePanel.displayMsg(
                        "Row " + order.getRow() + " " + order.order_id + " ignored since each order id only need to be confirmed once. ",
                        InformationLevel.Negative);
            }
        }

        return filtered;
    }

    /**
     * wc code gray label orders do not need to confirm
     *
     *
     */
    public Map<String, Order> removeWCGrayLabelOrders(Map<String, Order> orders) {

        Map<String, Order> filtered = new LinkedHashMap<>();

        orders.forEach((orderId, order) -> {
            if (! OrderEnums.Status.WaitCancel.value().toLowerCase().equals(order.status.toLowerCase())) {
                filtered.put(orderId, order);
            } else {
                messagePanel.displayMsg("Row " + order.getRow() + " " + order.order_id + " ignored as it's marcked WC gray order. ",
                        InformationLevel.Negative);
            }
        });

        return filtered;
    }


    public Map<String, Order> removeNotUnshippedOrders(Map<String, Order> orders, Country country) {

        List<String> amazonOrderIds = new ArrayList<>(orders.keySet());

        //todo: MWS API may not activated.
        List<com.amazonservices.mws.orders._2013_09_01.model.Order> amazonOrders = mwsOrderClient.getOrders(country, amazonOrderIds);

        Map<String, com.amazonservices.mws.orders._2013_09_01.model.Order> orderMap = new HashMap<>();
        for (com.amazonservices.mws.orders._2013_09_01.model.Order order : amazonOrders) {
            orderMap.put(order.getAmazonOrderId(), order);
        }


        Map<String, Order> filtered = new LinkedHashMap<>(orders);

        orders.forEach((orderId, order) -> {
            if (orderMap.containsKey(orderId)) {
                com.amazonservices.mws.orders._2013_09_01.model.Order amzOrder = orderMap.get(orderId);

                if (!"Unshipped".equals(amzOrder.getOrderStatus()) && !"PartiallyShipped".equals(amzOrder.getOrderStatus())) {
                    filtered.remove(orderId);

                    messagePanel.displayMsg(
                            "Row " + order.getRow() + " " + order.order_id + " ignored. Order status is " + amzOrder.getOrderStatus(),
                            InformationLevel.Negative
                    );


                    //If order is canceled, update in order update sheet: Column A (status) to finish,
                    // Column AD (remark) to buyer canceled/ refunded

                }
            }
        });

        return filtered;


    }
}
