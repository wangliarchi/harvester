package edu.olivet.harvester.hunt.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderColor;
import edu.olivet.harvester.common.model.OrderEnums.Status;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.common.RandomUtils;
import org.apache.commons.collections4.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/25/2018 10:32 AM
 */
public class SheetService extends edu.olivet.harvester.fulfill.service.SheetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);

    @Inject AppScript appScript;

    public void fillSellerInfo(Order order) {
        int row = locateOrder(order);
        List<ValueRange> dataToUpdate = new ArrayList<>();

        String range = String.format("%s!K%d:S%d", order.sheetName, row, row);
        ValueRange rowData = new ValueRange().setValues(Collections.singletonList(
                Lists.newArrayList(order.isbn_address, order.isbn, order.seller, order.seller_id, order.seller_price, order.url, order.condition, order.character, order.remark)))
                .setRange(range);
        dataToUpdate.add(rowData);

        order.status = Status.Initial.value();
        ValueRange statusRowData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.status)))
                .setRange(String.format("%s!A%d", order.sheetName, row));
        dataToUpdate.add(statusRowData);

        try {
            this.batchUpdateValues(order.spreadsheetId, dataToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", order.spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            //String sheetUrl, String sheetName, int row, OrderColor color
            this.appScript.markColor(order.spreadsheetId, order.sheetName, order.row, OrderColor.CommitSeller);
        } catch (Exception e) {
            LOGGER.error("Fail to mark order background {} - {}", order.spreadsheetId, e);
        }
    }

    public void updateLastCode(String spreadsheetId, List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }


        List<ValueRange> dateToUpdate = new ArrayList<>();

        for (Order order : orders) {
            String randCode = RandomUtils.randomAlphaNumeric(8);
            order.last_code = randCode;
            ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(randCode)))
                    .setRange(order.getSheetName() + "!AF" + order.row);

            dateToUpdate.add(codeRowData);
        }


        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

}
