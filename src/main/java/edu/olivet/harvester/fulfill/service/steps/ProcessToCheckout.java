package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.ShoppingCartPage;
import edu.olivet.harvester.fulfill.service.FlowFactory.FlowState;
import edu.olivet.harvester.fulfill.service.FlowFactory.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:23 PM
 */
public class ProcessToCheckout extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessToCheckout.class);



    protected void process(FlowState state) {
        ShoppingCartPage shoppingCartPage = new ShoppingCartPage(state.getBuyerPanel());
        shoppingCartPage.processToCheckout();
    }

    @Inject Checkout checkout;
    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);
        return checkout;
    }
}
