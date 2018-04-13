package edu.olivet.harvester.fulfill.service;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.NumberUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.nutz.lang.Lang;

import java.io.IOException;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 6:09 PM
 */
public class ProfitLostControl {
    private static final String APPSCRIPT_URL = "https://script.google.com/macros/s/AKfycbz92MpAp86yAfYkekA8eSv6gYyUrg0BCzW33i1VgnaBwOgK4THe/exec";

    @Repeat(expectedExceptions = BusinessException.class)
    static String get(Country country) {
        String url = APPSCRIPT_URL + "?account=" + Settings.load().getSid() + (country.europe() ? "EU" : country.name());
        try {
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }

    @Data
    public static class ProfitControlVariable {
        float breakPoint;
        float min1;
        float min2;
    }

    public static ProfitControlVariable profitControlVariable = null;

    public static ProfitControlVariable getVariable(Country country) {
        if (profitControlVariable == null) {
            try {
                String json = get(country);
                profitControlVariable = JSON.parseObject(json, ProfitControlVariable.class);
            } catch (Exception e) {
                //fail to load from service
                profitControlVariable = new ProfitControlVariable();
                profitControlVariable.setBreakPoint(20);
                profitControlVariable.setMin1(-5);
                profitControlVariable.setMin2(0.05f);
            }
        }
        return profitControlVariable;
    }

    /**
     * order earning in USD
     */
    public static float earning(Order order) {
        String qty = StringUtils.isNotBlank(order.quantity_fulfilled) ? order.quantity_fulfilled : order.quantity_purchased;
        float value = order.getAmazonPayout().toUSDAmount().floatValue() * Float.parseFloat(qty);
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
        //0
        float lostLimit = getLostLimit(order, cost);
        float profit = profit(order, cost);

        return profit - lostLimit >= 0;
    }


    public static float getLostLimit(Order order, Float cost) {
        ProfitControlVariable profitControlVariable = getVariable(OrderCountryUtils.getMarketplaceCountry(order));
        float lostLimit;
        if (order.fulfilledFromUK()) {
            lostLimit = 0;
        } else if (OrderValidator.skipCheck(order, OrderValidator.SkipValidation.Profit)) {
            lostLimit = -20;
        } else {
            if (cost <= profitControlVariable.breakPoint) {
                lostLimit = profitControlVariable.min1;
            } else {
                lostLimit = cost * profitControlVariable.min2;
            }

            if (order.purchaseBack()) {
                lostLimit += 10;
            }
        }

        return lostLimit;
    }

    public static void main(String[] args) {
        ProfitLostControl.getVariable(Country.US);
    }

}
