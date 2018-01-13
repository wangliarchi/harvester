package edu.olivet.harvester.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Objects;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.Keyable;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.ShippingEnums;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
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

    public String spreadsheetId;

    public String sheetName;

    /**
     * 做单时的上下文：账号-国家-sheet名称
     */
    private String context;
    /**
     * 做单时的上下文：sheet对应url地址
     */
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
     * 实际做单Shipping Cost
     */
    @JSONField(serialize = false)
    public Money shippingCost;


    @JSONField(serialize = false)
    public ShippingEnums.ShippingSpeed shippingSpeed;

    /**
     * 做單過程中remark 可能變化
     */
    @JSONField(serialize = false)
    public String originalRemark;

    /**
     * Determine whether a order number is valid or not by detecting whether it matches Amazon, BetterWorld or Ingram Order Number Pattern
     */
    @JSONField(serialize = false)
    public boolean orderNumberValid() {
        String orderNo = StringUtils.defaultString(this.order_number).trim();

        return StringUtils.isNotBlank(RegexUtils.getMatched(orderNo, Regex.AMAZON_ORDER_NUMBER)) ||
                Regex.EBAY_ORDER_NUMBER.isMatched(orderNo) ||
                Regex.BW_ORDER_NUMBER.isMatched(orderNo);
    }

    @JSONField(serialize = false)
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
    @JSONField(serialize = false)
    public boolean fulfilled() {
        return !this.colorIsGray() && !this.canceledBySeller() &&
                (this.orderNumberValid() ||
                        StringUtils.containsIgnoreCase(this.status, "fi") ||
                        Strings.containsAnyIgnoreCase(this.remark, FORWARDED) ||
                        Strings.containsAnyIgnoreCase(this.status, FORWARDED) ||
                        Strings.containsAnyIgnoreCase(this.cost, FORWARDED) ||
                        Strings.containsAnyIgnoreCase(this.order_number, FORWARDED) ||
                        Strings.containsAnyIgnoreCase(this.account, FORWARDED));
    }

    private static final String[] FORWARDED = {"yizhuan", "已转", "已移表"};

    /**
     * Determine whether current order has been canceled by seller
     */
    @JSONField(serialize = false)
    public boolean canceledBySeller() {
        return Remark.SELLER_CANCELED.isContainedByAny(cost, order_number);
    }

    /**
     * 部分情况下客人取消的订单会在新单号一列标注Buyer Cancel等字样，据此判定该条订单是否已被客人取消
     */
    @JSONField(serialize = false)
    public boolean buyerCanceled() {
        return Remark.CANCELLED.isContainedBy(this.order_number);
    }

    /**
     * Determine whether current order is purchased back and transfer via keyword match in column 'remark'
     * Currently used in Customer Service Email Generation <strong>ONLY!</strong>
     */
    @JSONField(serialize = false)
    public boolean purchaseBack() {

        if (Remark.purchaseBack(this.remark)) {
            return true;
        }

        // 产品目前默认都是US买回转运，Remark 没有标记。产品单如果不是直寄或UK FWD，都是US FWD
        return (type() == OrderEnums.OrderItemType.PRODUCT && !isDirectShip() && !isUKForward());

    }

    /**
     * 判定当前订单是否为切换国家直寄(<b>不同于买回转运</b>), DE Shipment, etc..
     */
    @JSONField(serialize = false)
    public boolean isDirectShip() {
        return Remark.isDirectShip(this.remark);
    }

    /**
     * 判定当前订单是否为切换国家直寄(<b>不同于买回转运</b>)
     */
    @JSONField(serialize = false)
    public boolean switchCountry() {
        return isDirectShip();
    }

    @JSONField(serialize = false)
    public boolean isIntl() {
        String fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(this).name();
        String marketplaceCountry = CountryStateUtils.getInstance().getCountryCode(OrderCountryUtils.getShipToCountry(this));
        return !fulfillmentCountry.equalsIgnoreCase(marketplaceCountry);
    }

    /**
     * Determine whether current order is fulfilled from US directly via its remark
     */
    @JSONField(serialize = false)
    public boolean fulfilledFromUS() {
        return Remark.FULFILL_FROM_US.isContainedBy(this.remark);
    }

    /**
     * Determine whether current order is fulfilled from UK directly via its remark
     */
    @JSONField(serialize = false)
    public boolean fulfilledFromUK() {
        return Remark.FULFILL_FROM_UK.isContainedBy(this.remark);
    }

    @JSONField(serialize = false)
    public boolean isUKForward() {
        return Remark.ukFwd(this.remark);
    }

    /**
     * <pre>
     * 判定当前订单是否需要买回转运，根据其状态和Remark，用于<strong>做单</strong>场景
     * <strong>产品单默认为 买回转运</strong>
     * <strong>如果批注中包含了直寄，则先不视为买回转运</strong>
     * </pre>
     */
    @JSONField(serialize = false)
    public boolean needBuyAndTransfer() {
        return this.statusIndicatePurchaseBack() ||
                Remark.purchaseBack(this.remark) ||
                Remark.ukFwd(this.remark) ||
                (type() == OrderEnums.OrderItemType.PRODUCT && !isDirectShip());
    }

    @JSONField(serialize = false)
    public boolean statusIndicatePurchaseBack() {
        return OrderEnums.Status.BuyAndTransfer.value().equalsIgnoreCase(status) ||
                OrderEnums.Status.PrimeBuyAndTransfer.value().equalsIgnoreCase(status);
    }

    @JSONField(serialize = false)
    public boolean ebay() {
        return Strings.containsAnyIgnoreCase(seller, "www.ebay.com") ||
                Strings.containsAnyIgnoreCase(isbn, "www.ebay.com") ||
                Strings.containsAnyIgnoreCase(remark, "ebay");
    }

    @JSONField(serialize = false)
    public boolean japan() {
        return Strings.startsWithAnyIgnoreCase(seller, Country.JP.name());
    }


    @JSONField(serialize = false)
    public String shippingCountryCode;

    @JSONField(serialize = false)
    public Date getPurchaseDate() {
        return parsePurchaseDate(purchase_date);
    }

    public static final String[] PURCHASE_DATE_PATTERNS = {"yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "M/d/yyyy H:mm",
            "M/d/yyyy H:mm:ss", "yyyy-MM-dd HH:mm:ss", "MM-dd-yyyy HH:mm", "yyyy-MM-dd"};

    @JSONField(serialize = false)
    public static Date parsePurchaseDate(String purchaseDateText) {
        return Dates.parseDate(purchaseDateText);
    }


    @Getter
    @Setter
    @JSONField(serialize = false)
    private String amazonOrderStatus;

    @JSONField(serialize = false)
    public Date latestEdd() {
        String estimatedDeliveryDateString;
        if (Strings.containsAnyIgnoreCase(estimated_delivery_date, " - ")) {
            estimatedDeliveryDateString = estimated_delivery_date.split("\\s-\\s")[1];
        } else {
            estimatedDeliveryDateString = StringUtils.split(estimated_delivery_date, " ")[1];
        }


        return Dates.parseDate(estimatedDeliveryDateString);
    }

    @JSONField(serialize = false)
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

    @JSONField(serialize = false)
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

    @JSONField(serialize = false)
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
    @JSONField(serialize = false)
    public boolean toBeChecked() {
        return Remark.TO_BE_CHECKED.isContainedBy(this.remark);
    }


    /**
     * 判断当前订单对应Seller是否为AP Warehouse（亚马逊）
     */
    @JSONField(serialize = false)
    public boolean sellerIsAPWarehouse() {
        return OrderEnums.SellerType.APWareHouse.abbrev().equalsIgnoreCase(this.character);
    }

    /**
     * 判断当前订单对应Seller是否为普通Seller
     */
    @JSONField(serialize = false)
    public boolean sellerIsPt() {
        return OrderEnums.SellerType.Pt.abbrev().equalsIgnoreCase(this.character) ||
                OrderEnums.SellerType.ImagePt.abbrev().equalsIgnoreCase(this.character);
    }

    /**
     * 获取订单的原始标价和当前实际Seller的标价差值
     */
    @JSONField(serialize = false)
    public float getPriceDiff() {
        //return Float.parseFloat(this.price) - Float.parseFloat(this.seller_price);
        return getOrderPrice().toUSDAmount().floatValue() - getSellerPrice().toUSDAmount().floatValue();
    }

    /**
     * 判断当前订单对应Seller是否为Prime
     */
    @JSONField(serialize = false)
    public boolean sellerIsPrime() {
        return OrderEnums.SellerType.Prime.abbrev().equalsIgnoreCase(this.character) ||
                OrderEnums.SellerType.ImagePrime.abbrev().equalsIgnoreCase(this.character) ||
                sellerIsAP() ||
                sellerIsAPWarehouse();
    }

    @JSONField(serialize = false)
    public boolean sellerIsAP() {
        return OrderEnums.SellerType.AP.name().equalsIgnoreCase(this.seller) || OrderEnums.SellerType.AP.abbrev().equalsIgnoreCase(this.character);
    }

    @JSONField(serialize = false)
    public OrderEnums.OrderItemType type = null;

    @JSONField(serialize = false)
    public OrderEnums.OrderItemType getType() {
        return type();
    }

    @JSONField(serialize = false)
    public OrderEnums.OrderItemType type() {
        if (type == null) {
            try {
                type = Settings.load().getSpreadsheetType(spreadsheetId);
            } catch (Exception e) {
                //
            }
        }

        if (type == null) {
            try {
                type = RuntimeSettings.load().getCurrentType();
            } catch (Exception e) {
                //
            }
        }

        return type;
    }


    /**
     * 一条订单提交成功时的相关信息: 原单基本信息、新单提交结果、提交时间
     */
    @JSONField(serialize = false)
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

    @JSONField(serialize = false)
    public String basicSuccessRecord() {
        return StringUtils.defaultString(this.cost) + Constants.TAB +
                StringUtils.defaultString(this.order_number) + Constants.TAB +
                StringUtils.defaultString(this.account) + Constants.TAB +
                StringUtils.defaultString(this.last_code);
    }

    @JSONField(serialize = false)
    private Money sellerPrice;

    @JSONField(serialize = false)
    public Money getSellerPrice() {
        if (sellerPrice == null) {
            float price = 0;
            if (StringUtils.isNotBlank(seller_price)) {
                price = FloatUtils.parseFloat(seller_price, 0);
            }

            sellerPrice = new Money(price, OrderCountryUtils.getFulfillmentCountry(this));
        }

        return sellerPrice;

    }

    @JSONField(serialize = false)
    private Money orderPrice;

    @JSONField(serialize = false)
    public Money getOrderPrice() {
        if (orderPrice == null) {
            float priceFloat = 0;
            if (StringUtils.isNotBlank(price)) {
                priceFloat = FloatUtils.parseFloat(price, 0);
            }
            orderPrice = new Money(priceFloat, OrderCountryUtils.getMarketplaceCountry(this));
        }

        return orderPrice;

    }


    @JSONField(serialize = false)
    private Money orderTotalPrice;

    @JSONField(serialize = false)
    public Money getOrderTotalPrice() {
        //if (orderTotalPrice == null) {
        float priceFloat = 0;
        if (StringUtils.isNotBlank(price)) {
            priceFloat = FloatUtils.parseFloat(price, 0);
        }
        if (StringUtils.isNotBlank(shipping_fee)) {
            priceFloat += FloatUtils.parseFloat(shipping_fee, 0);
        }
        orderTotalPrice = new Money(priceFloat, OrderCountryUtils.getMarketplaceCountry(this));
        //}

        return orderTotalPrice;

    }

    @JSONField(serialize = false)
    public Money orderTotalCost;

    @JSONField(serialize = false)
    public boolean expeditedShipping() {
        return Remark.fastShipping(remark);
    }

    @JSONField(serialize = false)
    public String getASIN() {
        return RegexUtils.getMatched(sku_address, RegexUtils.Regex.ASIN);
    }

    /*
    做单用 - 当前task
     */
    @JSONField(serialize = false)
    OrderSubmissionTask task;

    @Override
    public String getKey() {
        return this.order_id;
    }

    @JSONField(serialize = false)
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
                Objects.equal(StringUtils.stripStart(ship_zip, "0"), StringUtils.stripStart(order.ship_zip, "0")) &&
                //phone number is hard to compare as it may be formatted by google spreadsheet...  not crucial
                //Objects.equal(StringUtils.stripStart(, "0"), StringUtils.stripStart(order.ship_phone_number, "0")) &&
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

    @Override
    public String toString() {
        return order_id+"\t"+sheetName+"\t"+row+"\t"+
                status+"\t"+
                recipient_name+"\t"+
                purchase_date+"\t"+
                sku+"\t"+
                sku_address+"\t"+
                price+"\t"+
                quantity_purchased+"\t"+
                shipping_fee+"\t"+
                ship_state+"\t"+
                isbn+"\t"+
                seller+"\t"+
                seller_id+"\t"+
                seller_price+"\t"+
                url+"\t"+
                condition+"\t"+
                character+"\t"+
                remark+"\t"+
                reference+"\t"+
                code+"\t"+
                item_name+"\t"+
                ship_address_1+"\t"+
                ship_address_2+"\t"+
                ship_city+"\t"+
                ship_zip+"\t"+
                ship_phone_number+"\t"+
                ship_country+"\t"+
                cost+"\t"+
                order_number+"\t"+
                account+"\t"+
                last_code+"\t"+
                sales_chanel+"\t"+
                original_condition+"\t"+
                shipping_service+"\t"+
                cost+"\t";
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