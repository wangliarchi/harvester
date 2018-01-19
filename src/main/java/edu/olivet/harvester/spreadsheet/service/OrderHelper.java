package edu.olivet.harvester.spreadsheet.service;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Configs.KeyCase;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderColumn;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.common.model.State;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        setColumnValue(orderColumn, value, order);
    }

    public void setColumnValue(String columnName, String value, Order order) {
        OrderColumn orderColumn = OrderColumn.valueOf(columnName.replaceAll("-", "_").toUpperCase());
        setColumnValue(orderColumn, value, order);
    }

    public void setColumnValue(OrderColumn orderColumn, String value, Order order) {
        if (orderColumn != null) {
            String fieldName = orderColumn.name().toLowerCase();
            Field filed = ORDER_FIELDS_CACHE.computeIfAbsent(fieldName, s -> {
                try {
                    return Order.class.getDeclaredField(s);
                } catch (NoSuchFieldException e) {
                    return null;
                    //throw new BusinessException(e);
                }
            });
            if (filed == null) {
                return;
            }
            try {
                filed.set(order, StringUtils.defaultString(value).trim());
            } catch (IllegalAccessException e) {
                throw new BusinessException(e);
            }
        }
    }

    /**
     * 当订单的预期购买数量与实际购买数量不一致时，添加批注提示
     *
     * @param order 当前订单
     * @param actualQuantity 实际可以购买数量
     */
    public static void addQuantityChangeRemark(Order order, String actualQuantity) {
        if (order.quantity_purchased.equals(actualQuantity)) {
            return;
        }

        int count = Integer.parseInt(actualQuantity);
        int qtyLeft = Integer.parseInt(order.quantity_purchased) - count;

        order.quantity_fulfilled = String.valueOf(count);
        order.remark = Remark.appendPurchasedQuantityNotEnough(order.remark, count, qtyLeft);
    }


}