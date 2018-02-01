package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/15/17 3:37 PM
 */
public class OrderCountryUtils {

    public static String getShipToCountry(Order order) {
        if (order.purchaseBack()) {
            return Country.US.name();
        } else if (order.isUKForward()) {
            return Country.UK.name();
        } else {
            return CountryStateUtils.getInstance().getCountryCode(order.ship_country);
        }
    }

    public static Country getMarketplaceCountry(Order order) {
        try {
            return Country.fromSalesChanel(order.getSales_chanel());
        } catch (Exception e) {
            //sale channel is missing, current country;
            if (StringUtils.isNotBlank(order.spreadsheetId)) {
                return Settings.load().getSpreadsheetCountry(order.spreadsheetId);
            }
            try {
                return Country.fromCode(order.getRuntimeSettings().getMarketplaceName());
            } catch (Exception e1) {
                //
            }
        }

        throw new BusinessException("Fail to read marketplace country, please check if order data is integrated");
    }

    public static Country getFulfillmentCountry(Order order) {
        // 批注中直寄和买回同时存在的情况下，先考虑直寄，随后考虑买回, 如果非直寄 和 转运，默认和order order 的sales channel 相同
        // for EU books, if it's not direct ship, us fwd, or uk fwd, then it's uk shipment
        if (order.isDirectShip()) {
            return Remark.getDirectShipFromCountry(order.remark);
        } else if (order.purchaseBack()) {
            return Country.US;
        } else if (Remark.ukFwd(order.remark)) {
            return Country.UK;
        } else if (order.getType() == OrderItemType.BOOK && getMarketplaceCountry(order).europe()) {
            return Country.UK;
        } else {
            return getMarketplaceCountry(order);
        }
    }


    public static final String OFFER_LIST_URL_PATTERN = "/gp/offer-listing/%s/ref=olp_tab_%s?ie=UTF8&condition=%s&startIndex=%s&sr=8-1";
    private static final String PRIME_URL_PATTERN =
            "/gp/offer-listing/${ISBN}/ref=olp_prime_${CONDITION}?ie=UTF8&condition=${CONDITION}&shipPromoFilter=1";
    private static final String PT_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_tab_${CONDITION}?ie=UTF8&condition=${CONDITION}";
    private static final int MIN_SELLERID_LENGTH = 10;

    public static String getOfferListingUrl(Order order) {
        String condition = ConditionUtils.getMasterCondition(order.condition);
        order.isbn = Strings.fillMissingZero(order.isbn);
        String urlTemplate = order.sellerIsPrime() ? PRIME_URL_PATTERN : PT_URL_PATTERN;


        String result = urlTemplate.replace("${ISBN}", order.isbn).replace("${CONDITION}", condition);
        if (StringUtils.isNotBlank(order.seller_id) && order.seller_id.length() >= MIN_SELLERID_LENGTH) {
            result += "&seller=" + order.seller_id;
        }

        return getFulfillmentCountry(order).baseUrl() + result;
    }


}
