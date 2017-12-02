package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import com.mchange.lang.FloatUtils;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.model.page.checkout.CheckoutEnum.CheckoutPage;
import edu.olivet.harvester.fulfill.model.page.checkout.PlacedOrderDetailPage;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;
import edu.olivet.harvester.fulfill.service.DailyBudgetHelper;
import edu.olivet.harvester.logger.SuccessLogger;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:34 PM
 */
public class ReadOrderDetails extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadOrderDetails.class);
    @Inject
    SheetService sheetService;
    @Inject
    DailyBudgetHelper dailyBudgetHelper;

    @Override
    protected void process(FlowState state) {
        Order order = state.getOrder();
        order.remark = Remark.removeFailedRemark(order.remark);

        try {
            readOrderInfo(state);
        } catch (Exception e) {
            LOGGER.error("Failed to read placed order info.", e);
            //return;
        }

        RuntimeSettings settings = RuntimeSettings.load();
        //fill data back to google sheet
        try {
            updateInfoToOrderSheet(settings.getSpreadsheetId(), order);
        } catch (Exception e) {
            LOGGER.error("Failed to update order fulfillment info to order update sheet", e);
        }
        try {
            saveToDB(order);
        } catch (Exception e) {
            LOGGER.error("Failed to save order fulfillment info into database.", e);
        }

        try {
            updateSpending(settings.getSpreadsheetId(), order);
        } catch (Exception e) {
            LOGGER.error("Failed to update spending.", e);
        }

        try {
            SuccessLogger.log(order);
        } catch (Exception e) {
            //ignore
        }
    }

    @Inject StepHelper stepHelper;

    @Repeat(expectedExceptions = BusinessException.class)
    private void readOrderInfo(FlowState state) {
        if (stepHelper.detectCurrentPage(state) == CheckoutPage.LoginPage) {
            LoginPage loginPage = new LoginPage(state.getBuyerPanel());
            loginPage.execute(state.getOrder());
        }

        //read data from order detail page to order object
        PlacedOrderDetailPage placedOrderDetailPage = new PlacedOrderDetailPage(state.getBuyerPanel());
        placedOrderDetailPage.execute(state.getOrder());
    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void updateInfoToOrderSheet(String spreadsheetId, Order order) {
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
        record.setSpreadsheetId(RuntimeSettings.load().getSpreadsheetId());
        record.setIsbn(order.isbn);
        record.setSeller(order.seller);
        record.setSellerId(order.seller_id);
        record.setSellerPrice(order.seller_price);
        record.setCondition(order.condition);
        record.setCharacter(order.character);
        record.setCost(order.cost);
        record.setOrderNumber(order.order_number);
        record.setBuyerAccount(order.account);
        record.setLastCode(order.last_code);
        record.setRemark(order.remark);
        record.setQuantityPurchased(IntegerUtils.parseInt(order.quantity_purchased, 1));
        record.setQuantityBought(IntegerUtils.parseInt(order.quantity_fulfilled, 1));
        record.setShippingAddress(Address.loadFromOrder(order).toString());
        record.setFulfilledAddress(order.getFulfilledAddress().toString());
        record.setFulfilledASIN(order.getFulfilledASIN());
        record.setFulfillDate(new Date());

        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        dbManager.insert(record, OrderFulfillmentRecord.class);


    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void updateSpending(String spreadsheetId, Order order) {
        dailyBudgetHelper.addSpending(spreadsheetId, new Date(), FloatUtils.parseFloat(order.cost, 0));
    }

    @Override
    public Step createDynamicInstance(FlowState state) {
        return null;
    }


}
