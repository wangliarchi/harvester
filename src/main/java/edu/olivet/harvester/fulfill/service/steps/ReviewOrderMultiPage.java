package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewMultiPage;
import edu.olivet.harvester.fulfill.model.page.checkout.PaymentMethodMultiPage;
import edu.olivet.harvester.fulfill.service.AddressValidatorService;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 2:55 PM
 */
public class ReviewOrderMultiPage extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewOrderMultiPage.class);

    @Inject private
    AddressValidatorService addressValidator;

    //dispatcher method
    @SuppressWarnings("Duplicates")
    protected void process(FlowState state) {
        OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());

        if (OrderValidator.needCheck(state.getOrder(), OrderValidator.SkipValidation.Address)) {
            reviewAddress(state);
        }

        reviewPayment(state);

        try {
            orderReviewMultiPage.checkShippingCost(state.getOrder());
            LOGGER.info("Passed shipping cost check.");
        } catch (Exception e) {
            LOGGER.error("Failed shipping cost check. ", e);
            throw new OrderSubmissionException(e);
        }

        try {
            orderReviewMultiPage.checkTotalCost(state.getOrder());
            LOGGER.info("Passed cost check.");
        } catch (Exception e) {
            LOGGER.error("Failed cost check. ", e);
            throw e;
        }




    }


    private void reviewAddress(FlowState state) {

        String errorMsg = "";
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
            try {
                orderReviewMultiPage.reviewShippingAddress(addressValidator);
                LOGGER.info("Address passed review");
                return;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                LOGGER.warn("", e.getMessage());
                orderReviewMultiPage.changeShippingAddress();
                WaitTime.Shortest.execute();
            }

        }

        throw new BusinessException(errorMsg);


    }

    void reviewPayment(FlowState state) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
            if (orderReviewMultiPage.reviewPaymentMethod()) {
                LOGGER.info("Payment passed review");
                return;
            } else {
                LOGGER.info("Payment did not pass review");
                orderReviewMultiPage.changePaymentMethod();
                WaitTime.Shortest.execute();

                PaymentMethodMultiPage paymentMethodMultiPage = new PaymentMethodMultiPage(state.getBuyerPanel());
                paymentMethodMultiPage.execute(state.getOrder());
            }
        }

        throw new BusinessException("Payment did not pass review");
    }

    @Inject private
    PlaceOrder placeOrder;

    public Step createDynamicInstance(FlowState state) {
        return placeOrder;
    }
}
