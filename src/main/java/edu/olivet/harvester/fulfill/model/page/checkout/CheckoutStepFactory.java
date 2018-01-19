package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.ui.panel.BuyerPanel;

import java.lang.reflect.Constructor;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:39 PM
 */
public class CheckoutStepFactory {
    public static FulfillmentPage getCheckoutStepPage(BuyerPanel buyerPanel,
                                                      CheckoutEnum.CheckoutStep step, CheckoutEnum.CheckoutPageType type) {
        String className = CheckoutStepFactory.class.getPackage().getName() + "." + step.name() + type.name();
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor(BuyerPanel.class);
            Object object = ctor.newInstance(buyerPanel);
            return (FulfillmentPage) object;
        } catch (Exception e) {
            throw new BusinessException(e);
        }

    }
}
