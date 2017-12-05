package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.AmazonPrimeAdPage;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/16/17 2:55 PM
 */
public class AmazonPrimeAd extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonPrimeAd.class);

    @Override
    protected void process(FlowState state) {
        AmazonPrimeAdPage amazonPrimeAdPage = new AmazonPrimeAdPage(state.getBuyerPanel());
        amazonPrimeAdPage.execute(state.getOrder());
    }

    @Inject
    StepHelper stepHelper;

    @Override
    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);
        Step nextStep = stepHelper.detectStep(state);
        LOGGER.info("Next step is " + nextStep.stepName);
        return nextStep;
    }
}
