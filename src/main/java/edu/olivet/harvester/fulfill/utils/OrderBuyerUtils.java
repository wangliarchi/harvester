package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:13 PM
 */
public class OrderBuyerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderBuyerUtils.class);

    public static Account getBuyer(Order order) {
        if (order.sellerIsPrime()) {
            String buyerEmail = order.getRuntimeSettings().getPrimeBuyerAccount();
            return BuyerAccountSettingUtils.load().getByEmail(buyerEmail).getBuyerAccount();
        }

        BuyerAccountSetting buyerAccountSetting = BuyerAccountSettingUtils.load().getByEmail(order.getRuntimeSettings().getBuyerAccount());
        if (buyerAccountSetting == null) {
            LOGGER.error("No buyer account found for {}", order.getRuntimeSettings().getBuyerAccount());
            throw new BusinessException("No buyer account found for " + order.getRuntimeSettings().getBuyerAccount());
        }
        return buyerAccountSetting.getBuyerAccount();
    }


    public static Account getBuyer(Order order, RuntimeSettings runtimeSettings) {
        if (order.sellerIsPrime()) {
            return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getPrimeBuyerEmail()).getBuyerAccount();
        }
        return BuyerAccountSettingUtils.load().getByEmail(runtimeSettings.getBuyerEmail()).getBuyerAccount();
    }


}
