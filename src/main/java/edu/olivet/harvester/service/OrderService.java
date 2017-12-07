package edu.olivet.harvester.service;

import com.google.api.services.sheets.v4.model.*;
import com.google.inject.Inject;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.SheetUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/7/17 10:41 AM
 */
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Inject
    SheetAPI spreadsheetService;

    @Inject
    private OrderHelper orderHelper;

    /**
     * max days backward based on today to locate all possible sheets of given spreadsheet
     */
    private static final int MAX_DAYS = 30;

    public List<Order> fetchOrders(Spreadsheet spreadsheet) {
        Date minDate = DateUtils.addDays(new Date(), -MAX_DAYS);
        return fetchOrders(spreadsheet, minDate);
    }


    public List<Order> fetchOrders(String spreadsheetId, List<String> ranges) {


        List<ValueRange> valueRanges;
        try {
            valueRanges = spreadsheetService.batchGetSpreadsheetValues(spreadsheetId, ranges);
        } catch (BusinessException e) {
            LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, ranges, e);
            throw e;
        }

        Map<String, Map<Integer, Color>> colors = new HashMap<>();
        try {
            List<String> colorRanges = ranges.stream().map(it -> it.substring(0, it.indexOf("!")) + "!B:B").collect(Collectors.toList());
            colors = fetchBackgroundColors(spreadsheetId, colorRanges);
        } catch (Exception e) {
            LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, ranges, e);

        }


        List<Order> orders = new ArrayList<>();
        //loop by sheet
        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> rows = valueRange.getValues();
            String a1Notation = valueRange.getRange();

            if (CollectionUtils.isEmpty(rows)) {
                LOGGER.warn("{}->{}读取不到任何有效数据", spreadsheetId, StringUtils.defaultString(a1Notation, "!No A1notation!"));
                continue;
            }

            String sheetName = a1Notation.substring(1, a1Notation.indexOf("!") - 1);

            //loop for each row
            //first row is header, each row represents an order/orderitem
            List<String> header = new ArrayList<>();

            int starRow = 0;
            for (List<Object> objects : rows) {
                if (CollectionUtils.isEmpty(objects)) {
                    continue;
                }
                if (objects.toString().contains("order-id")) {
                    break;
                }
                starRow++;
            }

            if (starRow >= rows.size()) {
                LOGGER.warn("{}->{}读取不到任何有效Header", spreadsheetId, StringUtils.defaultString(a1Notation, "!No A1notation!"));
                continue;
            }

            List<Object> headerRow = rows.get(starRow);
            headerRow.forEach(it -> header.add(it.toString()));
            starRow++;


            List<Order> ordersForSheet = new ArrayList<>();
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

                Order order = new Order();

                int maxCol = columns.size() >= OrderEnums.OrderColumn.TRACKING_NUMBER.number() ?
                        OrderEnums.OrderColumn.TRACKING_NUMBER.number() : columns.size();
                for (int j = OrderEnums.OrderColumn.STATUS.number(); j <= maxCol; j++) {
                    try {
                        orderHelper.setColumnValue(j, columns.get(j - 1), order);
                    } catch (Exception e) {
                        //
                    }
                }


                order.row = row;

                try {
                    Color color = colors.get(sheetName).get(row);
                    order.color = SheetUtils.colorToHex(color);
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
                if (StringUtils.isEmpty(order.order_id) || StringUtils.isEmpty(order.sku)) {
                    continue;
                }

                if (!RegexUtils.Regex.AMAZON_ORDER_NUMBER.isMatched(order.order_id)) {
                    continue;
                }

                order.setSpreadsheetId(spreadsheetId);
                order.setSheetName(sheetName);
                ordersForSheet.add(order);
            }


            LOGGER.info("{}->{} found {} orders", spreadsheetId, sheetName, ordersForSheet.size());

            orders.addAll(ordersForSheet);
        }

        return orders;

    }

    public Map<String, Map<Integer, Color>> fetchBackgroundColors(String spreadsheetId, List<String> ranges) {

        Spreadsheet spreadsheet;
        try {
            spreadsheet = spreadsheetService.getSpreadsheet(spreadsheetId, ranges);
        } catch (BusinessException e) {
            LOGGER.error("error get sheet properties for {} with ranges {}. {}", spreadsheetId, ranges, e);
            throw e;
        }

        Map<String, Map<Integer, Color>> colors = new HashMap<>();
        spreadsheet.getSheets().forEach(sheet -> {
            List<GridData> data = sheet.getData();
            String sheetName = sheet.getProperties().getTitle();
            Map<Integer, Color> sheetColors = new HashMap<>();

            data.forEach(d -> {
                List<RowData> rows = d.getRowData();
                int row = 1;
                if (CollectionUtils.isNotEmpty(rows)) {
                    for (RowData rowData : rows) {
                        if (rowData.size() > 0) {
                            try {
                                Color color;
                                if (rowData.getValues().get(0).getEffectiveFormat() != null) {
                                    color = rowData.getValues().get(0).getEffectiveFormat().getBackgroundColor();
                                } else {
                                    color = rowData.getValues().get(0).getUserEnteredFormat().getBackgroundColor();
                                }
                                sheetColors.put(row, color);
                            } catch (Exception e) {
                                LOGGER.error("", e);
                            }
                        }
                        row++;
                    }
                }
            });

            colors.put(sheetName, sheetColors);
        });


        return colors;

    }

    public boolean isOrderSheet(String sheetName, Range<Date> dateRange) {
        Date minDate = dateRange.getMinimum();
        Date maxDate = DateUtils.addDays(dateRange.getMaximum(), 1);
        return "individual orders".equals(sheetName.toLowerCase()) ||
                "seller canceled orders".equals(sheetName.toLowerCase()) ||
                "seller cancelled orders".equals(sheetName.toLowerCase()) ||
                "ship to us".equals(sheetName.toLowerCase()) ||
                (RegexUtils.Regex.COMMON_ORDER_SHEET_NAME.isMatched(sheetName) &&
                        minDate.before(Dates.parseDateOfGoogleSheet(sheetName)) &&
                        maxDate.after(Dates.parseDateOfGoogleSheet(sheetName)));
    }

    public boolean isOrderSheet(String sheetName, Date minDate) {
        Date today = new Date();
        Range<Date> dateRange = Range.between(minDate, today);

        return isOrderSheet(sheetName, dateRange);
    }

    public List<Order> fetchOrders(Spreadsheet spreadsheet, Range<Date> dateRange) {
        final long start = System.currentTimeMillis();
        String spreadTitle = spreadsheet.getProperties().getTitle();

        List<String> sheetNames = new ArrayList<>();
        //save sheet properties to cache
        spreadsheet.getSheets().forEach(sheet -> sheetNames.add(sheet.getProperties().getTitle()));

        sheetNames.removeIf(it -> !isOrderSheet(it, dateRange));
        //
        LOGGER.info("{}下面总共找到{}个{}到{}之间的Sheet, {}", spreadsheet.getProperties().getTitle(), sheetNames.size(), Dates.format(dateRange.getMinimum(), "MM/dd/yyyy"), Dates.format(dateRange.getMaximum(), "MM/dd/yyyy"), sheetNames.toString());

        //LOGGER.info("{}下面有{}个Sheet待处理, {}", spreadsheet.getProperties().getTitle(), sheetNames.size(), sheetNames.toString());

        if (sheetNames.size() == 0) {
            throw new BusinessException("No worksheets between " + Dates.format(dateRange.getMinimum(), "M/d") + " and " + Dates.format(dateRange.getMaximum(), "M/d") + " found.");
        }

        List<String> a1Notations = sheetNames.stream().map(it -> it + "!A1:AZ").collect(Collectors.toList());

        List<Order> orders = fetchOrders(spreadsheet.getSpreadsheetId(), a1Notations);


        LOGGER.info("读取{}({})中位于{}-{}期间的{}个页签, 获得{}条订单信息，耗时{}", spreadTitle, spreadsheet.getSpreadsheetId(),
                Dates.format(dateRange.getMinimum(), "MM/dd"), Dates.format(dateRange.getMaximum(), "MM/dd"),
                sheetNames.size(),
                orders.size(),
                Strings.formatElapsedTime(start));

        return orders;

    }

    public List<Order> fetchOrders(Spreadsheet spreadsheet, Date minDate) {

        Range<Date> dateRange = Range.between(minDate, new Date());

        return fetchOrders(spreadsheet, dateRange);


    }


    public List<Order> getOrders(Spreadsheet spreadsheet) {
        Date minDate = DateUtils.addDays(new Date(), -MAX_DAYS);
        return getOrders(spreadsheet, minDate);
    }

    public List<Order> getOrders(Spreadsheet spreadsheet, List<String> ranges) {

        List<Order> orders;

        try {
            orders = fetchOrders(spreadsheet.getSpreadsheetId(), ranges);
        } catch (BusinessException e) {
            LOGGER.error("Error fetch orders from spreadsheet {} range {} - {}", spreadsheet.getSpreadsheetId(), ranges, e);
            throw e;
        }
        if (orders.isEmpty()) {
            LOGGER.error("No orders found from spreadsheet {} range {}.", spreadsheet.getSpreadsheetId(), ranges);
            return orders;
        }

        return orders;
    }

    public List<Order> getOrders(Spreadsheet spreadsheet, Date minDate) {
        //get all orders, and organized by sheet
        List<Order> orders;
        try {
            orders = fetchOrders(spreadsheet, minDate);
        } catch (BusinessException e) {
            LOGGER.error("Error fetch orders from spreadsheet {} - {}", spreadsheet.getSpreadsheetId(), e);
            throw e;
        }
        if (orders.isEmpty()) {
            LOGGER.error("No orders found from spreadsheet {}.", spreadsheet.getSpreadsheetId());
            return orders;
        }


        return orders;
    }


    public String getDestSheetNameFromOrder(Order order) {
        String sheetName = order.status.replaceAll("[^0-9/]", "");
        if (StringUtils.isBlank(sheetName)) {
            sheetName = order.remark.replaceAll("[^0-9/]", "");
        }

        return sheetName;
    }


    public List<Order> findDuplicates(Spreadsheet spreadsheet) {
        List<Order> orders = getOrders(spreadsheet);
        return findDuplicates(orders);
    }

    /**
     * <pre>
     * check duplicated orders
     * orders will be treated as duplicated if:
     * 1. same order id & sku, same remark
     * 2. same order id & sku, but in different sheet
     *
     * orders with same order id and sku, but have same account no and in the same sheet will not be treat as duplicate
     *
     * </pre>
     *
     * @param orders order in one spreadsheet
     */
    public List<Order> findDuplicates(List<Order> orders) {
        final List<Order> setToReturn = new ArrayList<>();
        Map<String, List<Order>> map = new HashMap<>();
        orders.forEach(it -> {
            String key = it.order_id + it.sku + originalRemark(it.remark);
            List<Order> list = map.get(key);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(it);
            map.put(key, list);
        });

        map.forEach((k, v) -> {
            if (v.size() > 1) {
                Set<String> sheetNames = v.stream().filter(it -> RegexUtils.Regex.COMMON_ORDER_SHEET_NAME.isMatched(it.sheetName)).map(Order::getSheetName).collect(Collectors.toSet());
                if (sheetNames.size() > 1) {
                    setToReturn.addAll(v);
                }

            }
        });


        return setToReturn;

    }

    public static String originalRemark(String remark) {
        if (StringUtils.isBlank(remark)) {
            return remark;
        }
        return remark.replaceAll("\\d{2}月/\\d{2}日已移表", "").trim().toUpperCase();
    }

    public static void main(String[] args) {
        System.out.println(OrderService.originalRemark("zuoba2 us fwd 10月/10日已移表"));
    }


}
