package edu.olivet.harvester.selforder;

import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.feeds.helper.ShipmentOrderFilter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 5/15/2018 10:28 AM
 */
public class SelfOrderFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentOrderFilter.class);

    @Setter
    @Getter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    /**
     *  @param orders List of orders
     *  @return orders filtered orders
     */

    public List<Order> filterOrders(List<Order> orders){

        orders.removeIf(it->!it.selfBuy());

        // Remove self orders that already refunded.
        List<Order> filteredOrders = removeRefundedOrders(orders);


        return filteredOrders;


    }

    public List<Order> removeRefundedOrders(List<Order> orders){

        List<Order> filtered = new ArrayList<>();
//        List<String> refundOrderIds = new ArrayList<>();

        for (Order order : orders) {
            if (Strings.containsAnyIgnoreCase(order.order_number, "refunded")) {
                continue;
            }
            filtered.add(order);

            }

        return filtered;
    }


}













