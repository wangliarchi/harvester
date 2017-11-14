package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 3:30 PM
 */
public class CantShipToAddressPage extends FulfillmentPage {

    public CantShipToAddressPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
    }


}
