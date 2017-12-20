package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewMultiPage;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class ReviewOrderChangePaymentMethod extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewOrderChangePaymentMethod.class);



    //dispatcher method
    protected void process(FlowState state) {
        LOGGER.info("Change shipping address");
        OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
        orderReviewMultiPage.changePaymentMethod();
    }

    @Inject SelectPaymentMethod selectPaymentMethod;
    public Step createDynamicInstance(FlowState state) {
        return selectPaymentMethod;
    }
}
