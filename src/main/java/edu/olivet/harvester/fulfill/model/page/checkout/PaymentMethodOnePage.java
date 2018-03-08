package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
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
        //enter promo code if any
        if (StringUtils.isNotBlank(order.promotionCode)) {
            enterPromoCode(order);
            String grandTotalText = JXBrowserHelper.text(browser, "#subtotals-marketplace-table .grand-total-price");
            try {
                Money total = Money.fromText(grandTotalText, buyerPanel.getCountry());
                if (total.getAmount().floatValue() == 0) {
                    click(order);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Error reading grand total. ", e);
            }
        }

        //wait until it's loaded
        JXBrowserHelper.wait(browser, By.cssSelector(CONTINUE_BTN_SELECTOR));

        DOMElement changePaymentLink = JXBrowserHelper.selectVisibleElement(browser, CHANGE_PAYMENT_METHOD_BTN_SELECTOR);
        if (changePaymentLink != null) {
            changePaymentLink.click();
            //JXBrowserHelper.clickJS(browser, CHANGE_PAYMENT_METHOD_BTN_SELECTOR);
        }

        WaitTime.Shortest.execute();
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

        selectCreditCard(order);

        click(order);
    }

    @Repeat
    public void click(Order order) {
        //continue;
        //JXBrowserHelper.insertChecker(browser);
        DOMElement continueBtn = JXBrowserHelper.selectElementByCssSelector(browser, CONTINUE_BTN_SELECTOR);
        if (continueBtn != null) {
            continueBtn.click();
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            //JXBrowserHelper.waitUntilNewPageLoaded(browser);
            JXBrowserHelper.waitUntilNotFound(browser, CONTINUE_BTN_SELECTOR);
        } else {
            LOGGER.error("Continue Btn not found on PaymentMethodOnePage");
        }
    }

    public void enterPromoCode(Order order) {
        try {
            String grandTotalText = JXBrowserHelper.text(browser, "#subtotals-marketplace-table .grand-total-price");
            Money total = Money.fromText(grandTotalText, buyerPanel.getCountry());
            //DOMElement checkbox = JXBrowserHelper.selectVisibleElement(browser, "#pm_promo");
            if (total.getAmount().floatValue() > 1) {
                JXBrowserHelper.waitUntilVisible(browser, "#spc-gcpromoinput,#gcpromoinput");
                JXBrowserHelper.fillValueForFormField(browser, "#spc-gcpromoinput,#gcpromoinput", order.promotionCode);
                WaitTime.Shortest.execute();
                JXBrowserHelper.selectVisibleElement(browser, "#gcApplyButtonId .a-button-inner,#new-giftcard-promotion .a-button-inner").click();
                WaitTime.Shortest.execute();
                JXBrowserHelper.waitUntilVisible(browser, "#gcApplyButtonId .a-button-inner,#new-giftcard-promotion .a-button-inner");
                WaitTime.Short.execute();
            }
        } catch (Exception e) {
            //
            LOGGER.error("", e);
        }
    }


}