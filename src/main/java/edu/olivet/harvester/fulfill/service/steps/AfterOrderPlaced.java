package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import com.mchange.lang.FloatUtils;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.model.page.checkout.OrderPlacedSuccessPage;
import edu.olivet.harvester.fulfill.service.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.logger.SuccessLogger;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.common.model.Order;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
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


    @Repeat(expectedExceptions = BusinessException.class)
    private void saveToDB(Order order) {
        OrderFulfillmentRecord record = new OrderFulfillmentRecord();

        record.setId(DigestUtils.sha256Hex(order.order_id + order.sku + order.row + order.remark));
        record.setOrderId(order.order_id);
        record.setSku(order.sku);
        record.setPurchaseDate(order.purchase_date);
        record.setSheetName(order.sheetName);
        record.setSpreadsheetId(order.getSpreadsheetId());
        record.setIsbn(order.isbn);
        record.setSeller(order.seller);
        record.setSellerId(order.seller_id);
        record.setSellerPrice(order.seller_price);
        record.setCondition(order.condition);
        record.setCharacter(order.character);
        if (order.orderTotalCost != null) {
            record.setCost(order.orderTotalCost.toUSDAmount().toPlainString());
        } else {
            record.setCost(order.cost);
        }
        record.setOrderNumber(order.order_number);
        record.setBuyerAccount(order.account);
        record.setLastCode(order.last_code);
        record.setRemark(order.remark);
        record.setQuantityPurchased(IntegerUtils.parseInt(order.quantity_purchased, 1));
        record.setQuantityBought(IntegerUtils.parseInt(order.quantity_fulfilled, 1));
        record.setShippingAddress(Address.loadFromOrder(order).toString());
        try {
            record.setFulfilledAddress(order.getFulfilledAddress().toString());
        } catch (Exception e) {
            record.setFulfilledAddress("");
        }
        if (StringUtils.isNotBlank(order.getFulfilledASIN())) {
            record.setFulfilledASIN(order.getFulfilledASIN());
        } else {
            record.setFulfilledASIN("");
        }
        record.setFulfillDate(new Date());

        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        dbManager.insert(record, OrderFulfillmentRecord.class);
    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void updateSpending(String spreadsheetId, Order order) {
        dailyBudgetHelper.addSpending(spreadsheetId, new Date(), FloatUtils.parseFloat(order.cost, 0));
    }

}
