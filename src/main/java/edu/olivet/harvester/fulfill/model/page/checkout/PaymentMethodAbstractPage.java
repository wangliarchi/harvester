package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.common.model.CreditCard;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
abstract class PaymentMethodAbstractPage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentMethodAbstractPage.class);
    private CreditCard creditCard;

    public PaymentMethodAbstractPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public void enterPromoCode(Order order) {
        if (StringUtils.isBlank(order.promotionCode)) {
            return;
        }

        DOMElement giftCardLink = JXBrowserHelper.selectVisibleElement(browser, "#wrapper-new-gc #gc-link-expander");
        if (giftCardLink != null) {
            giftCardLink.click();
            JXBrowserHelper.waitUntilVisible(browser, "#gcpromoinput");
            JXBrowserHelper.fillValueForFormField(browser, "#gcpromoinput", order.promotionCode);
            DOMElement giftCardButton = JXBrowserHelper.selectElementByCssSelector(browser, "#button-add-gcpromo");
            if (giftCardButton != null) {
                giftCardButton.click();
                WaitTime.Shortest.execute();
                JXBrowserHelper.waitUntilNotFound(giftCardButton);
            }

            DOMElement error = JXBrowserHelper.selectVisibleElement(browser, "#gcpromoinput.a-form-error");
            if (error != null) {
                throw new BusinessException("Promotional code is not valid.");
            }
        }
    }

    public void selectCreditCard(Order order) {

        //load all available cards
        try {
            creditCard = CreditCardUtils.getCreditCard(buyerPanel.getBuyer());
        } catch (Exception e) {
            creditCard = null;
        }

        if (creditCard == null) {
            if (StringUtils.isNotBlank(order.promotionCode)) {
                return;
            }
            throw new OrderSubmissionException("Credit card for buyer account " + buyer.getEmail() + " not found.");
        }
        JXBrowserHelper.waitUntilVisible(browser, ".payment-row");
        List<DOMElement> cards = JXBrowserHelper.selectElementsByCssSelector(browser, ".payment-row");

        if (CollectionUtils.isEmpty(cards)) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            throw new BusinessException("No credit card info found for buyer account " + buyer.getEmail());
        }

        for (DOMElement paymentRow : cards) {
            String cardInfoText = JXBrowserHelper.textFromElement(paymentRow, ".card-info");
            if (cardInfoText.contains(creditCard.lastDigits())) {
                paymentRow.click();
                WaitTime.Shortest.execute();
                //sometime amazon requires reenter cc number
                reenterCCNumber(paymentRow);
                //sometimes amazon requires reselect card currency.
                selectCurrency(paymentRow);
                return;
            }
        }


        //credit card not found
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
        throw new OrderSubmissionException(String.format("Credit card with last digits %s not found.", creditCard.lastDigits()));


    }

    @Repeat
    public void reenterCCNumber(DOMElement paymentRow) {
        //reenter credit card number

        DOMElement addrChallenge = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".addr-challenge");
        if (addrChallenge == null || JXBrowserHelper.isHidden(addrChallenge)) {
            return;
        }

        DOMElement addCreditCardNumber = JXBrowserHelper.selectElementByCssSelector(paymentRow, "#addCreditCardNumber");
        DOMElement confirmCardBtn = JXBrowserHelper.selectElementByCssSelector(paymentRow, "#confirm-card");

        if (confirmCardBtn != null && addCreditCardNumber != null) {
            JXBrowserHelper.fillValueForFormField(paymentRow, "#addCreditCardNumber", creditCard.getCardNo());

            WaitTime.Shortest.execute();

            DOMElement cvvField = JXBrowserHelper.selectElementByCssSelector(paymentRow, "#addCreditCardVerificationNumber");
            if (cvvField != null) {
                JXBrowserHelper.fillValueForFormField(paymentRow, "#addCreditCardVerificationNumber", creditCard.getCvv());
                WaitTime.Shortest.execute();
            }

            confirmCardBtn.click();

            WaitTime.Shortest.execute();

            List<DOMElement> creditCardErrors = JXBrowserHelper.selectElementsByCssSelector(browser, "#cc-errors .error-message");
            creditCardErrors.removeIf(JXBrowserHelper::isHidden);

            if (CollectionUtils.isNotEmpty(creditCardErrors)) {
                JXBrowserHelper.saveOrderScreenshot(buyerPanel.getOrder(), buyerPanel, "1");
                throw new BusinessException(creditCardErrors.get(0).getInnerText());
            }
            WaitTime.Shortest.execute();
        }


    }

    public void selectCurrency(DOMElement paymentRow) {
        DOMElement currencyList = JXBrowserHelper.selectVisibleElement(paymentRow, ".currency-list");
        if (currencyList == null || JXBrowserHelper.isHidden(currencyList)) {
            return;
        }
        WaitTime.Shortest.execute();
        DOMElement firstCurrency = JXBrowserHelper.selectVisibleElement(paymentRow, ".currency-list input.a-declarative");
        if (firstCurrency != null) {
            firstCurrency.click();
        }
    }

}