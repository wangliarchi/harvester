package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class PaymentMethodMultiPage extends PaymentMethodAbstractPage {

    //private static final Logger LOGGER = LoggerFactory.getLogger(PaymentMethodMultiPage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#order-summary-container #continue-top";

    public PaymentMethodMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        //wait until it's loaded
        JXBrowserHelper.wait(browser, By.cssSelector("#new-payment-methods"));

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
        //
        selectCreditCard(order);

        click();

    }

    @Repeat
    public void click() {
        //continue;
        DOMElement continueBtn = JXBrowserHelper.selectElementByCssSelector(browser, CONTINUE_BTN_SELECTOR);
        if (continueBtn != null && JXBrowserHelper.isVisible(continueBtn)) {
            //JXBrowserHelper.insertChecker(browser);
            continueBtn.click();
            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }
    }


}