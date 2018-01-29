package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Order;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:13 PM
 */
public class OrderBuyerUtils {

    public static Account getBuyer(Order order) {
        if (order.sellerIsPrime()) {
            String buyerEmail = order.getRuntimeSettings().getPrimeBuyerAccount();
            return BuyerAccountSettingUtils.load().getByEmail(buyerEmail).getBuyerAccount();
        }

        return BuyerAccountSettingUtils.load().getByEmail(order.getRuntimeSettings().getBuyerAccount()).getBuyerAccount();
    }


    public static Account getBuyer(Order order, RuntimeSettings runtimeSettings) {
        if (order.sellerIsPrime()) {
            return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getPrimeBuyerEmail()).getBuyerAccount();
        }
        return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getBuyerEmail()).getBuyerAccount();
    }


}
