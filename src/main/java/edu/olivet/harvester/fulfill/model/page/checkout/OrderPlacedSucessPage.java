package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:35 PM
 */
public class OrderPlacedSucessPage extends FulfillmentPage {

    public OrderPlacedSucessPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    public void execute(Order order) {
        //view order link
        DOMElement viewLink = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser,".a-alert-content a.a-link-emphasis");

        JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");

        viewLink.click();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);
        //JXBrowserHelper.waitUntilNotFound(browser,".a-alert-content a.a-link-emphasis");
    }
}
