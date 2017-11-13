package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewMultiPage;
import edu.olivet.harvester.fulfill.service.FlowFactory.FlowState;
import edu.olivet.harvester.fulfill.service.FlowFactory.Step;
import edu.olivet.harvester.fulfill.service.StepHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class ReviewOrderChangeShippingMethod extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewOrderChangeShippingMethod.class);


    //dispatcher method
    protected void process(FlowState state) {
        OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
        orderReviewMultiPage.changeShippingMethod();
    }

    @Inject
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        return stepHelper.detectStep(state);
    }
}
