package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMInputElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:45 PM
 */
public class ShippingAddressOnePage extends ShippingAddressAbstract {

    public static String NEW_ADDRESS_SELECTOR = "#add-new-address-popover-link";

    public ShippingAddressOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Repeat
    public void execute(Order order) {

        DOMElement changeAddressLink = JXBrowserHelper.selectElementByCssSelector(browser, "#addressChangeLinkId");
        if (changeAddressLink != null) {
            JXBrowserHelper.click(changeAddressLink);
        }

        DOMElement newAddressLink = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, NEW_ADDRESS_SELECTOR);
        newAddressLink.click();

        JXBrowserHelper.wait(browser, By.cssSelector("#enterAddressFullName"));
        WaitTime.Shortest.execute();
        fillNewAddressForm(order);
        WaitTime.Shortest.execute();

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
