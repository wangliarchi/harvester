package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutEnum;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutStepFactory;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class EnterShippingAddress extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnterShippingAddress.class);


    //dispatcher method
    protected void process(FlowState state) {

        FulfillmentPage page;
        if (stepHelper.detectCurrentPage(state) == CheckoutEnum.CheckoutPage.ShippingAddress) {
            page = CheckoutStepFactory.getCheckoutStepPage(
                    state.getBuyerPanel(), CheckoutEnum.CheckoutStep.ShippingAddress, CheckoutEnum.CheckoutPageType.MultiPage);
        } else {
            page = CheckoutStepFactory.getCheckoutStepPage(
                    state.getBuyerPanel(), CheckoutEnum.CheckoutStep.ShippingAddress, CheckoutEnum.CheckoutPageType.OnePage);
        }

        page.execute(state.getOrder());

        WaitTime.Short.execute();
    }

    @Inject private
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        Step nextStep = stepHelper.detectStep(state);
        LOGGER.info("Next step is " + nextStep.stepName);
        return nextStep;
    }
}
