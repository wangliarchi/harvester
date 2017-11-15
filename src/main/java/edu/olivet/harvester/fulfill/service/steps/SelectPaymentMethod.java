package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutEnum;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutStepFactory;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class SelectPaymentMethod extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectPaymentMethod.class);


    //dispatcher method
    protected void process(FlowState state) {

        FulfillmentPage page;
        if (stepHelper.detectCurrentPage(state) == CheckoutEnum.CheckoutPage.PaymentMethodOnePage) {
            page = CheckoutStepFactory.getCheckoutStepPage(state.getBuyerPanel(), CheckoutEnum.CheckoutStep.PaymentMethod, CheckoutEnum.CheckoutPageType.OnePage);
        } else {
            page = CheckoutStepFactory.getCheckoutStepPage(state.getBuyerPanel(), CheckoutEnum.CheckoutStep.PaymentMethod, CheckoutEnum.CheckoutPageType.MultiPage);
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
