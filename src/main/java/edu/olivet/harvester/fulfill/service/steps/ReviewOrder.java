package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewOnePage;
import edu.olivet.harvester.fulfill.model.page.checkout.PaymentMethodOnePage;
import edu.olivet.harvester.fulfill.model.page.checkout.ShippingAddressOnePage;
import edu.olivet.harvester.fulfill.service.AddressValidatorService;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;
import edu.olivet.harvester.fulfill.utils.OrderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class ReviewOrder extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewOrder.class);

    @Inject
    AddressValidatorService addressValidatorService;

    //dispatcher method
    protected void process(FlowState state) {
        OrderReviewOnePage orderReviewOnePage = new OrderReviewOnePage(state.getBuyerPanel());

        if (OrderValidator.needCheck(state.getOrder(), OrderValidator.SkipValidation.Address)) {
            reviewAddress(state);
        }

        reviewPayment(state);


        try {
            orderReviewOnePage.checkTotalCost(state.getOrder());
            LOGGER.info("Passed cost check.");
        } catch (Exception e) {
            LOGGER.error("Failed cost check. ", e);
            throw e;
        }


    }

    @Repeat
    private void reviewAddress(FlowState state) {
        String errorMsg = "";
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            OrderReviewOnePage orderReviewOnePage = new OrderReviewOnePage(state.getBuyerPanel());
            try {
                orderReviewOnePage.reviewShippingAddress(addressValidatorService);
                LOGGER.info("Address passed review");
                return;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                LOGGER.warn("", e.getMessage());
                ShippingAddressOnePage shippingAddressOnePage = new ShippingAddressOnePage(state.getBuyerPanel());
                shippingAddressOnePage.execute(state.getOrder());
                WaitTime.Shortest.execute();
            }

        }

        throw new BusinessException(errorMsg);
    }

    @Repeat
    private void reviewPayment(FlowState state) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            OrderReviewOnePage orderReviewOnePage = new OrderReviewOnePage(state.getBuyerPanel());
            if (orderReviewOnePage.reviewPaymentMethod()) {
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
