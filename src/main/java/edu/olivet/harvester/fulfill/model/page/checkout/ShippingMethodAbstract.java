package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.ui.BuyerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 4:24 PM
 */
public abstract class ShippingMethodAbstract extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingMethodAbstract.class);

    public ShippingMethodAbstract(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }
}
