package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.ShoppingCartPage;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 9:53 AM
 */
public class ClearShoppingCart extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearShoppingCart.class);


    protected void process(FlowState state) {
        ShoppingCartPage shoppingCartPage = new ShoppingCartPage(state.getBuyerPanel());
        shoppingCartPage.clearShoppingCart();
    }

    @Inject
    AddToCart addToCart;

    public Step createDynamicInstance(FlowState state) {
        if (PSEventListener.stopped()) {
            return null;
        }
        return addToCart;
    }
}

