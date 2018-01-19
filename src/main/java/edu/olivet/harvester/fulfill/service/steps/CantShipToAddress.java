package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.checkout.CantShipToAddressPage;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 3:32 PM
 */
public class CantShipToAddress extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(CantShipToAddress.class);

    @Override
    protected void process(FlowState state) {
        CantShipToAddressPage cantShipToAddressPage = new CantShipToAddressPage(state.getBuyerPanel());
        cantShipToAddressPage.execute(state.getOrder());
    }

    @Inject private StepHelper stepHelper;

    @Override
    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);
        Step nextStep = stepHelper.detectStep(state);
        LOGGER.info("Next step is " + nextStep.stepName);
        return nextStep;
    }
}
