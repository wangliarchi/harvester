package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 12:53 PM
 */
public class OrderReviewOnePage extends OrderReviewAbstractPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReviewOnePage.class);

    public OrderReviewOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }


    @Override
    public void execute(Order order) {

    }
}
