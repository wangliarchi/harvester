package edu.olivet.harvester.export.model;

import com.amazonservices.mws.orders._2013_09_01.model.Address;
import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.amazonservices.mws.orders._2013_09_01.model.OrderItem;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.model.OrderEnums.OrderColumn;
import edu.olivet.harvester.model.OrderEnums.Status;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.common.DateFormat;
import edu.olivet.harvester.utils.common.NumberUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/18/17 2:46 PM
 */
@SuppressWarnings("DefaultAnnotationParam")
@Table(value = "amazon_orders_new")
@Data
@EqualsAndHashCode(callSuper = false)

public class AmazonOrder extends PrimaryKey {
    public static final int NOT_EXPORTED = 10;
    static final int EXPORTED = 100;
    @Name
    private String orderItemId;

    @Column
    private String orderId;


    @Column
    private String asin;
    @Column
    private String sku;
    /**
     * 订单状态，参见：http://docs.developer.amazonservices.com/en_US/orders-2013-09-01/Orders_ListOrders.html
     */
    @Column
    private String orderStatus;
    /**
     * 下单日期，可用于后续按照下单日期区间查询
     */
    @Column
    private Date purchaseDate;
    /**
     * 原始{@link Order}数据序列化为XML结果，方便后续反序列化使用，避免数据库表中冗余过多字段
     *
     * @see Order#toXML()
     */
    @Column
    private String xml;
    /**
     * 原始{@link OrderItem}数据序列化为XML结果，方便后续反序列化使用，避免数据库表中冗余过多字段
     *
     * @see OrderItem#toXML()
     */
    @Column
    private String itemXml;
    /**
     * 来源点
     */
    @Column
    private String isbn;

    @Column
    private String name;

    @Column
    private String email;
    /**
     * 导单状态，通常为准备导出、已经导出两种
     */
    @Column
    private int exportStatus;
    /**
     * 数据最后更新时间
     */
    @Column
    private Date lastUpdate;

    static final int COLUMN_COUNTS = OrderColumn.SALES_CHANEL.number() + 4;
    private static final String SEPARATOR = " - ";

    /**
     * <pre>
     * 订单对应Purchase Date：
     * 在MWS Orders API返回结果中时区为GMT
     * 在ASC为对应卖场时区：比如US为PST，欧洲UK及其他卖场存在1小时差异
     * 程序运行所在终端（比如某台电脑）对应时区则不定，可能为PST，EST等等
     * 最终写入订单的时区，以ASC显示时区为准
     * </pre>
     */

    public edu.olivet.harvester.model.Order toOrder() {
        Order amazonOrder = MWSUtils.buildMwsObject(this.xml, Order.class);

        Address address = amazonOrder.getShippingAddress();
        OrderItem item = MWSUtils.buildMwsObject(this.itemXml, OrderItem.class);
        Country salesChanelCountry = Country.fromSalesChanel(amazonOrder.getSalesChannel());

        edu.olivet.harvester.model.Order order = new edu.olivet.harvester.model.Order();
        order.status = Status.Initial.value();
        order.order_id = this.orderId;
        order.recipient_name = address.getName();

        order.sku_address = salesChanelCountry.baseUrl() + "/dp/" + this.asin;
        order.sku = this.sku;
        order.quantity_purchased = String.valueOf(item.getQuantityOrdered());

        //price and shipping fee is unit price
        order.price = NumberUtils.toString(Float.parseFloat(item.getItemPrice().getAmount()) / (float) item.getQuantityOrdered(), 2);

        order.shipping_fee = NumberUtils.toString(Float.parseFloat(item.getShippingPrice().getAmount()) / (float) item.getQuantityOrdered(), 2);

        if (StringUtils.isBlank(this.isbn)) {
            order.isbn_address = this.isbn;
        } else {
            //统一用amazon.com的地址，找单的时候 可以换成实际做单国家的地址
            order.isbn_address = "https://www.amazon.com/dp/" + this.isbn;
        }
        order.isbn = this.isbn;


        order.seller = order.seller_id = order.seller_price = StringUtils.EMPTY;
        order.url = StringUtils.EMPTY;

        // 此处是最终找单结果对应Condition，不同于原始Condition
        // 例：Used物品可能最终会找New的代替，New的物品也可能会寻找Used - Like New甚至Used - Good替换
        order.condition = StringUtils.EMPTY;

        order.character = order.remark = order.reference = order.code = order.profit = StringUtils.EMPTY;
        order.item_name = item.getTitle();
        order.ship_address_1 = StringUtils.defaultString(address.getAddressLine1());
        order.ship_address_2 = StringUtils.defaultString(address.getAddressLine2());
        order.ship_city = StringUtils.defaultString(address.getCity());
        order.ship_state = StringUtils.defaultString(address.getStateOrRegion());
        order.ship_zip = StringUtils.defaultString(address.getPostalCode());
        if (address.getPhone() != null) {
            order.ship_phone_number = StringUtils.defaultString(address.getPhone().replaceAll("\\+", ""));
        } else {
            order.ship_phone_number = StringUtils.EMPTY;
        }
        order.cost = order.order_number = order.account = order.last_code = StringUtils.EMPTY;
        // country或是countryCode，有时前者可能没有
        order.ship_country =
                StringUtils.defaultIfBlank(address.getCounty(), CountryStateUtils.getInstance().getCountryName(address.getCountryCode()));
        order.sid = StringUtils.EMPTY;
        order.sales_chanel = amazonOrder.getSalesChannel();

        String cnd = item.getConditionId();
        order.original_condition = cnd + (!cnd.equalsIgnoreCase(ConditionUtils.Condition.New.name()) ? (SEPARATOR + item.getConditionSubtypeId()) : (SEPARATOR + ConditionUtils.Condition.New.name()));
        order.shipping_service = amazonOrder.getShipmentServiceLevelCategory();

        FastDateFormat ORDER_DATE_FORMAT = FastDateFormat.getInstance(DateFormat.DATE_TIME_STR.pattern(),
                ServiceUtils.getTimeZone(salesChanelCountry));
        FastDateFormat SHIP_DATE_FORMAT = FastDateFormat.getInstance(DateFormat.FULL_DATE.pattern(),
                ServiceUtils.getTimeZone(salesChanelCountry));
        order.expected_ship_date =
                SHIP_DATE_FORMAT.format(amazonOrder.getEarliestShipDate().toGregorianCalendar().getTime()) + " " +
                        SHIP_DATE_FORMAT.format(amazonOrder.getLatestShipDate().toGregorianCalendar().getTime());
        order.estimated_delivery_date =
                SHIP_DATE_FORMAT.format(amazonOrder.getEarliestDeliveryDate().toGregorianCalendar().getTime()) + " " +
                        SHIP_DATE_FORMAT.format(amazonOrder.getLatestDeliveryDate().toGregorianCalendar().getTime());
        order.purchase_date = ORDER_DATE_FORMAT.format(this.purchaseDate);

        return order;
    }

    @Override
    public String getPK() {
        return orderItemId;
    }
}
