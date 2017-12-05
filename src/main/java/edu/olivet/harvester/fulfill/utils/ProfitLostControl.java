package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.model.Order;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 6:09 PM
 */
public class ProfitLostControl {

    /**
     * order earning in USD
     */
    public static float earning(Order order) {
        String qty = StringUtils.isNotBlank(order.quantity_fulfilled) ? order.quantity_fulfilled : order.quantity_purchased;
        return (order.getOrderTotalPrice().toUSDAmount().floatValue() * 0.85f - 1.8f) * Float.parseFloat(qty);
    }

    /**
     * order profit in USD
     */
    public static float profit(Order order, Float cost) {
        float earning = earning(order);
        return earning - cost;
    }

    /**
     * <pre>
     * uk shipment的订单：亏钱不做单
     * 跳过所有检查和跳过利润检查的情况下，亏损上限是20
     * 标zuoba，做吧，Place The Order的情况下，亏损上限是 zuoba后面的值，但是仍然不能超过20，如果没有特别标明，就是20
     * 没有设置跳过检查，也没有标zuoba的时候，亏损上限可以在orderman里面设置，有7和5两个选项。
     *
     * cost is in USD
     * </pre>
     */
    public static boolean canPlaceOrder(Order order, Float cost) {

        float lostLimit;
        if (order.fulfilledFromUK()) {
            lostLimit = 0;
        } else if (OrderValidator.skipCheck(order, OrderValidator.SkipValidation.Profit)) {
            lostLimit = -20;
        } else {
            //RuntimeSettings settings = RuntimeSettings.load();
            //lostLimit = Float.parseFloat(settings.getLostLimit());
            if (cost <= 20) {
                lostLimit = -5;
            } else {
                lostLimit = cost * 0.05f;
            }
        }


        float profit = profit(order, cost);
        return profit - lostLimit >= 0;
    }

}
