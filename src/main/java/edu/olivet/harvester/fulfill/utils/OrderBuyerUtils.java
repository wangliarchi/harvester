package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:13 PM
 */
public class OrderBuyerUtils {

    public static Account getBuyer(Order order, OrderSubmissionTask task) {
        OrderItemType type = order.type();

        if (order.sellerIsPrime()) {
            return BuyerAccountSettingUtils.load().getByEmail(task.getPrimeBuyerAccount()).getBuyerAccount();
        }

        return BuyerAccountSettingUtils.load().getByEmail(task.getBuyerAccount()).getBuyerAccount();
    }

    public static Account getBuyer(Order order, RuntimeSettings runtimeSettings) {
        if (order.sellerIsPrime()) {
            return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getPrimeBuyerEmail()).getBuyerAccount();
        }
        return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getBuyerEmail()).getBuyerAccount();
    }


}
