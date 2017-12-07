package edu.olivet.harvester.fulfill.model.page.checkout;

import com.sun.java.browser.plugin2.DOM;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMInputElement;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
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
    private DOMElement btn;

    public ShippingAddressMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {

        fillNewAddressForm(order);

        DOMElement shipToBtn = JXBrowserHelper.selectElementByName(browser, "shipToThisAddress");
        JXBrowserHelper.insertChecker(browser);
        assert shipToBtn != null;
        shipToBtn.click();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);

        //check if error occur
        DOMElement errorMsg = JXBrowserHelper.selectElementByCssSelector(browser, "#identity-add-new-address #addressIMB");
        if (errorMsg != null) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            throw new BusinessException("Wrong address " + errorMsg.getInnerText());
        }

        //select original address if amazon recommends address..
        if (JXBrowserHelper.selectElementByCssSelector(browser, "#AVS") != null) {
            DOMInputElement selectOrigin = (DOMInputElement) JXBrowserHelper.selectElementByName(browser, "addr");
            if (selectOrigin != null && JXBrowserHelper.isVisible(selectOrigin)) {
                selectOrigin.setChecked(true);
            }

            JXBrowserHelper.insertChecker(browser);
            DOMElement btn = JXBrowserHelper.selectElementByName(browser, "useSelectedAddress");
            btn.click();

            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }
    }
}