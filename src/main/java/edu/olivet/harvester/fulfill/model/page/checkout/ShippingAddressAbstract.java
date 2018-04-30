package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderAddressUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    protected final I18N I18N_AMAZON;

    public ShippingAddressAbstract(BuyerPanel buyerPanel) {
        super(buyerPanel);
        I18N_AMAZON = new I18N("i18n/Amazon");
    }

    protected void fillNewAddressForm(Order order) {
        Address address = OrderAddressUtils.orderShippingAddress(order);
        if (address == null) {
            throw new BusinessException("Order address is not valid");
        }

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
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
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

    protected DOMElement getRandomAddressElement() {
        String orderCountryName = CountryStateUtils.getInstance().getCountryName(country.code());
        String translatedOrderCountryName = I18N_AMAZON.getText(orderCountryName, country);
        String usCountryName = "United States";
        String translatedUSCountryName = I18N_AMAZON.getText("country.us", country);
        List<DOMElement> addressElements = getAddressElementList();
        List<DOMElement> validAddressElements = new ArrayList<>();
        List<DOMElement> usAddressElements = new ArrayList<>();
        for (DOMElement addressElement : addressElements) {
            String countryName = getAddressElementCountry(addressElement);

            //orderCountryName.equalsIgnoreCase(countryName)
            if (Strings.containsAnyIgnoreCase(countryName, orderCountryName, translatedOrderCountryName)) {
                validAddressElements.add(addressElement);
            }

            if (Strings.containsAnyIgnoreCase(countryName, usCountryName, translatedUSCountryName)) {
                usAddressElements.add(addressElement);
            }

        }
        if (CollectionUtils.isEmpty(validAddressElements)) {
            validAddressElements = usAddressElements;
        }

        if (CollectionUtils.isEmpty(validAddressElements)) {
            throw new OrderSubmissionException("No address for country " + orderCountryName + " found.");
        }

        Random rand = new Random();
        return validAddressElements.get(rand.nextInt(validAddressElements.size()));
    }


    public List<DOMElement> getAddressElementList() {
        return null;
    }

    public String getAddressElementCountry(DOMElement addressElement) {
        return null;
    }
}
