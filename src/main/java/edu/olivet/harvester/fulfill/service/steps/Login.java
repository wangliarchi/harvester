package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 4:18 PM
 */
public class Login extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(Login.class);

    protected void process(FlowState state) {
        LoginPage loginPage = new LoginPage(state.getBuyerPanel());
        loginPage.execute(state.getOrder());
    }


    @Inject private
    ClearShoppingCart clearShoppingCart;
    @Inject private
    AddToCart addToCart;
    @Inject private
    ProcessToCheckout processToCheckout;

    @Inject private
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        Step prevStep = state.getPrevStep();
        state.setPrevStep(this);

        if (prevStep != null && !prevStep.stepName.equalsIgnoreCase(stepName)) {

            if (prevStep.stepName.equals(ProcessToCheckout.class.getName())) {
                return processToCheckout;
            }

            if (prevStep.stepName.equals(Checkout.class.getName())) {
                return processToCheckout;
            }

            return stepHelper.detectStep(state);
        }
        //check if there are items in cart
        DOMElement navCartCountSpan = JXBrowserHelper.selectElementByCssSelector(
                state.getBuyerPanel().getBrowserView().getBrowser(), "#nav-cart-count");

        if (navCartCountSpan != null && !"0".equals(navCartCountSpan.getInnerText())) {
            LOGGER.info("Next step is to clear shopping cart");
            return clearShoppingCart;
        }

        LOGGER.info("Next step is to add item to shopping cart");
        return addToCart;
    }
}
