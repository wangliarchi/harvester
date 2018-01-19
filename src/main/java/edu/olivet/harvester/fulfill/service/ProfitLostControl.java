package edu.olivet.harvester.fulfill.service;

import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.utils.common.NumberUtils;
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
        Float value = (order.getOrderTotalPrice().toUSDAmount().floatValue() * 0.85f - 1.8f) * Float.parseFloat(qty);
        return NumberUtils.round(value, 2);
    }

    /**
     * order profit in USD
     */
    public static float profit(Order order, Float cost) {
        float earning = earning(order);
        return NumberUtils.round(earning - cost, 2);
    }

    /**
     * <pre>
     * uk shipment的订单：亏钱不做单
     * 跳过所有检查和跳过利润检查的情况下，亏损上限是20
     * 标zuoba，做吧，Place The Order的情况下，亏损上限是 zuoba后面的值，但是仍然不能超过20，如果没有特别标明，就是20
     * 没有设置跳过检查，也没有标zuoba的时候，亏损上限可以在orderman里面设置，有7和5两个选项。
     *
     * cost is in USD
     * 12/04/2017
     * 对于利润判断：下午跟**干事和数据改价一起商量了，可以按照这个标准来
     * seller price 在$20以下（包括20），赔钱5美金以内可以做单（包括5美金）；
     * seller price 大于$20 ，利润小于seller price*5% 不做单
     * 这个标准做成默认标准；
     * 不过这个之外希望保留一个可以人工填写的可变标准。
     * 其中$20  -5  5% 这三个量设成可以人工填写的变量。可变标准一般情况无法启动，需要中央允许的情况下可以开启
     * </pre>
     */
    public static boolean canPlaceOrder(Order order, Float cost) {
        float lostLimit;
        if (order.fulfilledFromUK()) {
            lostLimit = 0;
        } else if (OrderValidator.skipCheck(order, OrderValidator.SkipValidation.Profit)) {
            lostLimit = -20;
        } else {
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
