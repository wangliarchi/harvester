package edu.olivet.harvester.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Objects;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.Keyable;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.model.OrderEnums.OrderColor;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.toIntExact;

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

    @JSONField(serialize = false)
    public String spreadsheetId;

    @JSONField(serialize = false)
    public String sheetName;

    /**
     * 做单时的上下文：账号-国家-sheet名称
     */
    @JSONField(serialize = false)
    private String context;
    /**
     * 做单时的上下文：sheet对应url地址
     */
    @JSONField(serialize = false)
    private String contextUrl;

    /**
     * 做单是 实际下单数量，如果卖家没有足够库存，这个数字和quantity_ordered会不一样
     */
    @JSONField(serialize = false)
    public String quantity_fulfilled;

    /**
     * 实际做单是的邮寄地址，从order confirmation 页面获取, 用来检查，纪录log
     */
    @JSONField(serialize = false)
    private Address fulfilledAddress;

    /**
     * 实际做单是的买单ASIN／ISBN，从order confirmation 页面获取, 用来检查，纪录log
     */
    @JSONField(serialize = false)
    private String fulfilledASIN;

    /**
     * Determine whether a order number is valid or not by detecting whether it matches Amazon, BetterWorld or Ingram Order Number Pattern
     */
    private boolean orderNumberValid() {
        String orderNo = StringUtils.defaultString(this.order_number).trim();
        return Regex.AMAZON_ORDER_NUMBER.isMatched(orderNo) ||
                Regex.EBAY_ORDER_NUMBER.isMatched(orderNo) ||
                Regex.BW_ORDER_NUMBER.isMatched(orderNo);
    }

    public boolean addressChanged() {
        return Remark.ADDRESS_CHANGED.isContainedBy(this.remark);
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
     * 判定当前订单是否为切换国家直寄(<b>不同于买回转运</b>), DE Shipment, etc..
     */
    public boolean isDirectShip() {
        return Remark.isDirectShip(this.remark);
    }

    /**
     * 判定当前订单是否为切换国家直寄(<b>不同于买回转运</b>)
     */
    public boolean switchCountry() {
        return isDirectShip();
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


    public boolean isUKForward() {
        return Remark.ukFwd(this.remark);
    }

    /**
     * <pre>
     * 判定当前订单是否需要买回转运，根据其状态和Remark，用于<strong>做单</strong>场景
     * <strong>如果批注中包含了直寄，则先不视为买回转运</strong>
     * </pre>
     */
    public boolean needBuyAndTransfer() {
        return this.statusIndicatePurchaseBack() || Remark.purchaseBack(this.remark) || Remark.ukFwd(this.remark);
    }


    public boolean statusIndicatePurchaseBack() {
        return OrderEnums.Status.BuyAndTransfer.value().equalsIgnoreCase(status) ||
                OrderEnums.Status.PrimeBuyAndTransfer.value().equalsIgnoreCase(status);
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

    public static final String[] PURCHASE_DATE_PATTERNS = {"yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "M/d/yyyy H:mm",
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


    @Getter
    @Setter
    private String amazonOrderStatus;

    public Date latestEdd() {
        String estimatedDeliveryDateString = StringUtils.split(estimated_delivery_date, " ")[1];

        return Dates.parseDate(estimatedDeliveryDateString);
    }

    public int maxEddDays() {
        String expectedShipDateString = StringUtils.split(expected_ship_date, " ")[1];
        String estimatedDeliveryDateString = StringUtils.split(estimated_delivery_date, " ")[1];

        Date expectedShipDate, estimatedDeliveryDate;
        try {
            expectedShipDate = FastDateFormat.getInstance("yyyy-M-d").parse(expectedShipDateString);
        } catch (ParseException e) {
            throw new BusinessException(e);
        }

        try {
            estimatedDeliveryDate = FastDateFormat.getInstance("yyyy-M-d").parse(estimatedDeliveryDateString);
        } catch (ParseException e) {
            throw new BusinessException(e);
        }

        long diff = estimatedDeliveryDate.getTime() - expectedShipDate.getTime();

        return toIntExact(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
    }


    public int eddDays() {
        String estimatedDeliveryDateString = StringUtils.split(estimated_delivery_date, " ")[1];

        Date today = new Date();

        Date estimatedDeliveryDate;

        try {
            estimatedDeliveryDate = FastDateFormat.getInstance("yyyy-M-d").parse(estimatedDeliveryDateString);
        } catch (ParseException e) {
            throw new BusinessException(e);
        }

        long diff = estimatedDeliveryDate.getTime() - today.getTime();

        return toIntExact(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
    }

    public boolean selfBuy() {
        return StringUtils.isBlank(quantity_purchased) ||
                "0".equals(quantity_purchased) ||
                Remark.SELF_ORDER.isContainedBy(remark);
    }

    /**
     * 判断当前产品是否为AddOn
     */
    @JSONField(serialize = false)
    public boolean isAddOn() {
        return Remark.ADD_ON.isContainedBy(this.remark);
    }

    /**
     * 判定当前订单是否为待检查状态
     */
    public boolean toBeChecked() {
        return Remark.TO_BE_CHECKED.isContainedBy(this.remark);
    }


    /**
     * 判断当前订单对应Seller是否为AP Warehouse（亚马逊）
     */
    public boolean sellerIsAPWarehouse() {
        return OrderEnums.SellerType.APWareHouse.abbrev().equalsIgnoreCase(this.character);
    }

    /**
     * 判断当前订单对应Seller是否为普通Seller
     */
    public boolean sellerIsPt() {
        return OrderEnums.SellerType.Pt.abbrev().equalsIgnoreCase(this.character) ||
                OrderEnums.SellerType.ImagePt.abbrev().equalsIgnoreCase(this.character);
    }

    /**
     * 获取订单的原始标价和当前实际Seller的标价差值
     */
    @JSONField(serialize = false)
    public float getPriceDiff() {
        return Float.parseFloat(this.price) - Float.parseFloat(this.seller_price);
    }

    /**
     * 判断当前订单对应Seller是否为Prime
     */
    public boolean sellerIsPrime() {
        return OrderEnums.SellerType.Prime.abbrev().equalsIgnoreCase(this.character) ||
                OrderEnums.SellerType.ImagePrime.abbrev().equalsIgnoreCase(this.character) ||
                sellerIsAP() ||
                sellerIsAPWarehouse();
    }

    public boolean sellerIsAP() {
        return OrderEnums.SellerType.AP.name().equalsIgnoreCase(this.seller) || OrderEnums.SellerType.AP.abbrev().equalsIgnoreCase(this.character);
    }

    OrderEnums.OrderItemType type;

    public OrderEnums.OrderItemType type() {
        if (type == null) {
            type = Settings.load().getSpreadsheetType(spreadsheetId);
        }

        return type;
    }


    /**
     * 一条订单提交成功时的相关信息: 原单基本信息、新单提交结果、提交时间
     */
    public String successRecord() {
        return Dates.now() + Constants.TAB +
                StringUtils.defaultString(this.sheetName) + Constants.TAB +
                this.row + Constants.TAB +
                StringUtils.defaultString(this.status) + Constants.TAB +
                StringUtils.defaultString(this.order_id) + Constants.TAB +
                StringUtils.defaultString(this.isbn) + Constants.TAB +
                StringUtils.defaultString(this.seller) + Constants.TAB +
                StringUtils.defaultString(this.seller_id) + Constants.TAB +
                StringUtils.defaultString(this.cost) + Constants.TAB +
                StringUtils.defaultString(this.order_number) + Constants.TAB +
                StringUtils.defaultString(this.account) + Constants.TAB +
                StringUtils.defaultString(this.last_code) + Constants.TAB +
                StringUtils.defaultString(this.profit) + Constants.TAB +
                StringUtils.defaultString(this.remark.replace(StringUtils.LF, StringUtils.SPACE)) + Constants.TAB +
                StringUtils.defaultString(this.spreadsheetId) + Constants.TAB +
                StringUtils.defaultString(Address.loadFromOrder(this).toString()) + Constants.TAB +
                StringUtils.defaultString(this.fulfilledAddress.toString()) + Constants.TAB +
                StringUtils.defaultString(this.fulfilledASIN) + Constants.TAB;
    }


    @Override
    public String getKey() {
        return this.order_id;
    }

    public boolean equalsLite(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equal(order_id, order.order_id) &&
                Objects.equal(sku, order.sku) &&
                Objects.equal(quantity_purchased, order.quantity_purchased) &&
                Objects.equal(isbn, order.isbn) &&
                Objects.equal(seller, order.seller) &&
                Objects.equal(seller_id, order.seller_id) &&
                Objects.equal(condition, order.condition) &&
                Objects.equal(character, order.character);
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


    public static void main(String[] args) {
        //
        try {
            Date date = DateUtils.parseDate("2016-10-12T22:53:00.000Z", Locale.US, Order.PURCHASE_DATE_PATTERNS);

            Date minDate = DateUtils.addDays(new Date(), -30);

            System.out.println(date.before(minDate));

        } catch (Exception e) {
            //ignore
        }
    }


}