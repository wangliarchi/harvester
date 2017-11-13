package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:45 PM
 */
public class ShippingAddressOnePage extends ShippingAddressAbstract {

    public static String NEW_ADDRESS_SELECTOR = "#add-new-address-popover-link";

    public ShippingAddressOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {

        DOMElement changeAddressLink = JXBrowserHelper.selectElementByCssSelector(browser, "#addressChangeLinkId");
        if (changeAddressLink != null) {
            changeAddressLink.click();
            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNotFound(browser, "#addressChangeLinkId");
        }

        DOMElement newAddressLink = JXBrowserHelper.selectElementByCssSelector(browser, NEW_ADDRESS_SELECTOR);
        newAddressLink.click();

        JXBrowserHelper.wait(browser, By.cssSelector("#enterAddressFullName"));
        WaitTime.Shortest.execute();
        fillNewAddressForm(order);
        WaitTime.Shortest.execute();
        DOMElement useThisAddressBtn = JXBrowserHelper.selectElementByCssSelector(browser, ".a-popover-footer .a-button-input");
        useThisAddressBtn.click();
        WaitTime.Shortest.execute();

        JXBrowserHelper.wait(browser,By.cssSelector("#addressChangeLinkId"));


    }


}
