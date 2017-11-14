package edu.olivet.harvester.spreadsheet.service;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Configs.KeyCase;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.model.*;
import edu.olivet.harvester.model.OrderEnums.OrderColumn;
import edu.olivet.harvester.ui.Harvester;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class OrderHelper {
    private static final String DUMMY_PHONE_NUMBER = "123456";
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderHelper.class);

    private Map<String, String> countryCodes;
    private Map<String, String> countryNames;

    @Inject
    public void init() {
        countryCodes = Configs.load("country-codes", KeyCase.UpperCase);
        countryNames = Configs.load("country-names");
    }

    public String correctUSState(String shipState) {
        if (!shipState.contains(".")) {
            return shipState;
        }

        String state = shipState.toUpperCase().replace(".", StringUtils.EMPTY).trim();
        for (State st : State.values()) {
            if (st.name().equals(state)) {
                return st.name();
            }
        }

        return shipState;
    }

    public String getCountryCode(String countryName) {
        if (StringUtils.isBlank(countryName) || Country.US.name().equals(countryName)) {
            return Country.US.name();
        }

        String upper = countryName.toUpperCase();
        String countryCode = countryCodes.get(upper);
        if (StringUtils.isBlank(countryCode)) {
            if (!countryCodes.containsValue(upper)) {
                throw new IllegalArgumentException("Unrecognized country name: " + countryName);
            } else {
                countryCode = upper;
            }
        }
        return countryCode;
    }

    public String getCountryName(String countryCode) {
        Preconditions.checkArgument(StringUtils.isNotBlank(countryCode), "Country code cannot be null or blank.");
        String countryName = countryNames.get(countryCode.toUpperCase());
        if (countryName == null) {
            LOGGER.warn("Cannot find matched entry for {} among {} records.", countryCode, countryNames.size());
        }
        return StringUtils.defaultIfBlank(countryName, countryCode);
    }

    /**
     * 字符类field去null，对isbn自动补齐，对condition自动修正
     */
    public void autoCorrect(Order order) {
        order.status = StringUtils.defaultString(order.status).toLowerCase();

        order.order_id = StringUtils.defaultString(order.order_id);
        order.recipient_name = StringUtils.defaultString(order.recipient_name);
        order.purchase_date = StringUtils.defaultString(order.purchase_date);
        order.sku_address = StringUtils.defaultString(order.sku_address);
        order.sku = StringUtils.defaultString(order.sku);
        order.price = StringUtils.defaultString(order.price);
        order.quantity_purchased = StringUtils.defaultString(order.quantity_purchased);
        order.shipping_fee = StringUtils.defaultString(order.shipping_fee);
        order.ship_state = StringUtils.defaultString(order.ship_state);
        order.isbn_address = StringUtils.defaultString(order.isbn_address);
        order.isbn = StringUtils.defaultString(order.isbn);
        order.seller = StringUtils.defaultString(order.seller);
        order.seller_id = StringUtils.defaultString(order.seller_id);
        order.seller_price = StringUtils.defaultString(order.seller_price);
        order.url = StringUtils.defaultString(order.url);
        order.condition = StringUtils.defaultString(order.condition);
        order.character = StringUtils.defaultString(order.character);
        order.remark = StringUtils.defaultString(order.remark);
        order.reference = StringUtils.defaultString(order.reference);
        order.code = StringUtils.defaultString(order.code);
        order.profit = StringUtils.defaultString(order.profit);
        order.item_name = StringUtils.defaultString(order.item_name);
        order.ship_address_1 = StringUtils.defaultString(order.ship_address_1);
        order.ship_address_2 = StringUtils.defaultString(order.ship_address_2);
        order.ship_city = StringUtils.defaultString(order.ship_city);
        order.ship_zip = StringUtils.defaultString(order.ship_zip);
        order.ship_phone_number = StringUtils.defaultString(order.ship_phone_number);
        order.cost = StringUtils.defaultString(order.cost);
        order.order_number = StringUtils.defaultString(order.order_number);
        order.account = StringUtils.defaultString(order.account);
        order.last_code = StringUtils.defaultString(order.last_code);

        order.ship_country = StringUtils.defaultIfBlank(order.ship_country, Country.US.name());
        order.isbn = Strings.fillMissingZero(order.isbn);
        order.ship_phone_number = StringUtils.defaultIfBlank(order.ship_phone_number, DUMMY_PHONE_NUMBER);
        if (order.ship_phone_number.length() <= 3) {
            order.ship_phone_number = DUMMY_PHONE_NUMBER;
        }
        order.ship_phone_number = order.ship_phone_number.replace("=", StringUtils.EMPTY);
        order.shippingCountryCode = this.getCountryCode(order.ship_country);
        if (Country.US.name().equalsIgnoreCase(order.shippingCountryCode)) {
            order.ship_state = correctUSState(order.ship_state);
        }
        order.sales_chanel = StringUtils.defaultString(order.sales_chanel);
    }

    private static final Map<String, Field> ORDER_FIELDS_CACHE = new HashMap<>();

    public void setColumnValue(int col, String value, Order order) {
        OrderColumn orderColumn = OrderColumn.get(col);
        if (orderColumn != null) {
            String fieldName = orderColumn.name().toLowerCase();
            Field filed = ORDER_FIELDS_CACHE.computeIfAbsent(fieldName, s -> {
                try {
                    return Order.class.getDeclaredField(s);
                } catch (NoSuchFieldException e) {
                    throw new BusinessException(e);
                }
            });

            try {
                filed.set(order, StringUtils.defaultString(value).trim());
            } catch (IllegalAccessException e) {
                throw new BusinessException(e);
            }
        }
    }

    public static Country getFulfillementCountry(Order order) {
        //todo UK & US FWD & Shipment
        Country salesChannelCountry = Country.fromSalesChanel(order.getSales_chanel());

        boolean isDirect = Remark.isDirectShip(order.remark);
        // 批注中直寄和买回同时存在的情况下，先考虑直寄，随后考虑买回
        if (isDirect) {
            return Remark.getDirectShipFromCountry(order.remark);
        } else if (Remark.purchaseBack(order.remark)) {
            return Country.US;
        } else if (Remark.ukFwd(order.remark)) {
            return Country.UK;
        } else {
            return salesChannelCountry;
        }
    }

    public static Account getBuyer(Order order) {
        //todo book or product
        Settings.Configuration config = Settings.load().getConfigByCountry(getFulfillementCountry(order));

        if (order.sellerIsPrime() && !order.switchCountry()) {
            return config.getPrimeBuyer();
        }

        return config.getBuyer();
    }

    public static CreditCard getCreditCard(Order order) {
        Account buyer = getBuyer(order);
        Map<String, CreditCard> creditCards = loadCreditCards();
        if (creditCards.containsKey(buyer.getEmail().toLowerCase())) {
            return creditCards.get(buyer.getEmail().toLowerCase());
        }
        throw new BusinessException("No credit card configed for buyer account " + buyer.getEmail());
    }

    public static Map<String, CreditCard> loadCreditCards() {
        File file = new File(Harvester.CC_CONFIG_FILE_PATH);
        Map<String, CreditCard> creditCards = new HashMap<>();
        if (file.exists() && file.isFile()) {
            JSON.parseArray(Tools.readFileToString(file), CreditCard.class).forEach(creditCard -> creditCards.put(creditCard.getAccountEmail().toLowerCase(), creditCard));
        }

        return creditCards;

    }

    private static final String IMAGE_PRIME_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=ref=olp_fsf?ie=UTF8&condition=${CONDITION}&freeShipping=1";
    private static final String IMAGE_PT_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_tab_${CONDITION}?ie=UTF8&condition=${CONDITION}&mv_style_name=1";
    private static final String PRIME_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_prime_${CONDITION}?ie=UTF8&condition=${CONDITION}&shipPromoFilter=1";
    private static final String PT_URL_PATTERN = "/gp/offer-listing/${ISBN}/ref=olp_tab_${CONDITION}?ie=UTF8&condition=${CONDITION}";
    private static final int MIN_SELLERID_LENGTH = 10;

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


    /**
     * 当订单的预期购买数量与实际购买数量不一致时，添加批注提示
     *
     * @param order       当前订单
     * @param actualQuant 实际可以购买数量
     */
    public static void addQuantChangeRemark(Order order, String actualQuant) {
        if (order.quantity_purchased.equals(actualQuant)) {
            return;
        }

        int count = Integer.parseInt(actualQuant);
        int quantLeft = Integer.parseInt(order.quantity_purchased) - count;

        order.quantity_purchased = String.valueOf(count);
        order.remark = Remark.appendPurchasedQuantityNotEnough(order.remark, count, quantLeft);
    }


    @Inject
    CountryStateUtils countryStateUtils;

    /**
     * 根据当前订单数据决定需要标识的状态，注意：分支判断的顺序<strong>不能颠倒!</strong>
     */
    public String determineStatus(Order order) {
        if (OrderEnums.Status.Finish.value().equalsIgnoreCase(order.status) || OrderEnums.Status.Skip.value().equalsIgnoreCase(order.status) || order.toBeChecked()) {
            return null;
        }

        Country finalAmazonCountry = null;
        try {
            finalAmazonCountry = OrderHelper.getFulfillementCountry(order);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // 客户改变地址、灰条等情况目前略过
        if (order.needBuyAndTransfer()) {
            if (order.sellerIsPrime()) {
                return OrderEnums.Status.PrimeBuyAndTransfer.value();
            } else {
                return OrderEnums.Status.BuyAndTransfer.value();
            }
        } else if (order.seller.toLowerCase().startsWith("bw-") || order.seller.toLowerCase().equals("bw")) {
            return OrderEnums.Status.SellerIsBetterWorld.value();
        } else if (order.seller.toLowerCase().startsWith("half-") || order.seller.toLowerCase().equals("half")) {
            return OrderEnums.Status.SellerIsHalf.value();
        } else if (order.seller.toLowerCase().startsWith("in-") || order.seller.toLowerCase().equals("in")) {
            return OrderEnums.Status.SellerIsIngram.value();
        } else if (!finalAmazonCountry.code().equals(countryStateUtils.getCountryCode(order.ship_country))) {
            return OrderEnums.Status.International.value();
        } else if (order.sellerIsPrime()) {
            return OrderEnums.Status.PrimeSeller.value();
        } else if (order.sellerIsPt()) {
            return OrderEnums.Status.CommonSeller.value();
        } else {
            return null;
        }
    }
}