package edu.olivet.harvester.finance.service;

import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.finance.model.UnfulfilledOrder;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/12/2018 11:29 AM
 */
public class FinanceSheetService extends SheetAPI {
    public static final String SPREADSHEET_ID = "1gzu0syWFNnDESwNyLQPTq9kk2I_UlwK_8Uo6uVy8IrI";
    private static final String TEMPLATE_SHEET_NAME = "template";

    public void fillInfoToSheet(List<UnfulfilledOrder> records) {
        String sid = Settings.load().getSid();
        SheetProperties sheetProperties = createNewSheetIfNotExisted(SPREADSHEET_ID, sid.toUpperCase(), TEMPLATE_SHEET_NAME);

        String sheetName = sheetProperties.getTitle();

        //find existed
        List<String> existed = existedOrderNumbers(sheetName, SPREADSHEET_ID);
        if (CollectionUtils.isNotEmpty(existed)) {
            records.removeIf(it -> existed.contains(it.getOrderNumber()));
        }

        if (CollectionUtils.isEmpty(records)) {
            return;
        }

        List<List<Object>> values = convertRecordsToRangeValues(records);
        try {
            this.spreadsheetValuesAppend(SPREADSHEET_ID, sheetName, new ValueRange().setValues(values));
        } catch (BusinessException e) {
            throw new BusinessException(e);
        }
    }


    public List<String> existedOrderNumbers(String sheetName, String spreadsheetId) {
        List<String> orderIds = new ArrayList<>();

        if (StringUtils.isBlank(spreadsheetId)) {
            return orderIds;
        }

        try {
            List<String> ranges = Lists.newArrayList(sheetName + "!B2:B");
            List<ValueRange> valueRanges;
            try {
                valueRanges = batchGetSpreadsheetValues(spreadsheetId, ranges);
            } catch (BusinessException e) {
                //LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, ranges, e);
                throw e;
            }

            for (ValueRange valueRange : valueRanges) {
                List<List<Object>> rows = valueRange.getValues();
                if (CollectionUtils.isEmpty(rows)) {
                    continue;
                }
                for (List<Object> objects : rows) {
                    if (CollectionUtils.isEmpty(objects)) {
                        //LOGGER.info("Row {} is empty for {} {}.", row, spreadTitle, sheetName);
                        continue;
                    }
                    String orderId = objects.get(0).toString();
                    if (StringUtils.isBlank(orderId) || !Regex.AMAZON_ORDER_NUMBER.isMatched(orderId)) {
                        continue;
                    }

                    orderIds.add(orderId);
                }
            }
        } catch (Exception e) {
            //LOGGER.error("", e);
        }
        return orderIds;
    }

    public List<List<Object>> convertRecordsToRangeValues(List<UnfulfilledOrder> records) {
        List<List<Object>> values = new ArrayList<>();
        records.forEach(order -> {
            Object[] row = new String[] {
                    order.getOrderDate(),
                    order.getOrderNumber(),
                    StringUtils.isBlank(order.getSku()) ? StringUtils.EMPTY : order.getSku(),
                    StringUtils.isBlank(order.getOrderDescription()) ? StringUtils.EMPTY : order.getOrderDescription(),
                    StringUtils.isBlank(order.getPrice()) ? StringUtils.EMPTY : order.getPrice(),
                    StringUtils.isBlank(order.getShipping()) ? StringUtils.EMPTY : order.getShipping(),
                    StringUtils.isBlank(order.getQty()) ? StringUtils.EMPTY : order.getQty(),
                    StringUtils.isBlank(order.getTotalPrice()) ? StringUtils.EMPTY : order.getTotalPrice(),
                    StringUtils.isBlank(order.getEstimatedCost()) ? StringUtils.EMPTY : order.getEstimatedCost(),
                    StringUtils.isBlank(order.getFulfilledAmount()) ? StringUtils.EMPTY : order.getFulfilledAmount(),
                    StringUtils.isBlank(order.getFulfilledDate()) ? StringUtils.EMPTY : order.getFulfilledDate(),
                    StringUtils.isBlank(order.getRefundAmount()) ? StringUtils.EMPTY : order.getRefundAmount(),
                    StringUtils.isBlank(order.getRefundDate()) ? StringUtils.EMPTY : order.getRefundDate(),
                    order.getSalesChannel(),
                    StringUtils.isBlank(order.getRemark()) ? StringUtils.EMPTY : order.getRemark(),
            };
            values.add(Arrays.asList(row));
        });

        return values;
    }
}
