package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.NoBudgetException;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;

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

        if (StringUtils.isNotBlank(order.promotionCode)) {
            enterPromoCode(order);
            WaitTime.Short.execute();
        }

        //existing-balance radio-col pm_promo
        DOMElement existingBalanceRadioElement = JXBrowserHelper.selectVisibleElement(browser, "#existing-balance input[type='radio']#pm_promo");
        if (existingBalanceRadioElement != null) {
            existingBalanceRadioElement.click();
        } else {
            selectCreditCard(order);
        }

        click();
    }

    @Repeat
    public void click() {
        try {
            JXBrowserHelper.waitUntilVisible(browser, CONTINUE_BTN_SELECTOR);
        } catch (Exception e) {
            throw new NoBudgetException("Something wrong with payment method. Please check gift card balance and/or credit card info");
        }

        //continue;
        DOMElement continueBtn = JXBrowserHelper.selectVisibleElement(browser, CONTINUE_BTN_SELECTOR);
        if (continueBtn != null) {
            //JXBrowserHelper.insertChecker(browser);
            continueBtn.click();
            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        } else {
            throw new NoBudgetException("Something wrong with payment method. Please check gift card balance and/or credit card info");
        }
    }


}