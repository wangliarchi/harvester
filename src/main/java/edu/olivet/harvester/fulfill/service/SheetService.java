package edu.olivet.harvester.fulfill.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.OrderEnums.Status;
import edu.olivet.harvester.fulfill.utils.FwdAddressUtils;
import edu.olivet.harvester.fulfill.utils.OrderStatusUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderColor;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.common.RandomUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 2:03 PM
 */
public class SheetService extends SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);

    @Inject private
    AppScript appScript;

    @Repeat(times = 5, expectedExceptions = BusinessException.class)
    public void fillFulfillmentOrderInfo(String spreadsheetId, Order order) {
        //need to relocate order row on google sheet, as it may be arranged during order fulfillment process.
        //int row = locateOrder(order);
        //reloaded before order placed, dont need to reload again;
        int row = order.row;
        List<ValueRange> dateToUpdate = new ArrayList<>();

        //status
        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList("finish")))
                .setRange(String.format("%s!A%d", order.sheetName, row));
        dateToUpdate.add(statusData);

        //fulfilled order info
        String range = String.format("%s!AC%d:AF%d", order.sheetName, row, row);

        //paste usd for all
        if (order.orderTotalCost != null) {
            order.cost = order.orderTotalCost.toUSDAmount().toPlainString();
        }
        ValueRange rowData = new ValueRange()
                .setValues(Collections.singletonList(Lists.newArrayList(order.cost, order.order_number, order.account, order.last_code)))
                .setRange(range);
        dateToUpdate.add(rowData);

        //remark
        if (StringUtils.isBlank(order.quantity_fulfilled)) {
            order.quantity_fulfilled = order.quantity_purchased;
        }

        if (!order.quantity_purchased.equals(order.quantity_fulfilled)) {
            OrderHelper.addQuantityChangeRemark(order, order.quantity_fulfilled);
        }

        ValueRange remarkData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.remark)))
                .setRange(String.format("%s!S%d", order.sheetName, row));
        dateToUpdate.add(remarkData);

        //url
        ValueRange urlData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.url)))
                .setRange(String.format("%s!P%d", order.sheetName, row));
        dateToUpdate.add(urlData);

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            updateFulfilledOrderBackgroundColor(spreadsheetId, order, row);
        } catch (Exception e) {
            LOGGER.error("Fail to update row background {} {} {}", order.order_id, order.sheetName, row);
        }

    }


    private void updateFulfilledOrderBackgroundColor(String spreadsheetId, Order order, int row) {
        if (Remark.quantityDiffered(order.remark)) {
            appScript.markColor(spreadsheetId, order.sheetName, row, OrderColor.InvalidByCode);
        } else {
            appScript.markColor(spreadsheetId, order.sheetName, row, OrderColor.Finished);
        }
    }

    public void fillUnsuccessfulMsg(String spreadsheetId, Order order, String msg) {
        //need to relocate order row on google sheet, as it may be arranged during order fulfillment process.
        int row = locateOrder(order);
        order.addRemark(msg);
        order.status = Status.Initial.value();
        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.status)))
                .setRange(String.format("%s!A%d", order.sheetName, row));
        ValueRange remarkData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.remark)))
                .setRange(String.format("%s!S%d", order.sheetName, row));
        dateToUpdate.add(statusData);
        dateToUpdate.add(remarkData);

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order error msg {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }

        appScript.markColor(spreadsheetId, order.sheetName, row, OrderColor.InvalidByCode);
    }


    public Map<String, List<String>> updateStatus(String spreadsheetId, List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }


        List<ValueRange> dateToUpdate = new ArrayList<>();

        Map<Integer, String> statusToUpdate = new HashMap<>();
        for (Order order : orders) {
            //update status cell
            String status = OrderStatusUtils.determineStatus(order);
            String randCode = RandomUtils.randomAlphaNumeric(8);
            String url = order.url;
            if (order.purchaseBack() && order.getType() == OrderItemType.PRODUCT) {
                url = FwdAddressUtils.usFwdProductRecipient(order);
            }

            order.last_code = randCode;
            statusToUpdate.put(order.row, status);
            ValueRange rowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(status)))
                    .setRange(order.getSheetName() + "!A" + order.row);
            ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(randCode)))
                    .setRange(order.getSheetName() + "!AF" + order.row);

            ValueRange urlRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(url)))
                    .setRange(order.getSheetName() + "!P" + order.row);


            dateToUpdate.add(rowData);
            dateToUpdate.add(codeRowData);
            dateToUpdate.add(urlRowData);
        }


        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }

        Map<Integer, String> newStatuses = fetchOrderStatus(spreadsheetId, orders);

        List<String> success = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Order order : orders) {
            String statusExpected = statusToUpdate.get(order.row);
            String actualStatus = newStatuses.get(order.row);

            if (StringUtils.equalsIgnoreCase(statusExpected, actualStatus)) {
                success.add("Row " + order.row + " status updated to [" + actualStatus + "] successfully");
            } else {
                failed.add("Row " + order.row + " failed to updated status from " +
                        order.status + " to [" + statusExpected + "], current is [" + actualStatus + "]");
            }
        }
        Map<String, List<String>> results = new HashMap<>();
        results.put("s", success);
        results.put("f", failed);
        return results;
    }

    public Map<Integer, String> fetchOrderStatus(String spreadsheetId, List<Order> orders) {

        List<String> ranges = new ArrayList<>(orders.size());
        orders.forEach(order -> ranges.add(String.format("%s!A%d", order.sheetName, order.row)));
        List<ValueRange> valueRanges = batchGetSpreadsheetValues(spreadsheetId, ranges);

        Map<Integer, String> statusMap = new HashMap<>();
        for (ValueRange valueRange : valueRanges) {
            String range = valueRange.getRange();
            int rowNo = rowNoFromRange(range);
            String status = valueRange.getValues().get(0).get(0).toString();
            statusMap.put(rowNo, status);
        }

        return statusMap;
    }


    public int locateOrder(Order order) {
        //id, sku, seller, price, remark
        Order o = reloadOrder(order);
        return o.row;
    }


    public List<Order> reloadOrders(List<Order> orders) {
        //id, sku, seller, price, remark
        List<Order> allOrders = appScript.readOrders(orders.get(0).spreadsheetId, orders.get(0).sheetName);

        Map<String, List<Order>> orderMap = new HashMap<>();
        allOrders.forEach(it -> {
            List<Order> os = orderMap.getOrDefault(it.order_id, new ArrayList<>());
            os.add(it);
            orderMap.put(it.order_id, os);
        });

        List<Order> reloadedOrders = new ArrayList<>();

        for (Order order : orders) {
            if (!orderMap.containsKey(order.order_id)) {
                LOGGER.info("Cant find order " + order.order_id + "on sheet" + order.sheetName);
                continue;
            }

            List<Order> os = orderMap.get(order.order_id);
            Order reloadedOrder = findOrder(order, os);

            if (reloadedOrder != null) {
                reloadedOrders.add(reloadedOrder);
            }
        }

        return reloadedOrders.stream().distinct().collect(Collectors.toList());
    }

    public Order reloadOrder(Order order) {
        Order reloadedOrder = _reloadOrder(order);
        reloadedOrder.setContext(order.getContext());
        reloadedOrder.setTask(order.getTask());
        order = reloadedOrder;
        return order;
    }

    @Repeat(expectedExceptions = BusinessException.class)
    protected Order _reloadOrder(Order order) {
        //id, sku, seller, price, remark
        List<Order> orders = appScript.readOrders(order.spreadsheetId, order.sheetName);
        List<Order> validOrders = orders.stream().filter(it -> it.equalsSuperLite(order) ||
                (StringUtils.length(order.last_code) == 8 && StringUtils.equalsIgnoreCase(order.last_code, it.last_code))
        ).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validOrders)) {
            LOGGER.error("Cant reload order {} from {} - all orders {} - valid {}", order, order.sheetName, orders, validOrders);
            throw new BusinessException("Cant find order " + order + "on order sheet");
        }


        Order reloadedOrder = findOrder(order, validOrders);

        if (reloadedOrder != null) {
            return reloadedOrder;
        }

        throw new BusinessException("Cant find order " + order + "on order sheet");
    }

    private Order findOrder(Order order, List<Order> os) {
        Order reloadedOrder = null;

        if (os.size() == 1) {
            reloadedOrder = os.get(0);
        }

        //check by last code. when mark status, each row will assign a random code
        if (reloadedOrder == null) {
            if (StringUtils.length(order.last_code) == 8) {
                for (Order o : os) {
                    if (order.last_code.equalsIgnoreCase(o.last_code)) {
                        reloadedOrder = o;
                        break;
                    }
                }
            }
        }

        if (reloadedOrder == null) {
            for (Order o : os) {
                if (o.equalsLite(order) &&
                        StringUtils.equalsAnyIgnoreCase(o.remark, order.remark, order.originalRemark)) {
                    reloadedOrder = o;
                    break;
                }
            }
        }

        if (reloadedOrder == null) {
            for (Order o : os) {
                if (o.equalsLite(order) &&
                        (o.row == order.row || StringUtils.isBlank(order.remark))) {
                    reloadedOrder = o;
                    break;
                }
            }
        }

        if (reloadedOrder != null) {
            reloadedOrder.setContext(order.getContext());
            reloadedOrder.setTask(order.getTask());
        }

        return reloadedOrder;
    }


}
