package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;

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

    public static Account getBuyer(Order order) {
        Country country = RuntimeSettings.load().getCurrentCountry();
        OrderItemType type = order.type();
        Configuration config = Settings.load().getConfigByCountry(country);


        if (type == OrderItemType.BOOK) {
            if (order.sellerIsPrime()) {
                return config.getPrimeBuyer();
            }
            return config.getBuyer();
        } else {
            if (order.sellerIsPrime()) {
                return config.getProdPrimeBuyer();
            }
            return config.getProdBuyer();
        }


    }


}
