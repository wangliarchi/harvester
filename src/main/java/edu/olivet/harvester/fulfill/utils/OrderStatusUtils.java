package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:39 PM
 */
public class OrderStatusUtils {


    /**
     * 根据当前订单数据决定需要标识的状态，注意：分支判断的顺序<strong>不能颠倒!</strong>
     */
    public static String determineStatus(Order order) {
        if (OrderEnums.Status.Finish.value().equalsIgnoreCase(order.status) || OrderEnums.Status.Skip.value().equalsIgnoreCase(order.status) || order.toBeChecked()) {
            return null;
        }

        Country finalAmazonCountry;
        try {
            finalAmazonCountry = OrderCountryUtils.getFulfillementCountry(order);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // 客户改变地址、灰条等情况目前略过
        if (order.needBuyAndTransfer()) {
            if (order.sellerIsPrime()) {
                return OrderEnums.Status.PrimeBuyAndTransfer.value();
            } else {
                return OrderEnums.Status.BuyAndTransfer.value();
            }
        } else if (order.seller.toLowerCase().startsWith("bw-") || order.seller.toLowerCase().equals("bw")) {
            return OrderEnums.Status.SellerIsBetterWorld.value();
        } else if (order.seller.toLowerCase().startsWith("half-") || order.seller.toLowerCase().equals("half")) {
            return OrderEnums.Status.SellerIsHalf.value();
        } else if (order.seller.toLowerCase().startsWith("in-") || order.seller.toLowerCase().equals("in")) {
            return OrderEnums.Status.SellerIsIngram.value();
        } else if (!finalAmazonCountry.code().equals(CountryStateUtils.getInstance().getCountryCode(order.ship_country))) {
            return OrderEnums.Status.International.value();
        } else if (order.sellerIsPrime()) {
            return OrderEnums.Status.PrimeSeller.value();
        } else if (order.sellerIsPt()) {
            return OrderEnums.Status.CommonSeller.value();
        } else {
            return null;
        }
    }
}
