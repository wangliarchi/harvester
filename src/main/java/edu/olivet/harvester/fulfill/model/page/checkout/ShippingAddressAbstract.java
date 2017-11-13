package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.model.Order;
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
            JXBrowserHelper.fillValueForFormField(browser, SELECTOR_FULL_NAME, order.recipient_name);
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

        JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");
    }
}
