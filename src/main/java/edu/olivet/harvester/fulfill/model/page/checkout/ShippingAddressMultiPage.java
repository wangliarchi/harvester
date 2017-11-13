package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class ShippingAddressMultiPage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingAddressMultiPage.class);

    public ShippingAddressMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {

        fillNewAddressForm(order);

        DOMElement shipToBtn = JXBrowserHelper.selectElementByName(browser, "shipToThisAddress");
        shipToBtn.click();

        JXBrowserHelper.waitUntilNotFound(browser, SELECTOR_FULL_NAME);

        DOMElement errorMsg = JXBrowserHelper.selectElementByCssSelector(browser, "#identity-add-new-address #addressIMB");
        if (errorMsg != null) {
            LOGGER.error("Wrong addres " + errorMsg.getInnerText());
        }

    }
}