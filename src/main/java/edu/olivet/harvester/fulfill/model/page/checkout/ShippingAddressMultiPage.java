package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMInputElement;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.FwdAddressUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class ShippingAddressMultiPage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingAddressMultiPage.class);
    private final I18N I18N_AMAZON;

    public ShippingAddressMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
        I18N_AMAZON = new I18N("i18n/Amazon");
    }

    public void execute(Order order) {
        //
        if (StringUtils.isBlank(order.recipient_name) && order.selfOrder) {
            selectAddressForOrder(order);
            return;
        }

        //fillAddress
        fillAddress(order);

        //select original address if amazon recommends address..
        checkAddressSuggestion();
    }


    public void fillAddress(Order order) {
        String errorMsg = "";
        for (int i = 0; i < 2; i++) {
            fillNewAddressForm(order);

            //submit
            DOMElement shipToBtn = JXBrowserHelper.selectElementByName(browser, "shipToThisAddress");
            if (shipToBtn == null) {
                throw new BusinessException("Error enter shipping address");
            }

            JXBrowserHelper.insertChecker(browser);
            shipToBtn.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);

            //check if error occur
            errorMsg = JXBrowserHelper.textFromElement(browser, "#addressIMB p");
            if (StringUtils.isNotBlank(errorMsg)) {
                JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
                LOGGER.error("Wrong address " + errorMsg);
                fixOrderAddress(order);
            } else {
                return;
            }
        }

        throw new OrderSubmissionException("Wrong address " + errorMsg);
    }

    public void checkAddressSuggestion() {
        //select original address if amazon recommends address..
        if (JXBrowserHelper.selectElementByCssSelector(browser, "#AVS") != null) {
            DOMInputElement selectOrigin = (DOMInputElement) JXBrowserHelper.selectElementByName(browser, "addr");
            if (selectOrigin != null && JXBrowserHelper.isVisible(selectOrigin)) {
                selectOrigin.setChecked(true);
            }

            JXBrowserHelper.insertChecker(browser);
            DOMElement btn = JXBrowserHelper.selectElementByName(browser, "useSelectedAddress");
            if (btn != null) {
                btn.click();
            }

            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }
    }

    public void selectAddressForOrder(Order order) {
        //find recent address from address book
        String orderCountryName = CountryStateUtils.getInstance().getCountryName(country.code());
        String translatedOrderCountryName = I18N_AMAZON.getText(orderCountryName, country);

        List<DOMElement> addressElements = JXBrowserHelper.selectElementsByCssSelector(browser, ".address-book-entry");
        for (DOMElement addressElement : addressElements) {
            String countryName = JXBrowserHelper.text(addressElement, ".displayAddressCountryName");

            //orderCountryName.equalsIgnoreCase(countryName)
            if (StringUtils.equalsAnyIgnoreCase(countryName, orderCountryName, translatedOrderCountryName)) {

                Address address = parseEnteredAddress(addressElement);
                order.recipient_name = address.getName();
                order.ship_address_1 = address.getAddress1();
                order.ship_address_2 = address.getAddress2();
                order.ship_city = address.getCity();
                order.ship_state = address.getState();
                order.ship_phone_number = address.getPhoneNumber();
                order.ship_zip = address.getZip();
                order.ship_country = address.getCountry();

                DOMElement continueBtn = JXBrowserHelper.selectVisibleElement(addressElement, ".ship-to-this-address.a-button .a-button-text");
                if (continueBtn != null) {
                    JXBrowserHelper.insertChecker(browser);
                    continueBtn.click();
                    WaitTime.Shortest.execute();
                    JXBrowserHelper.waitUntilNewPageLoaded(browser);
                    return;
                }
            }
        }

        throw new OrderSubmissionException("No address for country " + orderCountryName + " found.");
    }

    public Address parseEnteredAddress(DOMElement addressElement) {
        try {
            String name = JXBrowserHelper.text(addressElement, ".displayAddressUL .displayAddressFullName");
            String addressLine1 = JXBrowserHelper.text(addressElement, ".displayAddressUL .displayAddressAddressLine1");
            String addressLine2 = JXBrowserHelper.text(addressElement, ".displayAddressUL .displayAddressAddressLine2");
            String cityStateZip = JXBrowserHelper.text(addressElement, ".displayAddressUL .displayAddressCityStateOrRegionPostalCode");
            String country = JXBrowserHelper.text(addressElement, ".displayAddressUL .displayAddressCountryName");


            String[] parts = StringUtils.split(cityStateZip, ",");
            String city = parts[0].trim();
            String[] regionZip = StringUtils.split(parts[1].trim(), " ");
            String zip = regionZip[regionZip.length - 1];
            String state = StringUtils.join(Arrays.copyOf(regionZip, regionZip.length - 1), " ");

            String phoneNumber = JXBrowserHelper.text(addressElement, ".displayAddressUL .displayAddressPhoneNumber");
            try {
                String[] phoneNumberParts = StringUtils.split(phoneNumber.trim(), ":");
                phoneNumber = phoneNumberParts[phoneNumberParts.length - 1];
            } catch (Exception e) {
                phoneNumber = FwdAddressUtils.getUSFwdAddress().getPhoneNumber();
            }
            Address enteredAddress = new Address();
            enteredAddress.setName(name);
            enteredAddress.setAddress1(addressLine1);
            enteredAddress.setAddress2(addressLine2);
            enteredAddress.setCity(city);
            enteredAddress.setState(state);
            enteredAddress.setZip(zip);
            enteredAddress.setPhoneNumber(phoneNumber);
            enteredAddress.setNoInvoiceText(buyerPanel.getOrder().getRuntimeSettings().getNoInvoiceText());

            if (StringUtils.isNotBlank(country)) {
                enteredAddress.setCountry(country);
            }


            return enteredAddress;

        } catch (Exception e) {

            throw new BusinessException("Error parse shipping address for " + buyerPanel.getOrder().order_id);
        }
    }
}