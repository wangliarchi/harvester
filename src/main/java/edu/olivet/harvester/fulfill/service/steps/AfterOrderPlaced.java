package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderPlacedSuccessPage;
import edu.olivet.harvester.fulfill.service.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.service.OrderFulfillmentRecordService;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.logger.SuccessLogger;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.common.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:34 PM
 */
public class AfterOrderPlaced extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterOrderPlaced.class);

    @Inject private ErrorAlertService errorAlertService;
    @Inject private DailyBudgetHelper dailyBudgetHelper;

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
            LOGGER.info("self order {}, don't need to update spreadsheet.");
            return;
        }

        Order order = state.getOrder();
        //fill data back to google sheet
        new Thread(() -> {
            try {
                updateInfoToOrderSheet(state.getOrder().getSpreadsheetId(), order);
                LOGGER.info("{} order sheet info updated - {} {} {}", order.order_id, order.order_number, order.quantity_fulfilled, order.remark);
            } catch (Exception e) {
                LOGGER.error("Failed to update order fulfillment info to order update sheet", e);
                errorAlertService.sendMessage("Failed to update order fulfillment info to order update sheet", order.toString());
            }
        }).start();

        try {
            saveToDB(order);
        } catch (Exception e) {
            LOGGER.error("Failed to save order fulfillment info into database.", e);
        }

        try {
            SuccessLogger.log(order);
        } catch (Exception e) {
            //ignore
        }

        new Thread(() -> {
            try {
                updateSpending(order.getSpreadsheetId(), order);
            } catch (Exception e) {
                LOGGER.error("Failed to update spending.", e);
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


    @Inject OrderFulfillmentRecordService orderFulfillmentRecordService;
    @Repeat(expectedExceptions = BusinessException.class)
    private void saveToDB(Order order) {
        orderFulfillmentRecordService.save(order);
    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void updateSpending(String spreadsheetId, Order order) {
        dailyBudgetHelper.addSpending(spreadsheetId, new Date(), FloatUtils.parseFloat(order.cost, 0));
    }

}
