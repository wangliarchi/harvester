package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.checkout.ShippingAddressMultiPage;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/23/2018 1:01 PM
 */
public class AddressVerification extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressVerification.class);


    @Inject private
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        Step nextStep = stepHelper.detectStep(state);
        LOGGER.info("Next step is " + nextStep.stepName);
        return nextStep;
    }

    @Override
    protected void process(FlowState state) {
        ShippingAddressMultiPage shippingAddressMultiPage = new ShippingAddressMultiPage(state.getBuyerPanel());
        shippingAddressMultiPage.checkAddressSuggestion();
    }
}
