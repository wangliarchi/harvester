package edu.olivet.harvester.model;

import com.google.common.base.Objects;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.Keyable;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.OrderEnums.OrderColor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Order information mapped to one row of daily update spreadsheet
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@SuppressWarnings("CheckStyle")
@Data
public class Order implements Keyable {
    public int row;

    public String status;

    public String order_id;

    public String recipient_name;

    public String purchase_date;

    public String sku_address;

    public String sku;

    public String price;

    public String quantity_purchased;

    public String shipping_fee;

    public String ship_state;

    public String isbn_address;

    public String isbn;

    public String seller;

    public String seller_id;

    public String seller_price;

    public String url;

    public String condition;

    public String character;

    public String remark;

    public String reference;

    public String code;

    public String profit;

    public String item_name;

    public String ship_address_1;

    public String ship_address_2;

    public String ship_city;

    public String ship_zip;

    public String ship_phone_number;

    public String cost;

    /**
     * Supplier order number after fulfillment. However, this can be other remark such as 'yizhuan' to indicate
     * current order was transferred to UK forward spreadsheet already.
     */
    public String order_number;

    /**
     * Buyer account used to fulfill current order
     */
    public String account;

    public String last_code;

    public String ship_country;

    public String sid;

    /**
     * Sales chanel of order, which will be useful in multiple marketplaces scenario
     * For example, an order in Europe might be placed at UK, FR, DE, IT or ES
     */
    public String sales_chanel;

    public String original_condition;

    public String shipping_service;

    public String expected_ship_date;

    public String estimated_delivery_date;

    public String color = OrderColor.Finished.code();

    public boolean colorIsGray() {
        return OrderColor.isGray(this.color);
    }

    /**
     * Determine whether a order number is valid or not by detecting whether it matches Amazon, BetterWorld or Ingram Order Number Pattern
     */
    private boolean orderNumberValid() {
        String orderNo = StringUtils.defaultString(this.order_number).trim();
        return Regex.AMAZON_ORDER_NUMBER.isMatched(orderNo) ||
                Regex.EBAY_ORDER_NUMBER.isMatched(orderNo) ||
                Regex.BW_ORDER_NUMBER.isMatched(orderNo);
    }

    /**
     * <pre>
     * 校验一条订单是否已经完成找做单:
     * 1. 订单不能是灰条、Seller Canceled;
     * 2. 如果订单号一列为有效订单号: 亚马逊, BetterWorld, Half等, 满足条件;
     * 3. UK转运如果已经完成转移, 也视为满足条件。
     * </pre>
     */
    public boolean fulfilled() {
        return !this.colorIsGray() && !this.canceledBySeller() &&
                (this.orderNumberValid() ||
                        Strings.containsAnyIgnoreCase(this.cost, FORWARDED) ||
                        Strings.containsAnyIgnoreCase(this.order_number, FORWARDED) ||
                        Strings.containsAnyIgnoreCase(this.account, FORWARDED));
    }

    private static final String[] FORWARDED = {"yizhuan", "已转", "已移表"};

    /**
     * Determine whether current order has been canceled by seller
     */
    public boolean canceledBySeller() {
        return Remark.SELLER_CANCELED.isContainedByAny(cost, order_number);
    }

    /**
     * 部分情况下客人取消的订单会在新单号一列标注Buyer Cancel等字样，据此判定该条订单是否已被客人取消
     */
    public boolean buyerCanceled() {
        return Remark.CANCELLED.isContainedBy(this.order_number);
    }

    /**
     * Determine whether current order is purchased back and transfer via keyword match in column 'remark'
     * Currently used in Customer Service Email Generation <strong>ONLY!</strong>
     */
    public boolean purchaseBack() {
        return Remark.purchaseBack(this.remark);
    }

    /**
     * 判定当前订单是否为切换国家直寄(<b>不同于买回转运</b>)
     */
    public boolean switchCountry() {
        return Remark.needFulfillInOtherCountry(this.remark);
    }

    /**
     * Determine whether current order is fulfilled from US directly via its remark
     */
    public boolean fulfilledFromUS() {
        return Remark.FULFILL_FROM_US.isContainedBy(this.remark);
    }

    /**
     * Determine whether current order is fulfilled from UK directly via its remark
     */
    private boolean fulfilledFromUK() {
        return Remark.FULFILL_FROM_UK.isContainedBy(this.remark);
    }

    public boolean sellerIsBW() {
        return (Strings.containsAnyIgnoreCase(seller, "bw-", "bw") && !Regex.AMAZON_ORDER_NUMBER.isMatched(order_number)) ||
                Regex.BW_ORDER_NUMBER.isMatched(order_number);
    }

    public boolean ebay() {
        return Strings.containsAnyIgnoreCase(seller, "www.ebay.com") ||
                Strings.containsAnyIgnoreCase(isbn, "www.ebay.com") ||
                Strings.containsAnyIgnoreCase(remark, "ebay");
    }

    public boolean japan() {
        return Strings.startsWithAnyIgnoreCase(seller, Country.JP.name());
    }


    public String shippingCountryCode;


    public Date getPurchaseDate() {
        return parsePurchaseDate(purchase_date);
    }
    private static final String[] PURCHASE_DATE_PATTERNS = {"yyyy-MM-dd'T'HH:mm:ssXXX","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "M/d/yyyy H:mm",
            "M/d/yyyy H:mm:ss", "yyyy-MM-dd HH:mm:ss", "MM-dd-yyyy HH:mm", "yyyy-MM-dd"};
    public static Date parsePurchaseDate(String purchaseDateText) {
        try {
            return DateUtils.parseDate(purchaseDateText, Locale.US, PURCHASE_DATE_PATTERNS);
        } catch (ParseException e) {
            throw new BusinessException(
                    String.format("Failed to parse order purchase date in text: %s. Supported date patterns: %s.",
                            purchaseDateText,
                            StringUtils.join(PURCHASE_DATE_PATTERNS, " ")));
        }
    }

    @Override
    public String getKey() {
        return this.order_id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equal(order_id, order.order_id) &&
                Objects.equal(recipient_name, order.recipient_name) &&
                Objects.equal(sku, order.sku) &&
                Objects.equal(quantity_purchased, order.quantity_purchased) &&
                Objects.equal(isbn, order.isbn) &&
                Objects.equal(seller, order.seller) &&
                Objects.equal(seller_id, order.seller_id) &&
                Objects.equal(condition, order.condition) &&
                Objects.equal(character, order.character) &&
                Objects.equal(remark, order.remark) &&
                Objects.equal(item_name, order.item_name) &&
                Objects.equal(ship_address_1, order.ship_address_1) &&
                Objects.equal(ship_address_2, order.ship_address_2) &&
                Objects.equal(ship_city, order.ship_city) &&
                Objects.equal(ship_zip, order.ship_zip) &&
                Objects.equal(ship_phone_number, order.ship_phone_number) &&
                Objects.equal(order_number, order.order_number) &&
                Objects.equal(account, order.account) &&
                Objects.equal(ship_country, order.ship_country);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(order_id, recipient_name, sku, quantity_purchased, isbn, seller, seller_id, condition,
                character, remark, item_name, ship_address_1, ship_address_2, ship_city, ship_zip, ship_phone_number,
                order_number, account, ship_country);
    }
}