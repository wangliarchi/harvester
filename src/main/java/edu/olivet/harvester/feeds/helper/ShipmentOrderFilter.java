package edu.olivet.harvester.feeds.helper;

import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
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

        //check first order. if first order is from last year, and more than 30 days before current day. it should be last year's sheet
        try {
            Order firstOrder = orders.get(0);
            if (Dates.getYear(firstOrder.getPurchaseDate()) != Dates.getYear(new Date()) && firstOrder.getPurchaseDate().before(minDate)) {
                orders.clear();
                LOGGER.error("Sheet is from last year.");
                return orders;
            }
        } catch (Exception e) {
            LOGGER.error("Fail to check first order for {}",worksheet,e);
        }

        orders.removeIf(it -> {
            try {
                return it.getPurchaseDate().before(minDate);
            } catch (Exception e) {
                LOGGER.error("", e);
            }
            return false;
        });

        //remove duplicated orders. we only need unique AmazonOrderId here.
        List<Order> filteredOrders = removeDuplicatedOrders(orders, resultSummary, resultDetail);

        //wc code gray label orders do not need to confirm
        filteredOrders = removeWCGrayLabelOrders(filteredOrders, resultSummary, resultDetail);

        //check order status via MWS, only unshipped orders need to be confirmed
        filteredOrders = removeNotUnshippedOrders(filteredOrders, resultSummary, resultDetail);

        //return List
        filteredOrders.sort(Comparator.comparing(Order::getRow));

        return filteredOrders;
    }

    /**
     * remove duplicated orders. we only need unique AmazonOrderId here.
     */
    public List<Order> removeDuplicatedOrders(List<Order> orders, StringBuilder resultSummary, StringBuilder resultDetail) {
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
            resultSummary.append(duplicatedOrderIds.size()).append(" duplicated; ");
            resultDetail.append(String.format("%d duplicated: ", duplicatedOrderIds.size())).append("\n")
                    .append(StringUtils.join(duplicatedOrderIds, "\n")).append("\n\n");
        }

        return new ArrayList<>(filtered.values());
    }

    /**
     * wc code gray label orders do not need to confirm
     */
    public List<Order> removeWCGrayLabelOrders(List<Order> orders, StringBuilder resultSummary, StringBuilder resultDetail) {

        List<Order> filtered = new ArrayList<>();

        List<String> grayWCOrderIds = new ArrayList<>();

        orders.forEach(order -> {
            if (!OrderEnums.Status.WaitCancel.value().toLowerCase().equals(order.status.toLowerCase())) {
                filtered.add(order);
            } else {
                messagePanel.displayMsg("Row " + order.getRow() + " " + order.order_id + " ignored as it's marked WC gray order. ",
                        LOGGER, InformationLevel.Negative);
                grayWCOrderIds.add(order.order_id);
            }
        });


        if (!grayWCOrderIds.isEmpty()) {
            resultSummary.append(grayWCOrderIds.size()).append(" gray WC; ");
            resultDetail.append(String.format("%d gray WC: ", grayWCOrderIds.size())).append("\n")
                    .append(StringUtils.join(grayWCOrderIds, "\n")).append("\n\n");
        }

        return filtered;
    }


    public List<Order> removeNotUnshippedOrders(List<Order> orders, StringBuilder resultSummary, StringBuilder resultDetail) {

        List<Order> filtered = new ArrayList<>();
        List<Order> shipped = new ArrayList<>();
        List<Order> canceled = new ArrayList<>();

        orders.forEach(order -> {
            if (StringUtils.isNotEmpty(order.getAmazonOrderStatus()) &&
                    !"Unshipped".equals(order.getAmazonOrderStatus()) && !"PartiallyShipped".equals(order.getAmazonOrderStatus())) {
                messagePanel.displayMsg(
                        "Row " + order.getRow() + " " + order.order_id + " ignored. Order status is " + order.getAmazonOrderStatus(),
                        LOGGER, InformationLevel.Negative
                );

                if ("Shipped".equalsIgnoreCase(order.getAmazonOrderStatus())) {
                    shipped.add(order);
                } else if ("Canceled".equalsIgnoreCase(order.getAmazonOrderStatus())) {
                    canceled.add(order);
                }
            } else {
                filtered.add(order);
            }


        });

        if (!shipped.isEmpty()) {
            resultSummary.append(shipped.size()).append(" shipped; ");
            resultDetail.append(String.format("%d shipped: ", shipped.size())).append("\n")
                    .append(StringUtils.join(shipped.stream().map(it -> it.order_id).collect(Collectors.toSet()), "\n")).append("\n\n");
        }

        if (!canceled.isEmpty()) {
            resultSummary.append(canceled.size()).append(" canceled; ");
            resultDetail.append(String.format("%d canceled: ", canceled.size())).append("\n")
                    .append(StringUtils.join(canceled.stream().map(it -> it.order_id).collect(Collectors.toSet()), "\n")).append("\n\n");
        }

        return filtered;
    }


}
