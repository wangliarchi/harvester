package edu.olivet.harvester.utils.order;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 8:13 PM
 */
public class PageUtils {
    public static final String HREF = "href";

    /**
     * 亚马逊页面分页(比如Offering列表、订单历史列表等等)数：{@value}
     */
    public static final int PAGINATION = 10;


    /**
     * 提取Url地址中的Seller Id
     */
    public static String getSellerUUID(String url) {
        if (url.contains("/shops/")) {
            //eg:http://www.amazon.com/shops/A2RGW401VKH294/ref=olp_merch_name_3
            return url.replaceAll("http://www[.].*/shops/", StringUtils.EMPTY).replaceAll("/ref=.*", StringUtils.EMPTY);
        } else {
            //eg:gp/aag/main/ref=olp_merch_name_1?ie=UTF8&asin=0718015592&isAmazonFulfilled=0&seller=A1MIVE4G63176B
            return getParameters(url).get("seller");
        }
    }

    /**
     * 解析、提取url中参数键值对
     *
     * @param url url地址
     */
    public static Map<String, String> getParameters(String url) {
        Map<String, String> cache = new HashMap<>();
        int index = url.indexOf("?");
        if (index != -1) {
            String params = url.substring(index + 1);
            String[] arr = StringUtils.split(params, '&');
            for (String pv : arr) {
                String[] arr2 = StringUtils.split(pv, "=");
                if (arr2.length == 2) {
                    cache.put(arr2[0], arr2[1]);
                }
            }
        }

        return cache;
    }
}
