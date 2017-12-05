package edu.olivet.harvester.fulfill.utils;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.fulfill.exception.OrderSubmissionException;
import edu.olivet.harvester.model.CreditCard;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.Harvester;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/5/17 10:16 AM
 */
public class CreditCardUtils {
    public static final String CC_CONFIG_FILE_PATH = Directory.Customize.path() + "/creditcard-config.json";
    public static final String TEST_CC_CONFIG_FILE_PATH = "src/test/resources/conf/creditcard-config.json";


    public static CreditCard getCreditCard(Account buyer) {
        Map<String, CreditCard> creditCards = loadCreditCards();
        if (creditCards.containsKey(buyer.getEmail().toLowerCase())) {
            return creditCards.get(buyer.getEmail().toLowerCase());
        }
        throw new OrderSubmissionException("No credit card configed for buyer account " + buyer.getEmail());
    }

    public static CreditCard getCreditCard(Order order) {
        Account buyer = OrderBuyerUtils.getBuyer(order);
        return getCreditCard(buyer);
    }

    public static Map<String, CreditCard> loadCreditCards() {

        Map<String, CreditCard> creditCards = new HashMap<>();
        File file = new File(getConfigFilePath());


        if (file.exists() && file.isFile()) {
            JSON.parseArray(Tools.readFileToString(file), CreditCard.class).forEach(creditCard -> creditCards.put(creditCard.getAccountEmail().toLowerCase(), creditCard));
        }

        return creditCards;

    }

    public static String getConfigFilePath() {
        if (Harvester.debugFlag) {
            return TEST_CC_CONFIG_FILE_PATH;
        } else {
            return CC_CONFIG_FILE_PATH;
        }
    }

    public static void saveToFile(List<CreditCard> creditCards) {
        File file = new File(getConfigFilePath());
        Tools.writeStringToFile(file, JSON.toJSONString(creditCards, true));
    }
}
