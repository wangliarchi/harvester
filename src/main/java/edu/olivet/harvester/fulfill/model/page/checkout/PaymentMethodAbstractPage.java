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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
        List<String> cardEndingDigits = new ArrayList<>();

        boolean creditCardSelected = false;
        if (CollectionUtils.isNotEmpty(cards)) {
            for (DOMElement paymentRow : cards) {
                String lastDigits = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".card-info").getInnerText().replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
                if (creditCard.getCardNo().endsWith(lastDigits)) {

                    creditCardSelected = true;
                    paymentRow.click();
                    WaitTime.Shortest.execute();

                    reenterCCNumber(paymentRow);
                    break;
                }
            }
        }

        //credit card not found
        if (!creditCardSelected) {
            throw new BusinessException(String.format("Credit card with no %s not found.", creditCard.getCardNo()));
        }


    }

    @Repeat
    public void reenterCCNumber(DOMElement paymentRow) {
        //reenter credit card number

        DOMElement addrChalllenage = JXBrowserHelper.selectElementByCssSelector(paymentRow, ".addr-challenge");
        if (addrChalllenage == null || StringUtils.containsIgnoreCase(addrChalllenage.getAttribute("style"), "none")) {
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
            creditCardErrors.removeIf(it -> !JXBrowserHelper.isVisible(browser, "#" + it.getAttribute("id")));
            if (CollectionUtils.isNotEmpty(creditCardErrors)) {
                throw new BusinessException(creditCardErrors.get(0).getInnerText());
            }
        }


    }

}