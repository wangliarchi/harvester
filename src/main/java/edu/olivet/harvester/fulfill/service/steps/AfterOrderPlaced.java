package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderPlacedSuccessPage;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:34 PM
 */
public class AfterOrderPlaced extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterOrderPlaced.class);

    @Override
    protected void process(FlowState state) {
        //navigate to order detail page
        try {
            OrderPlacedSuccessPage orderPlacedSuccessPage = new OrderPlacedSuccessPage(state.getBuyerPanel());
            orderPlacedSuccessPage.execute(state.getOrder());
        }catch (Exception e) {
            LOGGER.error("",e);
        }

    }

    @Inject StepHelper stepHelper;
    @Override
    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);
        return stepHelper.detectStep(state);
    }


}
