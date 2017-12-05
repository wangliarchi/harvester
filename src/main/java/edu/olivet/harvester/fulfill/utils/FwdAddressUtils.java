package edu.olivet.harvester.fulfill.utils;

import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/4/17 4:39 PM
 */
public class FwdAddressUtils {
    public static final HashMap<String, Integer> FWD_LAST_INDEX = new HashMap<>();

    public static String getFwdRecipient(Order order) {
        Country fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(order);
        OrderItemType orderItemType = order.getType();
        switch (fulfillmentCountry) {
            case US:
                if (orderItemType == OrderItemType.BOOK) {
                    return usFwdBookRecipient(order);
                }

                return usFwdProductRecipient(order);

        }

        return order.url;
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
        return String.format("zhuanyun/%s/%s", order.getContext().substring(0, order.getContext().length() - 2), order.order_id.substring(order.order_id.lastIndexOf('-') + 1));
    }

    public static String usFwdProductRecipient(Order order) {
        //Ammy/12/04/701CA003
        String prefix = String.format("%s/%s/%s", RuntimeSettings.load().getFinderCode(), order.sheetName, order.getContext());
        if (StringUtils.isNotBlank(order.url) && order.url.contains(order.sheetName)) {
            return order.url;
        }

        int lastIndex = getLastFWDIndex(order);
        String code = prefix + String.format("%03d", lastIndex + 1);

        order.url = code;

        return code;
    }

}
