package edu.olivet.harvester.hunt.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/25/2018 10:32 AM
 */
public class SheetService extends SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);

    public void fillSellerInfo(Order order) {
        List<ValueRange> dataToUpdate = new ArrayList<>();

        String range = String.format("%s!M%d:S%d", order.sheetName, order.row, order.row);
        ValueRange rowData = new ValueRange().setValues(Collections.singletonList(
                Lists.newArrayList(order.seller, order.seller_id, order.seller_price, order.url, order.condition, order.character, order.remark)))
                .setRange(range);
        dataToUpdate.add(rowData);

        try {
            this.batchUpdateValues(order.spreadsheetId, dataToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet status {} - {}", order.spreadsheetId, e);
            throw new BusinessException(e);
        }
    }
}
