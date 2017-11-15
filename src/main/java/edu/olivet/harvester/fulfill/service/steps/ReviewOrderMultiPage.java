package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewMultiPage;
import edu.olivet.harvester.fulfill.model.page.checkout.PaymentMethodOnePage;
import edu.olivet.harvester.fulfill.model.page.checkout.ShippingAddressOnePage;
import edu.olivet.harvester.fulfill.service.addressvalidator.USPSAddressValidator;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class ReviewOrderMultiPage extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewOrderMultiPage.class);

    @Inject
    USPSAddressValidator uspsAddressValidator;

    //dispatcher method
    protected void process(FlowState state) {
        OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());

        reviewAddress(state);

        reviewPayment(state);

        try {
            orderReviewMultiPage.checkTotalCost(state.getOrder());
            LOGGER.info("Passed cost check.");
        } catch (Exception e) {
            LOGGER.error("Failed cost check. ", e);
            throw e;
        }

    }


    private void reviewAddress(FlowState state) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
            if (!orderReviewMultiPage.reviewShippingAddress(uspsAddressValidator)) {
                LOGGER.warn("Address did not pass verification. try to enter again.");
                ShippingAddressOnePage shippingAddressOnePage = new ShippingAddressOnePage(state.getBuyerPanel());
                shippingAddressOnePage.execute(state.getOrder());
                WaitTime.Short.execute();


            } else {
                LOGGER.info("Address passed review");
                return;
            }
        }

        throw new BusinessException("Address did not pass verification. try to enter again.");
    }

    @Repeat
    private void reviewPayment(FlowState state) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
            if (orderReviewMultiPage.reviewPaymentMethod()) {
                LOGGER.info("Payment passed review");
                return;
            } else {
                LOGGER.info("Payment did not pass review");
                PaymentMethodOnePage paymentMethodOnePage = new PaymentMethodOnePage(state.getBuyerPanel());
                paymentMethodOnePage.execute(state.getOrder());
                WaitTime.Shortest.execute();

            }
        }

        throw new BusinessException("Payment did not pass review");
    }

    @Inject
    PlaceOrder placeOrder;

    public Step createDynamicInstance(FlowState state) {
        return placeOrder;
    }
}
