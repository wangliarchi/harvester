package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.harvester.fulfill.utils.pagehelper.GiftOptionHelper;
import edu.olivet.harvester.fulfill.utils.pagehelper.QtyUtils;
import edu.olivet.harvester.fulfill.utils.pagehelper.ShipOptionUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class ShippingMethodOnePage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingMethodOnePage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#shippingOptionFormId input.a-button-text";

    public ShippingMethodOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        //get all available options
        JXBrowserHelper.saveOrderScreenshot(buyerPanel.getOrder(), buyerPanel, "1");

        ShipOptionUtils.selectShipOption(buyerPanel);

        JXBrowserHelper.saveOrderScreenshot(buyerPanel.getOrder(), buyerPanel, "2");
        //remove long edd?
    }

    public void updateQty(Order order) {
        QtyUtils.updateQty(buyerPanel, order);

        //set gift option
        GiftOptionHelper.giftOption(buyerPanel.getBrowserView().getBrowser(),order);
    }



}