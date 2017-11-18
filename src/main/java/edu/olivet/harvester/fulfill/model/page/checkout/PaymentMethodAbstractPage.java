package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
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
        creditCard = OrderBuyerUtils.getCreditCard(order);

        if (creditCard == null) {
            throw new BusinessException("Credit card for buyer account " + buyer.getEmail() + " not found.");
        }

        List<DOMElement> cards = JXBrowserHelper.selectElementsByCssSelector(browser, ".payment-row");

        if (CollectionUtils.isEmpty(cards)) {
            throw new BusinessException("No credit card info fdound for buyer account " + buyer.getEmail());
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

        throw new BusinessException(String.format("Credit card with no %s not found.", creditCard.getCardNo()));


    }

    @Repeat
    public void reenterCCNumber(DOMElement paymentRow) {
        //reenter credit card number

        DOMElement addrChalllenage = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".addr-challenge");
        if (addrChalllenage == null || JXBrowserHelper.isHidden(addrChalllenage)) {
            return;
        }

        DOMElement addCreditCardNumber = JXBrowserHelper.selectElementByCssSelector(paymentRow, "#addCreditCardNumber");
        DOMElement confirmCardBtn = JXBrowserHelper.selectElementByCssSelector(paymentRow, "#confirm-card");

        if (confirmCardBtn != null && addCreditCardNumber != null) {
            JXBrowserHelper.fillValueForFormField(paymentRow, "#addCreditCardNumber", creditCard.getCardNo());

            WaitTime.Shortest.execute();

            confirmCardBtn.click();

            WaitTime.Shortest.execute();

            List<DOMElement> creditCardErrors = JXBrowserHelper.selectElementsByCssSelector(browser, "#cc-errors .error-message");
            creditCardErrors.removeIf(it -> JXBrowserHelper.isHidden(it));

            if (CollectionUtils.isNotEmpty(creditCardErrors)) {
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