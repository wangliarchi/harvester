package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/22/17 1:58 PM
 */
public class OrderAddressUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderAddressUtils.class);

    public static Address orderShippingAddress(Order order) {
        if (order.isUKForward()) {
            return null;
        }

        if (order.purchaseBack()) {
            Address address = Address.USFwdAddress();
            address.setName(usFwdBookRecipient(order));
            return address;
        }

        Address address = Address.loadFromOrder(order);
        address.setName(recipientName(order));
        return address;

    }

    public static String usFwdBookRecipient(Order order) {

        if (OrderCountryUtils.getFulfillementCountry(order) == Country.US) {
            return String.format("zhuanyun/%s/%s", order.getContext().substring(0, order.getContext().length() - 2), order.order_id.substring(order.order_id.lastIndexOf('-') + 1));
        } else {
            return order.url;
        }

    }


    public static String recipientName(Order order) {
        String fullName = order.recipient_name;
        // 如果拼接的姓名超过亚马逊允许上限，且当前价格差异大于20，可以不加上No Invoice，但需要补上Remark
        // 存在Seller为Prime但标识了a的情况，此时仍然需要当做Prime的情况处理Full Name
        if (!order.sellerIsPrime()) {
            String s = order.recipient_name + RuntimeSettings.load().getNoInvoiceText();
            int max = maxNameLength(OrderCountryUtils.getFulfillementCountry(order));
            fullName = s.length() > max ? order.recipient_name : s;

            if (s.length() > max && order.getPriceDiff() > 20.0f) {
                order.remark = Remark.EMAIL_SELLER_NO_INVOICE.appendTo(order.remark);
                LOGGER.debug("当前客户提供的姓名{}过长{}(上限{})，无法添加No Invoice标识", fullName, fullName.length(), max);
            }
        }

        return fullName;
    }

    /**
     * 获取指定亚马逊国家在地址输入时，收件人姓名一栏的长度上限
     * 美国上限是50，加拿大是35，默认也认为是50
     *
     * @param country 亚马逊国家
     */
    public static int maxNameLength(Country country) {
        return country == Country.CA ? 35 : 50;
    }


    public static void main(String[] args) {
        Order order = new Order();
        order.order_id = "111-7432922-1916254";
        order.remark = "US FWD";

        OrderAddressUtils.orderShippingAddress(order);
    }
}
