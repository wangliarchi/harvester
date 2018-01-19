package edu.olivet.harvester.fulfill.utils;

import com.alibaba.fastjson.JSON;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/4/17 4:39 PM
 */
public class FwdAddressUtils {
    private static final HashMap<String, Integer> FWD_LAST_INDEX = new HashMap<>();

    public static String getFwdRecipient(Order order) {
        Country fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(order);
        OrderItemType orderItemType = order.getType();
        switch (fulfillmentCountry) {
            case US:
                if (orderItemType == OrderItemType.BOOK) {
                    return usFwdBookRecipient(order);
                }

                return usFwdProductRecipient(order);
            default:
                return order.url;
        }


    }

    public static int getLastFWDIndex(String spreadsheetId, String sheetName, List<Order> orders) {
        String key = spreadsheetId + sheetName;

        return FWD_LAST_INDEX.computeIfAbsent(key, k -> {
            final int[] lastIndex = {0};
            orders.forEach(order -> {
                if (StringUtils.length(order.url) > 3 && order.url.contains(order.sheetName)) {
                    try {
                        int i = IntegerUtils.parseInt(order.url.substring(order.url.length() - 3), 0);
                        if (i > lastIndex[0]) {
                            lastIndex[0] = i;
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            });
            return lastIndex[0];
        });
    }

    public static int getLastFWDIndex(Order order) {
        String key = order.spreadsheetId + order.sheetName;
        int lastIndex = FWD_LAST_INDEX.getOrDefault(key, 0);

        FWD_LAST_INDEX.put(key, lastIndex + 1);
        return lastIndex;
    }

    public static String usFwdBookRecipient(Order order) {
        return String.format("zhuanyun/%s/%s", order.getContext().substring(0, order.getContext().length() - 2),
                order.order_id.substring(order.order_id.lastIndexOf('-') + 1));
    }

    public static String usFwdProductRecipient(Order order) {
        //Ammy/12/04/701CA003
        String url;
        if (StringUtils.isNotBlank(order.url) && order.url.contains(order.sheetName) && order.url.contains(order.getContext())) {
            url = order.url;
        } else {
            url = generateUrl(order);
        }

        order.url = url;
        appendCountryCodeToUrl(order);

        return order.url;

    }

    public static void appendCountryCodeToUrl(Order order) {
        String countryCode = CountryStateUtils.getInstance().getCountryCode(order.ship_country);
        if (StringUtils.isBlank(countryCode)) {
            return;
        }

        if ("GB".equals(countryCode)) {
            countryCode = "UK";
        }

        String urlWithCountryCode = String.format("%s/%s", countryCode, order.sheetName);
        if (Strings.containsAnyIgnoreCase(order.url, urlWithCountryCode)) {
            return;
        }

        order.url = order.url.replace("/" + order.sheetName, urlWithCountryCode);
    }

    public static String generateUrl(Order order) {
        int lastIndex = getLastFWDIndex(order);
        String prefix = String.format("%s/%s/%s", order.getTask().getFinderCode(), order.sheetName, order.getContext());

        return prefix + String.format("%03d", lastIndex + 1);
    }

    public static Address getUSFwdAddress() {
        return JSON.parseObject(Configs.read(Config.USForwardAddress.fileName()), Address.class);
    }

    public static Address getUKFwdAddress() {
        return JSON.parseObject(Configs.read(Config.UKForwardAddress.fileName()), Address.class);
    }

}
