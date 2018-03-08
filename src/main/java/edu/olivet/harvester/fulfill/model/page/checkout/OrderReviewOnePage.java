package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.common.model.CreditCard;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 12:53 PM
 */
public class OrderReviewOnePage extends OrderReviewAbstractPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReviewOnePage.class);

    public OrderReviewOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }


    public boolean reviewPaymentMethod() {
        if (StringUtils.isNotBlank(buyerPanel.getOrder().promotionCode)) {
            Money total = parseTotal();
            if (total.getAmount().floatValue() > 1) {
                return false;
            }
        }
        String lastDigits = JXBrowserHelper.text(browser, "#payment-information .a-color-secondary span");
        CreditCard creditCard = CreditCardUtils.getCreditCard(buyerPanel.getBuyer());
        String creditCardLastDigits =  creditCard.getCardNo().substring(creditCard.getCardNo().length() - 4);
        LOGGER.info("Last digits {} - {}",lastDigits,creditCardLastDigits);
        return StringUtils.containsIgnoreCase(lastDigits,creditCardLastDigits);
    }


    @Override
    public void execute(Order order) {

    }
}
