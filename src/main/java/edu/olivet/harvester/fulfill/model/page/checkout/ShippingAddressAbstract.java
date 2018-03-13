package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.utils.OrderAddressUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
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
        Address address = OrderAddressUtils.orderShippingAddress(order);
        if (address == null) {
            throw new BusinessException("Order address is not valid");
        }


        _fillNewAddressForm(address);

    }

    private void _fillNewAddressForm(Address address) {
        try {
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_FULL_NAME, address.getName());
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_ADDR1, address.getAddress1());
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_ADDR2, address.getAddress2());
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_CITY, address.getCity());

            if (JXBrowserHelper.isSelectElement(browser, SELECTOR_STATE)) {
                JXBrowserHelper.setValueForFormSelect(browser, SELECTOR_STATE, address.getFullStateName());
            } else {
                JXBrowserHelper.fillValueForFormField(browser, SELECTOR_STATE, address.getState());
            }

            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_ZIP, address.getZip());
            JXBrowserHelper.setValueForFormSelect(browser, SELECTOR_COUNTRY, address.getCountryCode());
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_PHONE, address.getPhoneNumber());
        } catch (Exception e) {
            LOGGER.info("can not fill ship address form", e);
            throw new BusinessException("can not fill ship address form, " + e.getMessage());
        }
    }

    protected void fixOrderAddress(Order order) {
        if (order.ship_zip.length() < 5) {
            order.ship_zip = "0" + order.ship_zip;
        } else {
            String tmp = order.ship_address_1;
            order.ship_address_1 = order.ship_address_2;
            order.ship_address_2 = tmp;
        }
    }


}
