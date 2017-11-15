package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:39 PM
 */
public class Checkout extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(Checkout.class);


    //dispatcher method
    protected void process(FlowState state) {

    }


    @Inject
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        Step nextStep = stepHelper.detectStep(state);
        LOGGER.info("Next step is " + nextStep.stepName);
        return nextStep;
    }
}
