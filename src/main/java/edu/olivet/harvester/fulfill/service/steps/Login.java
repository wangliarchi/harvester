package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.service.FlowFactory.FlowState;
import edu.olivet.harvester.fulfill.service.FlowFactory.Step;
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


    @Inject
    ClearShoppingCart clearShoppingCart;
    @Inject
    AddToCart addToCart;
    @Inject
    ProcessToCheckout processToCheckout;

    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);

        if (this.prevStep!= null && this.prevStep.stepName.equals(ProcessToCheckout.class.getName())) {
            return processToCheckout;
        }

        //
        DOMElement navCartCountSpan = JXBrowserHelper.selectElementByCssSelector(state.getBuyerPanel().getBrowserView().getBrowser(), "#nav-cart-count");
        if (!"0".equals(navCartCountSpan.getInnerText())) {
            LOGGER.info("Next step is to clear shopping cart");
            return clearShoppingCart;
        }

        LOGGER.info("Next step is to add item to shopping cart");
        return addToCart;
    }
}
