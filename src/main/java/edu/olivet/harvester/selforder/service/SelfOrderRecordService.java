package edu.olivet.harvester.selforder.service;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.OrderEnums.OrderColor;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.model.SelfOrderRecord;
import edu.olivet.harvester.selforder.utils.SelfOrderRecordHelper;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.RandomUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/10/2018 11:59 AM
 */
public class SelfOrderRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfOrderRecordService.class);

    @Inject SelfOrderSheetService selfOrderSheetService;
    @Inject SelfOrderRecordHelper selfOrderRecordHelper;
    @Inject AppScript appScript;

    public List<SelfOrderRecord> getRecordToPostFeedbacks() {
        List<SelfOrderRecord> records = getRecords();
        records.removeIf(it -> StringUtils.isBlank(it.feedback) || it.feedbackPosted());
        return records;
    }

    public List<SelfOrderRecord> getRecordWithFeedbacks() {
        List<SelfOrderRecord> records = getRecords();
        records.removeIf(it -> StringUtils.isBlank(it.feedback) || !it.feedbackPosted());
        return records;
    }

    public List<SelfOrderRecord> getRecords() {
        String sid = Settings.load().getSid();
        String spreadsheetId = SystemSettings.load().getSelfOrderStatsSpreadsheetId();
        return getRecords(sid, spreadsheetId);
    }

    public List<SelfOrderRecord> getRecords(String sid, String spreadsheetId) {
        Spreadsheet spreadsheet = selfOrderSheetService.getSpreadsheet(spreadsheetId);
        List<String> sheetNames = new ArrayList<>();
        //save sheet properties to cache
        spreadsheet.getSheets().forEach(sheet -> sheetNames.add(sheet.getProperties().getTitle()));

        //
        LOGGER.info("{}下面总共找到{}个Sheet, {}",
                spreadsheet.getProperties().getTitle(), sheetNames.size(), sheetNames.toString());

        //LOGGER.info("{}下面有{}个Sheet待处理, {}", spreadsheet.getProperties().getTitle(), sheetNames.size(), sheetNames.toString());

        if (sheetNames.size() == 0) {
            throw new BusinessException("No worksheets found.");
        }

        List<String> a1Notations = sheetNames.stream().map(it -> it + "!A1:AZ").collect(Collectors.toList());


        List<ValueRange> valueRanges;
        try {
            valueRanges = selfOrderSheetService.batchGetSpreadsheetValues(spreadsheetId, a1Notations);
        } catch (BusinessException e) {
            LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, a1Notations, e);
            throw e;
        }

        List<SelfOrderRecord> records = new ArrayList<>();

        //loop by sheet
        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> rows = valueRange.getValues();
            String a1Notation = valueRange.getRange();
            LOGGER.warn("{}->{}", spreadsheetId, StringUtils.defaultString(a1Notation, "!No A1notation!"));
            if (CollectionUtils.isEmpty(rows)) {
                LOGGER.warn("{}->{}读取不到任何有效数据", spreadsheetId, StringUtils.defaultString(a1Notation, "!No A1notation!"));
                continue;
            }
            int start = a1Notation.contains("'") ? 1 : 0;
            String sheetName = a1Notation.substring(start, a1Notation.indexOf("!") - start);
            int starRow = 1;
            List<SelfOrderRecord> recordsForSheet = new ArrayList<>();
            int row = 0;
            for (List<Object> objects : rows) {
                row++;
                if (row <= starRow) {
                    continue;
                }
                if (CollectionUtils.isEmpty(objects)) {
                    //LOGGER.info("Row {} is empty for {} {}.", row, spreadTitle, sheetName);
                    continue;
                }
                if (StringUtils.isBlank(objects.get(0).toString())) {
                    continue;
                }
                List<String> columns = new ArrayList<>();
                objects.forEach(value -> columns.add(value.toString()));

                SelfOrderRecord record = new SelfOrderRecord();
                int maxCol = columns.size();
                for (int j = 1; j <= maxCol; j++) {
                    try {
                        selfOrderRecordHelper.setColumnValue(j, columns.get(j - 1), record);
                    } catch (Exception e) {
                        //
                    }
                }

                if (StringUtils.isBlank(record.orderNumber) || !Regex.AMAZON_ORDER_NUMBER.isMatched(record.orderNumber)) {
                    continue;
                }

                if (!sid.equalsIgnoreCase(record.buyerAccountCode)) {
                    continue;
                }

                record.setRow(row);
                record.setSpreadsheetId(spreadsheetId);
                record.setSheetName(sheetName);
                recordsForSheet.add(record);
            }

            LOGGER.info("{}->{} found {} orders", spreadsheetId, sheetName, recordsForSheet.size());

            records.addAll(recordsForSheet);
        }

        return records;
    }

    public List<String> existedOrderNumbers(String sheetName) {
        String spreadsheetId = SystemSettings.load().getSelfOrderStatsSpreadsheetId();
        return existedOrderNumbers(sheetName, spreadsheetId);
    }

    public List<String> existedOrderNumbers(String sheetName, String spreadsheetId) {
        List<String> orderIds = new ArrayList<>();

        if (StringUtils.isBlank(spreadsheetId)) {
            return orderIds;
        }

        try {
            List<String> ranges = Lists.newArrayList(sheetName + "!I2:I");
            List<ValueRange> valueRanges;
            try {
                valueRanges = selfOrderSheetService.batchGetSpreadsheetValues(spreadsheetId, ranges);
            } catch (BusinessException e) {
                LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, ranges, e);
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
            LOGGER.error("", e);
        }
        return orderIds;
    }


    public void updateUniqueCode(String spreadsheetId, List<SelfOrderRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }

        List<ValueRange> dateToUpdate = new ArrayList<>();
        for (SelfOrderRecord order : records) {
            String randCode = RandomUtils.randomAlphaNumeric(8);
            order.uniqueCode = randCode;

            ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(randCode)))
                    .setRange(order.getSheetName() + "!M" + order.row);
            dateToUpdate.add(codeRowData);
        }

        try {
            selfOrderSheetService.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update unique code {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    @Repeat(times = 5, expectedExceptions = BusinessException.class)
    public void fillFailedOrderInfo(SelfOrderRecord record, String msg) {

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(msg)))
                .setRange(record.getSheetName() + "!K" + record.row);
        dateToUpdate.add(codeRowData);

        try {
            selfOrderSheetService.batchUpdateValues(record.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update unique code {} - {}", record.spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            updateFulfilledOrderBackgroundColor(record, false);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    @Repeat(times = 5, expectedExceptions = BusinessException.class)
    public void fillSuccessInfo(SelfOrderRecord record) {
        String msg = "Yes";
        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange codeRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(msg)))
                .setRange(record.getSheetName() + "!K" + record.row);
        dateToUpdate.add(codeRowData);

        ValueRange dateRowData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(Dates.today())))
                .setRange(record.getSheetName() + "!M" + record.row);
        dateToUpdate.add(dateRowData);

        try {
            selfOrderSheetService.batchUpdateValues(record.spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update unique code {} - {}", record.spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            updateFulfilledOrderBackgroundColor(record, true);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void updateFulfilledOrderBackgroundColor(SelfOrderRecord record, boolean success) {
        if (success) {
            appScript.markColor(record.spreadsheetId, record.sheetName, record.row, OrderColor.HighPriority);
        } else {
            appScript.markColor(record.spreadsheetId, record.sheetName, record.row, OrderColor.InvalidByCode);
        }
    }

    public static void main(String[] args) {
        SelfOrderRecordService selfOrderRecordService = ApplicationContext.getBean(SelfOrderRecordService.class);

        List<String> orderIds = selfOrderRecordService.existedOrderNumbers("702US");
        System.out.println(orderIds);

        List<SelfOrderRecord> records = selfOrderRecordService.getRecordToPostFeedbacks();
        System.out.println(records);
        System.exit(0);
    }
}
