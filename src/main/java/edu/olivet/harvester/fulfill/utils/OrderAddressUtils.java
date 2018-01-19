package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.fulfill.model.Address;
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
            Address address = FwdAddressUtils.getUKFwdAddress();
            address.setName(FwdAddressUtils.getFwdRecipient(order));
            return address;
        }

        if (order.purchaseBack()) {
            Address address = FwdAddressUtils.getUSFwdAddress();
            address.setName(FwdAddressUtils.getFwdRecipient(order));
            return address;
        }

        Address address = Address.loadFromOrder(order);
        address.setName(recipientName(order));
        return address;

    }


    public static String recipientName(Order order) {
        String fullName = order.recipient_name.replaceAll("\"", "").replaceAll("&#34;", "");
        // 如果拼接的姓名超过亚马逊允许上限，且当前价格差异大于20，可以不加上No Invoice，但需要补上Remark
        // 存在Seller为Prime但标识了a的情况，此时仍然需要当做Prime的情况处理Full Name
        if (!order.sellerIsPrime()) {
            String s = fullName + order.getTask().getNoInvoiceText();
            int max = maxNameLength(OrderCountryUtils.getFulfillmentCountry(order));
            fullName = s.length() > max ? fullName : s;

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
