package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class PaymentMethodMultiPage extends PaymentMethodAbstractPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentMethodMultiPage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#order-summary-container #continue-top";

    public PaymentMethodMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        //wait until it's loaded
        JXBrowserHelper.wait(browser, By.cssSelector("#new-payment-methods"));

        JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");
        //
        selectCreditCard(order);

        //continue;
        JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, CONTINUE_BTN_SELECTOR).click();
        JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");

        JXBrowserHelper.waitUntilNotFound(browser,"#new-payment-methods");
    }


}