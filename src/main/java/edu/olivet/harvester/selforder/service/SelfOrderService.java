package edu.olivet.harvester.selforder.service;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.utils.SelfOrderHelper;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/2/2018 6:04 AM
 */
public class SelfOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfOrderService.class);

    @Inject SelfOrderSheetService selfOrderSheetService;
    @Inject SelfOrderHelper selfOrderHelper;

    public enum OrderAction {
        Process,
        AddProduct,
        All
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, Date minDate) {

        String sid = Settings.load().getSid();
        return fetchSelfOrders(spreadsheetId, minDate, sid);
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, Date minDate, String sid) {
        Range<Date> dateRange = Range.between(minDate, new Date());
        return fetchSelfOrders(spreadsheetId, dateRange, sid);
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, Range<Date> dateRange, String sid) {
        final long start = System.currentTimeMillis();
        Spreadsheet spreadsheet = selfOrderSheetService.getSpreadsheet(spreadsheetId);
        String spreadTitle = spreadsheet.getProperties().getTitle();

        List<String> sheetNames = new ArrayList<>();
        //save sheet properties to cache
        spreadsheet.getSheets().forEach(sheet -> sheetNames.add(sheet.getProperties().getTitle()));

        sheetNames.removeIf(it -> !isOrderSheet(it, dateRange));
        //
        LOGGER.info("{}下面总共找到{}个{}到{}之间的Sheet, {}",
                spreadsheet.getProperties().getTitle(), sheetNames.size(), Dates.format(dateRange.getMinimum(), "MM/dd/yyyy"),
                Dates.format(dateRange.getMaximum(), "MM/dd/yyyy"), sheetNames.toString());

        //LOGGER.info("{}下面有{}个Sheet待处理, {}", spreadsheet.getProperties().getTitle(), sheetNames.size(), sheetNames.toString());

        if (sheetNames.size() == 0) {
            throw new BusinessException("No worksheets between " + Dates.format(dateRange.getMinimum(), "M/d") +
                    " and " + Dates.format(dateRange.getMaximum(), "M/d") + " found.");
        }

        List<String> a1Notations = sheetNames.stream().map(it -> it + "!A1:AZ").collect(Collectors.toList());

        List<SelfOrder> orders = fetchSelfOrders(spreadsheet.getSpreadsheetId(), a1Notations, OrderAction.AddProduct, sid);


        LOGGER.info("读取{}({})中位于{}-{}期间的{}个页签, 获得{}条订单信息，耗时{}", spreadTitle, spreadsheet.getSpreadsheetId(),
                Dates.format(dateRange.getMinimum(), "MM/dd"), Dates.format(dateRange.getMaximum(), "MM/dd"),
                sheetNames.size(),
                orders.size(),
                Strings.formatElapsedTime(start));

        return orders;
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, String sheetName, OrderAction orderAction) {
        String sid = Settings.load().getSid();
        return fetchSelfOrders(spreadsheetId, Lists.newArrayList(sheetName + "!A1:AZ"), orderAction, sid);
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, List<String> ranges, OrderAction orderAction) {
        String sid = Settings.load().getSid();
        return fetchSelfOrders(spreadsheetId, ranges, orderAction, sid);
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, List<String> ranges, OrderAction orderAction, String sid) {

        List<ValueRange> valueRanges;
        try {
            valueRanges = selfOrderSheetService.batchGetSpreadsheetValues(spreadsheetId, ranges);
        } catch (BusinessException e) {
            LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, ranges, e);
            throw e;
        }

        List<SelfOrder> selfOrders = new ArrayList<>();

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
            List<SelfOrder> ordersForSheet = new ArrayList<>();
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

                SelfOrder selfOrder = new SelfOrder();
                int maxCol = columns.size();
                for (int j = 1; j <= maxCol; j++) {
                    try {
                        selfOrderHelper.setColumnValue(j, columns.get(j - 1), selfOrder);
                    } catch (Exception e) {
                        //
                    }
                }


                if (orderAction == OrderAction.Process) {
                    if (!sid.equalsIgnoreCase(selfOrder.buyerAccountCode)) {
                        continue;
                    }
                } else if (orderAction == OrderAction.AddProduct) {
                    if (!sid.equalsIgnoreCase(StringUtils.substring(selfOrder.ownerAccountCode, 0, selfOrder.ownerAccountCode.length() - 2))) {
                        continue;
                    }
                } else {
                    if (!sid.equalsIgnoreCase(selfOrder.buyerAccountCode) && !sid.equalsIgnoreCase(StringUtils.substring(selfOrder.ownerAccountCode, 0, selfOrder.ownerAccountCode.length() - 2))) {
                        continue;
                    }
                }

                selfOrder.setRow(row);
                selfOrder.setSpreadsheetId(spreadsheetId);
                selfOrder.setSheetName(sheetName);
                ordersForSheet.add(selfOrder);
            }

            LOGGER.info("{}->{} found {} orders", spreadsheetId, sheetName, ordersForSheet.size());

            selfOrders.addAll(ordersForSheet);
        }

        return selfOrders;
    }

    public static boolean isOrderSheet(String sheetName, Range<Date> dateRange) {
        Date minDate = dateRange.getMinimum();
        Date maxDate = DateUtils.addDays(dateRange.getMaximum(), 1);
        return (RegexUtils.Regex.COMMON_ORDER_SHEET_NAME.isMatched(sheetName) &&
                minDate.before(Dates.parseDateOfGoogleSheet(sheetName)) &&
                maxDate.after(Dates.parseDateOfGoogleSheet(sheetName)));
    }


    public static void main(String[] args) {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        SelfOrderService selfOrderService = ApplicationContext.getBean(SelfOrderService.class);
        List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, "03/01", OrderAction.Process);
        System.out.println(selfOrders);

        System.exit(0);
    }
}
