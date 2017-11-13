package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.model.Order;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 6:09 PM
 */
public class ProfitLostControl {
    public static float earning(Order order) {
        String qty = StringUtils.isNotBlank(order.quantity_fulfilled) ? order.quantity_fulfilled : order.quantity_purchased;
        return ((Float.parseFloat(order.price) + Float.parseFloat(order.shipping_fee)) * 0.85f - 1.8f) * Float.parseFloat(qty);
    }

    public static float profit(Order order, Float cost) {
        float earning = earning(order);
        return earning - cost;
    }

    public static boolean canPlaceOrder(Order order, Float cost) {
        if (StringUtils.containsIgnoreCase(order.remark, "zuoba")) {
            return true;
        }
        RuntimeSettings settings = RuntimeSettings.load();
        float lostLimit = Float.parseFloat(settings.getLostLimit());
        float profit = profit(order, cost);
        return !(profit + lostLimit < 0);
    }

}
