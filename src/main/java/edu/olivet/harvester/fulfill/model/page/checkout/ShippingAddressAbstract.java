package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 4:24 PM
 */
public abstract class ShippingAddressAbstract extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingAddressAbstract.class);
    protected static final String SELECTOR_FULL_NAME = "#enterAddressFullName";
    protected static final String SELECTOR_ADDR1 = "#enterAddressAddressLine1";
    protected static final String SELECTOR_ADDR2 = "#enterAddressAddressLine2";
    protected static final String SELECTOR_CITY = "#enterAddressCity";
    protected static final String SELECTOR_STATE = "#enterAddressStateOrRegion";
    protected static final String SELECTOR_ZIP = "#enterAddressPostalCode";
    protected static final String SELECTOR_ZIP_JP_1 = "#enterAddressPostalCode1";
    protected static final String SELECTOR_ZIP_JP_2 = "#enterAddressPostalCode2";
    protected static final String SELECTOR_COUNTRY = "#enterAddressCountryCode";
    protected static final String SELECTOR_COUNTRY_JP = "#enterAddressAddressLine3";
    protected static final String SELECTOR_PHONE = "#enterAddressPhoneNumber";

    public ShippingAddressAbstract(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    protected void fillNewAddressForm(Order order) {
        try {
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_FULL_NAME, recipientName(order));
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_ADDR1, order.ship_address_1);
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_ADDR2, order.ship_address_2);
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_CITY, order.ship_city);
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_STATE, order.ship_state);
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_ZIP, order.ship_zip);
            JXBrowserHelper.setValueForFormSelect(browser, SELECTOR_COUNTRY, order.ship_country);
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_PHONE, order.ship_phone_number);
        } catch (Exception e) {
            LOGGER.info("can not fill ship address form", e);
            throw new BusinessException(e);
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
    }

    public String recipientName(Order order) {
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
    public int maxNameLength(Country country) {
        return country == Country.CA ? 35 : 50;
    }
}
