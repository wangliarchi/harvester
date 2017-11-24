package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.model.Remark;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:37 PM
 */
public class OrderCountryUtils {

    public static String getShipToCountry(Order order) {
        if (Remark.purchaseBack(order.remark)) {
            return Country.US.name();
        } else if (Remark.ukFwd(order.remark)) {
            return Country.UK.name();
        } else {
            // 产品目前默认都是US买回转运，Remark 没有标记
            try {
                if (order.type() == OrderEnums.OrderItemType.PRODUCT) {
                    return Country.US.name();
                }
            } catch (Exception e) {
                //ignore
            }

            return ApplicationContext.getBean(CountryStateUtils.class).getCountryCode(order.ship_country);
        }
    }

    public static Country getFulfillementCountry(Order order) {
        // 批注中直寄和买回同时存在的情况下，先考虑直寄，随后考虑买回, 如果非直寄 和 转运，默认和order order 的sales channel 相同
        if (order.isDirectShip()) {
            return Remark.getDirectShipFromCountry(order.remark);
        } else if (Remark.purchaseBack(order.remark)) {
            return Country.US;
        } else if (Remark.ukFwd(order.remark)) {
            return Country.UK;
        } else {
            // 产品目前默认都是US买回转运，Remark 没有标记
            try {
                if (order.type() == OrderEnums.OrderItemType.PRODUCT) {
                    return Country.US;
                }
            } catch (Exception e) {
                //ignore
            }
            return Country.fromSalesChanel(order.getSales_chanel());
        }
    }


    public static final String OFFER_LIST_URL_PATTERN = "/gp/offer-listing/%s/ref=olp_tab_%s?ie=UTF8&condition=%s&startIndex=%s&sr=8-1";
    public static final String OFFER_LIST_URL_ALL = "/gp/offer-listing/%s/ref=olp_f_used?ie=UTF8&f_new=true&f_used=true&f_usedAcceptable=true&f_usedGood=true&f_usedLikeNew=true&f_usedVeryGood=true"
            + "&startIndex=%s";
    public static final String IMAGE_PRIME_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=ref=olp_fsf?ie=UTF8&condition=${CONDITION}&freeShipping=1";
    public static final String IMAGE_PT_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_tab_${CONDITION}?ie=UTF8&condition=${CONDITION}&mv_style_name=1";
    public static final String PRIME_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_prime_${CONDITION}?ie=UTF8&condition=${CONDITION}&shipPromoFilter=1";
    public static final String PT_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_tab_${CONDITION}?ie=UTF8&condition=${CONDITION}";
    public static final int MIN_SELLERID_LENGTH = 10;

    public static String getOfferListingUrl(Order order) {
        String condition = ConditionUtils.getMasterCondtion(order.condition);
        order.isbn = Strings.fillMissingZero(order.isbn);
        String urlTemplate = order.sellerIsPrime() ? PRIME_URL_PATTERN : PT_URL_PATTERN;


        String result = urlTemplate.replace("${ISBN}", order.isbn).replace("${CONDITION}", condition);
        if (StringUtils.isNotBlank(order.seller_id) && order.seller_id.length() >= MIN_SELLERID_LENGTH) {
            result += "&seller=" + order.seller_id;
        }

        return getFulfillementCountry(order).baseUrl() + "/" + result;

    }

}
