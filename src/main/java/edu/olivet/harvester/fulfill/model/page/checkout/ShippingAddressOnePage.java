package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMInputElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:45 PM
 */
public class ShippingAddressOnePage extends ShippingAddressAbstract {

    public static String NEW_ADDRESS_SELECTOR = "#add-new-address-popover-link";
    private final I18N I18N_AMAZON;

    public ShippingAddressOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
        I18N_AMAZON = new I18N("i18n/Amazon");
    }

    @Repeat
    public void execute(Order order) {


        DOMElement changeAddressLink = JXBrowserHelper.selectElementByCssSelector(browser, "#addressChangeLinkId");
        if (changeAddressLink != null) {
            JXBrowserHelper.click(changeAddressLink);
        }

        if (StringUtils.isBlank(order.recipient_name) && order.selfOrder) {
            JXBrowserHelper.waitUntilVisible(browser, ".a-row.address-row");
            //find recent address from address book
            String orderCountryName = CountryStateUtils.getInstance().getCountryName(country.code());
            orderCountryName = I18N_AMAZON.getText(orderCountryName, country);
            String translatedOrderCountryName = I18N_AMAZON.getText(orderCountryName, country);
            List<DOMElement> addressElements = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-row.address-row");
            for (DOMElement addressElement : addressElements) {
                String addressText = JXBrowserHelper.text(addressElement, ".a-label.a-radio-label");

                if (Strings.containsAnyIgnoreCase(addressText, orderCountryName, translatedOrderCountryName)) {
                    DOMElement editLink = JXBrowserHelper.selectVisibleElement(addressElement, ".address-edit-link a");
                    if (editLink != null) {
                        editLink.click();

                        JXBrowserHelper.waitUntilVisible(browser, "#identity-add-new-address");
                        order.recipient_name = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_FULL_NAME);
                        order.ship_address_1 = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_ADDR1);
                        order.ship_address_2 = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_ADDR2);
                        order.ship_city = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_CITY);
                        order.ship_state = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_STATE);
                        order.ship_phone_number = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_PHONE);
                        order.ship_zip = JXBrowserHelper.getValueFromFormField(browser, SELECTOR_ZIP);
                        order.ship_country = orderCountryName;

                        WaitTime.Shortest.execute();
                        break;
                    }
                }
            }

            if (StringUtils.isBlank(order.recipient_name)) {
                throw new OrderSubmissionException("No address for country " + orderCountryName + " found.");
            }

        } else {
            DOMElement newAddressLink = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, NEW_ADDRESS_SELECTOR);
            newAddressLink.click();

            JXBrowserHelper.wait(browser, By.cssSelector("#enterAddressFullName"));
            WaitTime.Shortest.execute();
            fillNewAddressForm(order);
            WaitTime.Shortest.execute();
        }
        //amazon may pop up address verification after clicking "use this address" btn, use user's original input by default.
        int tried = 0;
        while (tried <= Constants.MAX_REPEAT_TIMES) {
            tried++;
            DOMElement useThisAddressBtn = JXBrowserHelper.selectVisibleElement(browser, ".a-popover-footer .a-button-primary .a-button-input");

            if (useThisAddressBtn == null) {
                break;
            }

            DOMInputElement selectOrigin = (DOMInputElement) JXBrowserHelper.selectElementByName(browser, "addr");
            if (selectOrigin != null && JXBrowserHelper.isVisible(selectOrigin)) {
                selectOrigin.setChecked(true);
            }

            useThisAddressBtn.click();

            WaitTime.Shortest.execute();
        }
        if (JXBrowserHelper.selectVisibleElement(browser, ".a-popover-footer .a-button-primary .a-button-input") != null) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            throw new BusinessException("Error to enter shipping address " + Address.loadFromOrder(order));
        }


    }


}
