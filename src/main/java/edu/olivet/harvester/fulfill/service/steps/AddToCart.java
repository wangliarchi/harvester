package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.OfferListingPage;
import edu.olivet.harvester.fulfill.service.SellerService;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 9:59 AM
 */
public class AddToCart extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddToCart.class);



    @Inject SellerService sellerService;
    protected void process(FlowState state) {
        OfferListingPage offerListingPage = new OfferListingPage(state.getBuyerPanel(), sellerService);
        offerListingPage.addToCart(state.getOrder());
    }

    @Inject ProcessToCheckout processToCheckout;
    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);
        return processToCheckout;
    }
}
