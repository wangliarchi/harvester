package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class PaymentMethodOnePage extends PaymentMethodAbstractPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentMethodOnePage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#useThisPaymentMethodButtonId #continue-top";
    private static final String CHANGE_PAYMENT_METHOD_BTN_SELECTOR = "#payChangeButtonId";

    public PaymentMethodOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {

        //wait until it's loaded
        JXBrowserHelper.wait(browser, By.cssSelector(CONTINUE_BTN_SELECTOR));

        DOMElement changePaymentLink = JXBrowserHelper.selectElementByCssSelector(browser, CHANGE_PAYMENT_METHOD_BTN_SELECTOR);
        if (changePaymentLink != null) {
            changePaymentLink.click();
            JXBrowserHelper.waitUntilNotFound(browser, CHANGE_PAYMENT_METHOD_BTN_SELECTOR);
            WaitTime.Shortest.execute();
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

        //
        selectCreditCard(order);

        click(order);

    }

    @Repeat
    public void click(Order order) {
        //continue;
        //JXBrowserHelper.insertChecker(browser);
        JXBrowserHelper.selectElementByCssSelector(browser, CONTINUE_BTN_SELECTOR).click();

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
        //JXBrowserHelper.waitUntilNewPageLoaded(browser);
        JXBrowserHelper.waitUntilNotFound(browser, CONTINUE_BTN_SELECTOR);
    }


}