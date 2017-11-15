package edu.olivet.harvester.fulfill.utils;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.model.CreditCard;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.ui.Harvester;
import edu.olivet.harvester.utils.Settings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:13 PM
 */
public class OrderBuyerUtils {
    public static Account getBuyer(Order order) {
        Settings settings = Settings.load();
        Settings.Configuration config = settings.getConfigByCountry(OrderCountryUtils.getFulfillementCountry(order));
        OrderEnums.OrderItemType type = settings.getSpreadsheetType(order.getSpreadsheetId());

        //Direct shipment uses non prime account
        if (type == OrderEnums.OrderItemType.BOOK) {
            if (order.sellerIsPrime() && !order.isDirectShip()) {
                return config.getPrimeBuyer();
            }
            return config.getBuyer();
        } else {
            if (order.sellerIsPrime() && !order.isDirectShip()) {
                return config.getProdPrimeBuyer();
            }
            return config.getProdBuyer();
        }


    }

    public static CreditCard getCreditCard(Account buyer) {
        Map<String, CreditCard> creditCards = loadCreditCards();
        if (creditCards.containsKey(buyer.getEmail().toLowerCase())) {
            return creditCards.get(buyer.getEmail().toLowerCase());
        }
        throw new BusinessException("No credit card configed for buyer account " + buyer.getEmail());
    }

    public static CreditCard getCreditCard(Order order) {
        Account buyer = getBuyer(order);
        return getCreditCard(buyer);
    }

    public static Map<String, CreditCard> loadCreditCards() {
        File file = new File(Harvester.CC_CONFIG_FILE_PATH);
        Map<String, CreditCard> creditCards = new HashMap<>();
        if (file.exists() && file.isFile()) {
            JSON.parseArray(Tools.readFileToString(file), CreditCard.class).forEach(creditCard -> creditCards.put(creditCard.getAccountEmail().toLowerCase(), creditCard));
        }

        return creditCards;

    }
}
