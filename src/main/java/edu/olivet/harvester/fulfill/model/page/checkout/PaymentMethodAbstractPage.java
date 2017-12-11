package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.model.CreditCard;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public abstract class PaymentMethodAbstractPage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentMethodAbstractPage.class);
    private CreditCard creditCard;

    public PaymentMethodAbstractPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }


    public void selectCreditCard(Order order) {

        //load all available cards
        creditCard = CreditCardUtils.getCreditCard(order);

        if (creditCard == null) {
            throw new OrderSubmissionException("Credit card for buyer account " + buyer.getEmail() + " not found.");
        }
        JXBrowserHelper.waitUntilVisible(browser, ".payment-row");
        List<DOMElement> cards = JXBrowserHelper.selectElementsByCssSelector(browser, ".payment-row");

        if (CollectionUtils.isEmpty(cards)) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            throw new BusinessException("No credit card info found for buyer account " + buyer.getEmail());
        }

        for (DOMElement paymentRow : cards) {
            String lastDigits = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".card-info").getInnerText().replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
            if (creditCard.getCardNo().endsWith(lastDigits)) {
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
        throw new BusinessException(String.format("Credit card with no %s not found.", creditCard.getCardNo()));


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
        }


    }

    public void selectCurrency(DOMElement paymentRow) {
        DOMElement currencyList = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".a-row.currency-list");
        if (currencyList == null || JXBrowserHelper.isHidden(currencyList)) {
            return;
        }

        DOMElement firstCurrency = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".a-row.currency-list input.a-declarative");
        firstCurrency.click();


    }

}