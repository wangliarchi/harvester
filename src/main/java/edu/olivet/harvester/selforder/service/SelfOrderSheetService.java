package edu.olivet.harvester.selforder.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.OrderEnums.OrderColor;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.service.SelfOrderService.OrderAction;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.common.RandomUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/1/2018 10:47 AM
 */
public class SelfOrderSheetService extends SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfOrderSheetService.class);

    @Inject SelfOrderService selfOrderService;
    @Inject AppScript appScript;

    @Repeat(times = 5, expectedExceptions = BusinessException.class)
    public void fillFulfillmentOrderInfo(SelfOrder selfOrder) {
        SelfOrder reloadedOrder = reloadOrder(selfOrder);
        selfOrder.row = reloadedOrder.row;
        List<ValueRange> dateToUpdate = new ArrayList<>();

        //fulfilled order info
        String range = String.format("%s!H%d:J%d", selfOrder.sheetName, selfOrder.row, selfOrder.row);

        ValueRange rowData = new ValueRange()
                .setValues(Collections.singletonList(Lists.newArrayList(selfOrder.buyerAccountEmail, selfOrder.orderNumber, selfOrder.cost)))
                .setRange(range);
        dateToUpdate.add(rowData);

        try {
            this.batchUpdateValues(selfOrder.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", selfOrder.spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            updateFulfilledOrderBackgroundColor(selfOrder, true);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class)
    public void fillSellerId(SelfOrder selfOrder) {
        int row = selfOrder.row;
        try {
            SelfOrder reloadedOrder = reloadOrder(selfOrder);
            row = reloadedOrder.row;
        } catch (Exception e) {
            //
        }
        List<ValueRange> dateToUpdate = new ArrayList<>();

        //fulfilled order info
        String range = String.format("%s!B%d:C%d", selfOrder.sheetName, row, row);

        ValueRange rowData = new ValueRange()
                .setValues(Collections.singletonList(Lists.newArrayList(selfOrder.ownerAccountStoreName, selfOrder.ownerAccountSellerId)))
                .setRange(range);
        dateToUpdate.add(rowData);

        try {
            this.batchUpdateValues(selfOrder.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", selfOrder.spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    @Repeat(times = 5, expectedExceptions = BusinessException.class)
    public void fillFailedOrderInfo(SelfOrder selfOrder, String msg) {
        SelfOrder reloadedOrder = reloadOrder(selfOrder);
        selfOrder.row = reloadedOrder.row;

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(msg)))
                .setRange(selfOrder.getSheetName() + "!I" + selfOrder.row);
        dateToUpdate.add(codeRowData);

        try {
            this.batchUpdateValues(selfOrder.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update unique code {} - {}", selfOrder.spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            updateFulfilledOrderBackgroundColor(selfOrder, false);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void updateFulfilledOrderBackgroundColor(SelfOrder selfOrder, boolean success) {
        if (success) {
            appScript.markColor(selfOrder.spreadsheetId, selfOrder.sheetName, selfOrder.row, OrderColor.Finished);
        } else {
            appScript.markColor(selfOrder.spreadsheetId, selfOrder.sheetName, selfOrder.row, OrderColor.InvalidByCode);
        }
    }

    public void updateUniqueCode(String spreadsheetId, List<SelfOrder> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }

        List<ValueRange> dateToUpdate = new ArrayList<>();
        for (SelfOrder order : orders) {
            String randCode = RandomUtils.randomAlphaNumeric(8);
            order.uniqueCode = randCode;

            ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(randCode)))
                    .setRange(order.getSheetName() + "!U" + order.row);
            dateToUpdate.add(codeRowData);
        }

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update unique code {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }


    public SelfOrder reloadOrder(SelfOrder order) {
        SelfOrder reloadedOrder = _reloadOrder(order);
        reloadedOrder.orderNumber = order.orderNumber;
        reloadedOrder.buyerAccountEmail = order.buyerAccountEmail;
        reloadedOrder.cost = order.cost;

        return reloadedOrder;
    }

    @Repeat(expectedExceptions = BusinessException.class)
    protected SelfOrder _reloadOrder(SelfOrder order) {
        //id, sku, seller, price, remark
        List<SelfOrder> orders = selfOrderService.fetchSelfOrders(order.spreadsheetId, order.sheetName, OrderAction.All);
        List<SelfOrder> validOrders = orders.stream().filter(it -> it.equalsSuperLite(order) ||
                (StringUtils.isNotBlank(order.uniqueCode) && StringUtils.equalsIgnoreCase(order.uniqueCode, it.uniqueCode))
        ).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validOrders)) {
            LOGGER.error("Cant reload order {} from {} - all orders {} - valid {}", order, order.sheetName, orders, validOrders);
            throw new BusinessException("Cant find order " + order + "on order sheet");
        }


        SelfOrder reloadedOrder = findOrder(order, validOrders);

        if (reloadedOrder != null) {
            return reloadedOrder;
        }

        throw new BusinessException("Cant find self order " + order + "on order sheet");
    }

    private SelfOrder findOrder(SelfOrder order, List<SelfOrder> os) {
        if (os.size() == 1) {
            return os.get(0);
        }

        //check by last code. when mark status, each row will assign a random code
        if (StringUtils.isNotBlank(order.uniqueCode)) {
            for (SelfOrder o : os) {
                if (order.uniqueCode.equalsIgnoreCase(o.uniqueCode)) {
                    return o;
                }
            }
        }


        for (SelfOrder o : os) {
            if (o.equalsSuperLite(order) && o.row == order.row) {
                return o;
            }
        }


        return null;
    }

}
