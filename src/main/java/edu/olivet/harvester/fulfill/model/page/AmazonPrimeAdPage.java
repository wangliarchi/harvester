package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/16/17 2:57 PM
 */
public class AmazonPrimeAdPage extends FulfillmentPage implements PageObject {

    public AmazonPrimeAdPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    public void execute(Order order) {

        DOMElement noLink = JXBrowserHelper.selectElementByCssSelector(browser, "#mom-no-thanks");
        if (noLink != null && JXBrowserHelper.isVisible(noLink)) {
            noLink.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
            return;
        }

        noLink = JXBrowserHelper.selectElementByCssSelector(browser, ".prime-popover-actions button.primary");
        if (noLink != null && JXBrowserHelper.isVisible(noLink)) {
            noLink.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }


    }
}
