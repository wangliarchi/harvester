package edu.olivet.harvester.fulfill.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.inject.Inject;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Lists;

import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 2:03 PM
 */
public class SheetService extends SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);
    @Inject
    OrderHelper orderHelper;

    public void fillFulfillmentOrderInfo(String spreadsheetId, Order order) {
        List<ValueRange> dateToUpdate = new ArrayList<>();
        String range = String.format("%s!AC%d:AF%d", order.sheetName, order.row, order.row);

        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList("finish")))
                .setRange(String.format("%s!A%d", order.sheetName, order.row));
        dateToUpdate.add(statusData);

        ValueRange rowData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.cost, order.order_number, order.account, order.last_code)))
                .setRange(range);
        dateToUpdate.add(rowData);

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }


    public void fillUnsuccessfulMsg(String spreadsheetId, Order order, String msg) {



        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.remark + " " + msg)))
                .setRange(String.format("%s!S%d", order.sheetName, order.row));
        dateToUpdate.add(statusData);

        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order error msg {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }


    public Map<String, List<String>> updateStatus(String spreadsheetId, List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }

        long start = System.currentTimeMillis();

        int total = orders.size();
        int validCount = 0;

        List<ValueRange> dateToUpdate = new ArrayList<>();

        Map<Integer, String> statusToUpdate = new HashMap<>();
        for (Order order : orders) {
            //update status cell
            String status = orderHelper.determineStatus(order);
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


}
