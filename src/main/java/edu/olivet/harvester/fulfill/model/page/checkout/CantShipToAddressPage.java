package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.utils.OrderAddressUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 3:30 PM
 */
public class CantShipToAddressPage extends FulfillmentPage {

    public CantShipToAddressPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");


        String errorMsg = JXBrowserHelper.text(browser,
                "#changeQuantityFormId .alertMessage,#changeQuantityFormId .lineitem-error-message");
        if (StringUtils.isNotBlank(errorMsg)) {
            throw new OrderSubmissionException(errorMsg);
        }

        //if no error message, try to change to one address mode
        DOMElement shipToOneAddressLink = JXBrowserHelper.selectElementByCssSelector(browser, "#changeQuantityFormId a.pipeline-link");
        DOMElement useThisAddressBtn = JXBrowserHelper.selectElementByCssSelector(browser,
                "#changeQuantityFormId .a-button.primary-action-button input");
        if (shipToOneAddressLink != null) {
            String currentAddress = JXBrowserHelper.text(browser, "#changeQuantityFormId .address-dropdown .a-dropdown-prompt").trim();
            if (StringUtils.isNotBlank(currentAddress) &&
                    Strings.containsAnyIgnoreCase(currentAddress, OrderAddressUtils.orderShippingAddress(order).getName()) &&
                    useThisAddressBtn != null) {
                useThisAddressBtn.click();
            } else {
                shipToOneAddressLink.click();
            }

            JXBrowserHelper.waitUntilNotFound(shipToOneAddressLink);
            return;
        }

        //try to process anyway. address will be validated before placing the order
        if (useThisAddressBtn != null) {
            useThisAddressBtn.click();
            JXBrowserHelper.waitUntilNotFound(useThisAddressBtn);
            return;
        }

        //no use this address button?
        errorMsg = "Sorry, this item can't be shipped to selected address.";
        throw new OrderSubmissionException(errorMsg);


    }


}
