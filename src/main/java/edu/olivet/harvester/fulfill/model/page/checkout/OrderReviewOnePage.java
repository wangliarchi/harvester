package edu.olivet.harvester.fulfill.model.page.checkout;

import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.model.CreditCard;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
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
        String lastDigits = JXBrowserHelper.text(browser,"#payment-information .a-color-secondary span");
        CreditCard creditCard = CreditCardUtils.getCreditCard(buyerPanel.getOrder());
        return creditCard.getCardNo().endsWith(lastDigits);
    }



    @Override
    public void execute(Order order) {

    }
}
