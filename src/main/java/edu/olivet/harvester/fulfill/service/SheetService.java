package edu.olivet.harvester.fulfill.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Or;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.OrderStatusUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 2:03 PM
 */
public class SheetService extends SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);

    @Inject
    AppScript appScript;

    public void fillFulfillmentOrderInfo(String spreadsheetId, Order order) {
        //need to relocate order row on google sheet, as it may be arranged during order fulfillment process.
        int row = locateOrder(order);

        List<ValueRange> dateToUpdate = new ArrayList<>();

        //status
        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList("finish")))
                .setRange(String.format("%s!A%d", order.sheetName, row));
        dateToUpdate.add(statusData);

        //fulfilled order info
        String range = String.format("%s!AC%d:AF%d", order.sheetName, row, row);
        ValueRange rowData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.cost, order.order_number, order.account, order.last_code)))
                .setRange(range);
        dateToUpdate.add(rowData);

        //remark
        ValueRange remarkData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.remark)))
                .setRange(String.format("%s!S%d", order.sheetName, row));
        dateToUpdate.add(remarkData);

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }


    public void fillUnsuccessfulMsg(String spreadsheetId, Order order, String msg) {
        //need to relocate order row on google sheet, as it may be arranged during order fulfillment process.
        int row = locateOrder(order);

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.remark + " " + msg)))
                .setRange(String.format("%s!S%d", order.sheetName, row));
        dateToUpdate.add(statusData);

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order error msg {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }


    @Inject
    OrderStatusUtils orderStatusUtils;

    public Map<String, List<String>> updateStatus(String spreadsheetId, List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }


        List<ValueRange> dateToUpdate = new ArrayList<>();

        Map<Integer, String> statusToUpdate = new HashMap<>();
        for (Order order : orders) {
            //update status cell
            String status = orderStatusUtils.determineStatus(order);
            statusToUpdate.put(order.row, status);
            ValueRange rowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(status)))
                    .setRange(order.getSheetName() + "!A" + order.row);
            dateToUpdate.add(rowData);
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
                success.add("Row " + order.row + " status updated to " + actualStatus + " successfully");
            } else {
                failed.add("Row " + order.row + " failed to updated status from " + order.status + " to " + statusExpected + ", current is " + actualStatus);
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
        List<ValueRange> valueRanges = bactchGetSpreadsheetValues(spreadsheetId, ranges);

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
        RuntimeSettings settings = RuntimeSettings.load();
        List<Order> orders = appScript.readOrders(settings);
        for(Order o : orders) {
            if(order.equalsLite(o)) {
                return o.row;
            }
        }
        throw new BusinessException("Cant find order on order " + order + " sheet");
    }




}
