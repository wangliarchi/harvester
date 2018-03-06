package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.FwdAddressUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/3/2018 5:56 AM
 */
public class AddressesPage extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressesPage.class);
    private final I18N I18N_AMAZON;

    public AddressesPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
        I18N_AMAZON = new I18N("i18n/Amazon");
    }

    private void enter() {
        String url = country.baseUrl() + "/a/addresses";
        JXBrowserHelper.loadPage(browser, url);
    }

    @Override
    public void execute(Order order) {
        if (Strings.isNotBlank(order.recipient_name) || !order.selfOrder) {
            return;
        }

        enter();
        //find recent address from address book
        String orderCountryName = CountryStateUtils.getInstance().getCountryName(country.code());
        orderCountryName = I18N_AMAZON.getText(orderCountryName, country);

        List<DOMElement> addressElements = JXBrowserHelper.selectElementsByCssSelector(browser, ".address-column");
        for (DOMElement addressElement : addressElements) {
            String countryName = JXBrowserHelper.text(addressElement, "#address-ui-widgets-Country");

            if (orderCountryName.equalsIgnoreCase(countryName)) {

                Address address = parseEnteredAddress(addressElement);
                if (address == null) {
                    continue;
                }
                order.recipient_name = address.getName();
                order.ship_address_1 = address.getAddress1();
                order.ship_address_2 = address.getAddress2();
                order.ship_city = address.getCity();
                order.ship_state = address.getState();
                order.ship_phone_number = address.getPhoneNumber();
                order.ship_zip = address.getZip();
                order.ship_country = address.getCountry();

                return;
            }
        }

        throw new OrderSubmissionException("No address for country " + orderCountryName + " found.");
    }

    public Address parseEnteredAddress(DOMElement addressElement) {
        try {
            String name = JXBrowserHelper.text(addressElement, "#address-ui-widgets-FullName");
            String addressLine1 = JXBrowserHelper.text(addressElement, "#address-ui-widgets-AddressLineOne");
            String addressLine2 = JXBrowserHelper.text(addressElement, "#address-ui-widgets-AddressLineTwo");
            String cityStateZip = JXBrowserHelper.text(addressElement, "#address-ui-widgets-CityStatePostalCode");
            String country = JXBrowserHelper.text(addressElement, "#address-ui-widgets-Country");

            String[] parts = StringUtils.split(cityStateZip, ",");
            String city = parts[0].trim();
            String[] regionZip = StringUtils.split(parts[1].trim(), " ");
            String zip = regionZip[regionZip.length - 1];
            String state = StringUtils.join(Arrays.copyOf(regionZip, regionZip.length - 1), " ");

            String phoneNumber = JXBrowserHelper.text(addressElement, "#address-ui-widgets-PhoneNumber");
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

            //throw new BusinessException("Error parse shipping address for " + buyerPanel.getOrder().order_id);
        }

        return null;
    }
}
