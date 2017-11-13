package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutEnum;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutStepFactory;
import edu.olivet.harvester.fulfill.service.FlowFactory.FlowState;
import edu.olivet.harvester.fulfill.service.FlowFactory.Step;
import edu.olivet.harvester.fulfill.service.StepHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class SelectShippingMethod extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectShippingMethod.class);


    //dispatcher method
    protected void process(FlowState state) {
        Browser browser = state.getBuyerPanel().getBrowserView().getBrowser();

        FulfillmentPage page;
        //todo add to a helper method
        if (stepHelper.detectCurrentPage(state) == CheckoutEnum.CheckoutPage.ShippingMethodOnePage) {
            page = CheckoutStepFactory.getCheckoutStepPage(state.getBuyerPanel(), CheckoutEnum.CheckoutStep.ShippingMethod, CheckoutEnum.CheckoutPageType.OnePage);
        } else {
            page = CheckoutStepFactory.getCheckoutStepPage(state.getBuyerPanel(), CheckoutEnum.CheckoutStep.ShippingMethod, CheckoutEnum.CheckoutPageType.MultiPage);
        }

        page.execute(state.getOrder());

    }


    @Inject
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        Step nextStep = stepHelper.detectStep(state);
        LOGGER.info("Next step is " + nextStep.stepName);
        return nextStep;
    }
}
