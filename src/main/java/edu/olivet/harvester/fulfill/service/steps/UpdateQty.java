package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.checkout.ShippingMethodOnePage;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class UpdateQty extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateQty.class);


    //dispatcher method
    protected void process(FlowState state) {

        ShippingMethodOnePage selectShippingMethod = new ShippingMethodOnePage(state.getBuyerPanel());
        selectShippingMethod.updateQty(state.getOrder());
    }

    @Inject
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        return stepHelper.detectStep(state);
    }
}
