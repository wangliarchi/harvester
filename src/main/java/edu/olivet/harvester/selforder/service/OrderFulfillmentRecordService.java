package edu.olivet.harvester.selforder.service;

import com.google.inject.Inject;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.harvester.selforder.model.OrderFulfillmentRecord;
import edu.olivet.harvester.selforder.model.SelfOrder;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/2/2018 11:56 AM
 */
public class OrderFulfillmentRecordService {
    @Inject DBManager dbManager;

    public OrderFulfillmentRecord save(SelfOrder selfOrder) {
        OrderFulfillmentRecord record = new OrderFulfillmentRecord();
        record.setSheetName(selfOrder.sheetName);
        record.setSeller(selfOrder.getOwnerAccountStoreName());
        record.setSellerCode(selfOrder.getOwnerAccountCode());
        record.setSellerId(selfOrder.getOwnerAccountSellerId());
        record.setAsin(selfOrder.asin);
        record.setBuyerAccount(selfOrder.buyerAccountEmail);
        record.setOrderNumber(selfOrder.orderNumber);
        record.setCost(selfOrder.cost);
        record.setPrimoCode(selfOrder.promoCode);
        record.setFulfilledAddress(selfOrder.recipientName + "\n" + selfOrder.shippingAddress1 + "\n" +
                selfOrder.shippingAddress2 + "\n" + selfOrder.shippingCity + "\n" + selfOrder.shippingState + ", " + selfOrder.shippingZipCode + "\n"
                + selfOrder.shippingCountry);
        record.setFulfillDate(new Date());
        record.setId(DigestUtils.sha256Hex(selfOrder.toString()));
        record.setCountry(selfOrder.country);

        dbManager.insert(record, OrderFulfillmentRecord.class);
        return record;
    }
}
