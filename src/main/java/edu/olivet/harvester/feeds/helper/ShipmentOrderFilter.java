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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
    public List<Order> filterOrders(List<Order> orders, Worksheet worksheet, StringBuilder resultSummary, StringBuilder resultDetail) {


        //if order purchased date is over maxDaysBack days, ignore???
        Date minDate = DateUtils.addDays(new Date(), -30);
        orders.removeIf(it -> {
            try {
                return it.getPurchaseDate().before(minDate);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
            return false;
        });

        //remove duplicated orders. we only need unique AmazonOrderId here.
        Map<String, Order> filteredOrders = removeDuplicatedOrders(orders, resultSummary, resultDetail);

        //wc code gray label orders do not need to confirm
        filteredOrders = removeWCGrayLabelOrders(filteredOrders, resultSummary, resultDetail);

        //check order status via MWS, only unshipped orders need to be confirmed
        filteredOrders = removeNotUnshippedOrders(filteredOrders, worksheet.getSpreadsheet().getSpreadsheetCountry(), resultSummary, resultDetail);

        //return List
        List<Order> filteredList = new ArrayList<>();
        filteredOrders.forEach((orderId, order) -> filteredList.add(order));

        filteredList.sort(Comparator.comparing(Order::getRow));

        return filteredList;
    }

    /**
     * remove duplicated orders. we only need unique AmazonOrderId here.
     */
    public Map<String, Order> removeDuplicatedOrders(List<Order> orders, StringBuilder resultSummary, StringBuilder resultDetail) {
        Map<String, Order> filtered = new LinkedHashMap<>();
        List<String> duplicatedOrderIds = new ArrayList<>();
        for (Order order : orders) {
            if (!filtered.containsKey(order.order_id)) {
                filtered.put(order.order_id, order);
            } else {
                messagePanel.displayMsg(
                        "Row " + order.getRow() + " " + order.order_id + " ignored since each order id only need to be confirmed once. ",
                        LOGGER, InformationLevel.Negative);
                duplicatedOrderIds.add(order.order_id);
            }
        }

        if (!duplicatedOrderIds.isEmpty()) {
            resultSummary.append(duplicatedOrderIds.size()).append("duplicated; ");
            resultDetail.append(String.format("%d duplicated: ", duplicatedOrderIds.size())).append("\n")
                    .append(StringUtils.join(duplicatedOrderIds, "\n")).append("\n\n");
        }

        return filtered;
    }

    /**
     * wc code gray label orders do not need to confirm
     */
    public Map<String, Order> removeWCGrayLabelOrders(Map<String, Order> orders, StringBuilder resultSummary, StringBuilder resultDetail) {

        Map<String, Order> filtered = new LinkedHashMap<>();

        List<String> grayWCOrderIds = new ArrayList<>();

        orders.forEach((orderId, order) -> {
            if (!OrderEnums.Status.WaitCancel.value().toLowerCase().equals(order.status.toLowerCase())) {
                filtered.put(orderId, order);
            } else {
                messagePanel.displayMsg("Row " + order.getRow() + " " + order.order_id + " ignored as it's marcked WC gray order. ",
                        LOGGER, InformationLevel.Negative);
                grayWCOrderIds.add(order.order_id);
            }
        });


        if (!grayWCOrderIds.isEmpty()) {
            resultSummary.append(grayWCOrderIds.size()).append("gray WC; ");
            resultDetail.append(String.format("%d gray WC: ", grayWCOrderIds.size())).append("\n")
                    .append(StringUtils.join(grayWCOrderIds, "\n")).append("\n\n");
        }

        return filtered;
    }


    public Map<String, Order> removeNotUnshippedOrders(Map<String, Order> orders, Country country, StringBuilder resultSummary, StringBuilder resultDetail) {

        List<String> amazonOrderIds = new ArrayList<>(orders.keySet());

        //todo: MWS API may not activated.
        List<com.amazonservices.mws.orders._2013_09_01.model.Order> amazonOrders;
        try {
            amazonOrders = mwsOrderClient.getOrders(country, amazonOrderIds);
        } catch (Exception e) {
            LOGGER.error("Error load order info via MWS for country {}, {}", country.name(), e.getMessage());
            return orders;
        }


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

        if (!shipped.isEmpty()) {
            resultSummary.append(shipped.size()).append("shipped; ");
            resultDetail.append(String.format("%d shipped: ", shipped.size())).append("\n")
                    .append(StringUtils.join(shipped.stream().map(it -> it.order_id).collect(Collectors.toSet()), "\n")).append("\n\n");
        }

        if (!canceled.isEmpty()) {
            resultSummary.append(canceled.size()).append("canceled; ");
            resultDetail.append(String.format("%d canceled: ", canceled.size())).append("\n")
                    .append(StringUtils.join(canceled.stream().map(it -> it.order_id).collect(Collectors.toSet()), "\n")).append("\n\n");
        }

        return filtered;


    }
}
