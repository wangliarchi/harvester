package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderPlacedSuccessPage;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.common.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:34 PM
 */
public class AfterOrderPlaced extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterOrderPlaced.class);

    @Inject private ErrorAlertService errorAlertService;

    @Override
    protected void process(FlowState state) {
        //navigate to order detail page
        if (SystemSettings.load().isOrderSubmissionDebugModel()) {
            state.getOrder().order_number = "111-1111111-1111111";
            state.getOrder().account = state.getBuyerPanel().getBuyer().getEmail();
        } else {
            try {
                OrderPlacedSuccessPage orderPlacedSuccessPage = new OrderPlacedSuccessPage(state.getBuyerPanel());
                orderPlacedSuccessPage.execute(state.getOrder());
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        if (state.getOrder().selfOrder) {
            return;
        }
        //fill data back to google sheet
        new Thread(() -> {
            try {
                updateInfoToOrderSheet(state.getOrder().getSpreadsheetId(), state.getOrder());
            } catch (Exception e) {
                LOGGER.error("Failed to update order fulfillment info to order update sheet", e);
                errorAlertService.sendMessage("Failed to update order fulfillment info to order update sheet", state.getOrder().toString());
            }
        }).start();

    }

    @Inject private StepHelper stepHelper;
    @Inject private ReadOrderDetails readOrderDetails;

    @Override
    public Step createDynamicInstance(FlowState state) {
        state.setPrevStep(this);
        if (SystemSettings.load().isOrderSubmissionDebugModel()) {
            return readOrderDetails;
        }
        return stepHelper.detectStep(state);
    }

    @Inject private
    SheetService sheetService;

    private void updateInfoToOrderSheet(String spreadsheetId, Order order) {
        order.remark = Remark.removeFailedRemark(order.remark);
        sheetService.fillFulfillmentOrderInfo(spreadsheetId, order);
    }

}
