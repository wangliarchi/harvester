package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.Order;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:13 PM
 */
public class OrderBuyerUtils {

    public static Account getBuyer(Order order) {
        if (order.sellerIsPrime()) {
            return BuyerAccountSettingUtils.load().getByEmail(order.getTask().getPrimeBuyerAccount()).getBuyerAccount();
        }

        return BuyerAccountSettingUtils.load().getByEmail(order.getTask().getBuyerAccount()).getBuyerAccount();
    }


    public static Account getBuyer(Order order, RuntimeSettings runtimeSettings) {
        if (order.sellerIsPrime()) {
            return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getPrimeBuyerEmail()).getBuyerAccount();
        }
        return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getBuyerEmail()).getBuyerAccount();
    }


}
