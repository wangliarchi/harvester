package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.utils.common.DatetimeHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/23/2018 2:21 PM
 */
public class OrderFulfillmentRecordService {
    @Inject private DBManager dbManager;

    public float totalCost(String spreadsheetId, Date date) {
        List<OrderFulfillmentRecord> records = dbManager.query(OrderFulfillmentRecord.class,
                Cnd.where("spreadsheetId", "=", spreadsheetId)
                        .and("fulfillDate", ">=", DatetimeHelper.getStartOfDay(date))
                        .and("fulfillDate", "<=", DatetimeHelper.getEndOfDay(date))
                        .asc("fulfillDate"));

        return (float) records.stream().mapToDouble(it -> Float.parseFloat(it.getCost())).sum();
    }

    public void save(Order order) {
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
        record.setLastCode(StringUtils.isBlank(order.last_code) ? "" : order.last_code);
        record.setRemark(StringUtils.isBlank(order.remark) ? "" : order.remark);
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

        dbManager.insert(record, OrderFulfillmentRecord.class);
    }

    public static void main(String[] args) {
        OrderFulfillmentRecordService orderFulfillmentRecordService = ApplicationContext.getBean(OrderFulfillmentRecordService.class);
        float total = orderFulfillmentRecordService.totalCost("1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI", Dates.parseDate("02/14/2018"));
        System.out.println(total);
    }
}
