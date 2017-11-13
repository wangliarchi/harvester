package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class PaymentMethodOnePage extends PaymentMethodAbstractPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentMethodOnePage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#useThisPaymentMethodButtonId #continue-top";

    public PaymentMethodOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {

        DOMElement changeAddressLink = JXBrowserHelper.selectElementByCssSelector(browser, "#payChangeButtonId");
        if (changeAddressLink != null) {
            changeAddressLink.click();
            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNotFound(browser, "#payChangeButtonId");
        }


        //wait until it's loaded
        JXBrowserHelper.wait(browser, By.cssSelector(CONTINUE_BTN_SELECTOR));

        JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");
        //
        selectCreditCard(order);

        //continue;
        JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, CONTINUE_BTN_SELECTOR).click();

        JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");

        WaitTime.Shortest.execute();
        JXBrowserHelper.wait(browser, By.cssSelector("#payment-information"));
    }


}