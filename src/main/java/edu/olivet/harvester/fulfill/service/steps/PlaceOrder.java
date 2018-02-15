package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutEnum;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewMultiPage;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderReviewOnePage;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 6:49 PM
 */
public class PlaceOrder extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceOrder.class);

    @Inject SheetService sheetService;

    //dispatcher method
    protected void process(FlowState state) {
        //
        Order order = sheetService.reloadOrder(state.getOrder());
        if (!order.fulfillable()) {
            throw new OrderSubmissionException("Order status [" + order.status + "] is not marked for fulfillment.");
        }

        if (stepHelper.detectCurrentPage(state) == CheckoutEnum.CheckoutPage.ShippingMethodOnePage) {
            OrderReviewOnePage orderReviewOnePage = new OrderReviewOnePage(state.getBuyerPanel());
            orderReviewOnePage.placeOrder(state.getOrder());
            return;
        }

        OrderReviewMultiPage orderReviewMultiPage = new OrderReviewMultiPage(state.getBuyerPanel());
        orderReviewMultiPage.placeOrder(state.getOrder());

        //
    }

    @Inject private
    StepHelper stepHelper;

    public Step createDynamicInstance(FlowState state) {
        return stepHelper.detectStep(state);
    }
}
