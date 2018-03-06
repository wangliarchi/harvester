package edu.olivet.harvester.selforder.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.selforder.model.SelfOrder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/2/2018 6:08 AM
 */
public class SelfOrderHelper {


    public static Order convertToOrder(SelfOrder selfOrder) {
        Order order = new Order();
        order.status = "";
        order.order_id = "111-1111111-1111111";
        order.isbn = selfOrder.asin;
        order.seller = selfOrder.ownerAccountStoreName;
        order.seller_id = selfOrder.ownerAccountSellerId;
        order.character = SellerType.Pt.abbrev();
        order.condition = "";
        order.recipient_name = selfOrder.recipientName;
        order.ship_address_1 = selfOrder.shippingAddress1;
        order.ship_address_2 = selfOrder.shippingAddress2;
        order.ship_city = selfOrder.shippingCity;
        order.ship_state = selfOrder.shippingState;
        order.ship_zip = selfOrder.shippingZipCode;
        order.ship_phone_number = selfOrder.shippingPhoneNumber;
        order.ship_country = selfOrder.shippingCountry;
        order.sales_chanel = Country.fromCode(selfOrder.country).baseUrl().replaceAll("https://www.", "");
        order.promotionCode = selfOrder.promoCode;
        order.seller_free_shipping = true;
        order.findNewSellerIfDisappeared = false;
        order.selfOrder = true;
        order.quantity_purchased = "1";
        order.row = selfOrder.row;
        order.sheetName = selfOrder.sheetName;
        order.spreadsheetId = selfOrder.spreadsheetId;

        order.remark = Country.fromCode(selfOrder.country).name() + " shipment";
        return order;
    }


    public void setColumnValue(int col, String value, SelfOrder selfOrder) {
        Column orderColumn = Column.get(col);
        setColumnValue(orderColumn, value, selfOrder);
    }


    private static final Map<String, Field> RECORD_FIELDS_CACHE = new HashMap<>();

    public void setColumnValue(Column orderColumn, String value, SelfOrder selfOrder) {
        if (orderColumn != null) {
            String fieldName = orderColumn.name();
            Field filed = RECORD_FIELDS_CACHE.computeIfAbsent(fieldName, s -> {
                try {
                    return SelfOrder.class.getDeclaredField(s);
                } catch (NoSuchFieldException e) {
                    return null;
                }
            });
            if (filed == null) {
                return;
            }

            try {
                filed.set(selfOrder, StringUtils.defaultString(value).trim());
            } catch (IllegalAccessException e) {
                throw new BusinessException(e);
            }
        }
    }


    public enum Column {
        ownerAccountCode(1),
        ownerAccountStoreName(2),
        ownerAccountSellerId(3),
        country(4),
        asin(5),
        promoCode(6),
        buyerAccountCode(7),
        buyerAccountEmail(8),
        orderNumber(9),
        cost(10),
        recipientName(11),
        shippingAddress1(12),
        shippingAddress2(13),
        shippingCity(14),
        shippingState(15),
        shippingZipCode(16),
        shippingPhoneNumber(17),
        shippingCountry(18),
        carrier(19),
        trackingNumber(20),
        uniqueCode(21);
        private final int number;

        /**
         * 获取对应的列号
         */
        public int number() {
            return number;
        }

        public int index() {
            return number - 1;
        }

        Column(int number) {
            this.number = number;
        }

        public static @Nullable Column get(int number) {
            for (Column column : Column.values()) {
                if (column.number == number) {
                    return column;
                }
            }
            return null;
        }
    }
}
