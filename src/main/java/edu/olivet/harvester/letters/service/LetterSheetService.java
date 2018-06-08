package edu.olivet.harvester.letters.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 5:16 PM
 */
public class LetterSheetService extends edu.olivet.harvester.fulfill.service.SheetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LetterSheetService.class);

    public void fillSuccessInfo(Order order) {
        String status = order.status;
        order = reloadOrder(order);
        int row = order.row;

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(status)))
                .setRange(String.format("%s!A%d", order.sheetName, row));
        dateToUpdate.add(statusData);

        ValueRange infoData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.order_number + " emailed")))
                .setRange(String.format("%s!AD%d", order.sheetName, row));
        dateToUpdate.add(infoData);

        try {
            this.batchUpdateValues(order.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order error msg {} - {}", order.spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    public void fillRefundSuccessInfo(Order order) {
        String status = order.status;
        order = reloadOrder(order);
        int row = order.row;

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(status)))
                .setRange(String.format("%s!A%d", order.sheetName, row));
        dateToUpdate.add(statusData);

        ValueRange infoData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.order_number + " refunded")))
                .setRange(String.format("%s!AD%d", order.sheetName, row));
        dateToUpdate.add(infoData);

        try {
            this.batchUpdateValues(order.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order error msg {} - {}", order.spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    public void fillFailedInfo(Order order, String msg) {
        order = reloadOrder(order);
        int row = order.row;

        List<ValueRange> dateToUpdate = new ArrayList<>();


        ValueRange statusData = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(order.order_number + " " + msg)))
                .setRange(String.format("%s!AD%d", order.sheetName, row));
        dateToUpdate.add(statusData);

        try {
            this.batchUpdateValues(order.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order error msg {} - {}", order.spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

}
