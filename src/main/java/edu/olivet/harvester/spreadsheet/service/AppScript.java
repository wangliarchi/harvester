package edu.olivet.harvester.spreadsheet.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.setting.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.FwdAddressUtils;
import edu.olivet.harvester.fulfill.utils.OrderFilter;
import edu.olivet.harvester.common.model.ConfigEnums;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderColor;
import edu.olivet.harvester.common.model.OrderEnums.OrderColumn;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Singleton
public class AppScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppScript.class);
    protected static final String APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycby3oR8IH5Z7uaTi-i_GPUfWeeFV96ejxfyo3dlq8tXZivpW51F0/exec";
    protected static final String SUCCESS = "Success";
    protected static final String PARAM_SHEET_NAME = "sn";
    protected static final String PARAM_SPREAD_ID = "s";
    protected static final String PARAM_METHOD = "method";
    protected static final String PARAM_ROW = "row";
    public static final String AUTHORIZED_EMAIL = "ordermanibport@gmail.com";

    protected static Map<String, Spreadsheet> SPREADSHEET_CLIENT_CACHE = new HashMap<>();

    @Repeat(expectedExceptions = {BusinessException.class, JSONException.class})
    public Spreadsheet getSpreadsheet(String spreadId) {
        Spreadsheet spreadsheet = SPREADSHEET_CLIENT_CACHE.computeIfAbsent(spreadId, k -> this.reloadSpreadsheet(spreadId));
        return afterSpreadsheetLoaded(spreadsheet);

    }

    public @Nullable Spreadsheet getSpreadsheetFromCache(String spreadId) {
        return SPREADSHEET_CLIENT_CACHE.getOrDefault(spreadId, null);

    }

    public Spreadsheet reloadSpreadsheet(String spreadId) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_METHOD, "GETSPREADMETADATA");
        String json = this.processResult(this.get(params));
        Spreadsheet spreadsheet;
        try {
            spreadsheet = JSON.parseObject(json, Spreadsheet.class);
        } catch (Exception e) {
            if (StringUtils.containsIgnoreCase(json, "is missing")) {
                SheetAPI sheetAPI = ApplicationContext.getBean(SheetAPI.class);
                sheetAPI.sharePermissions(spreadId, AUTHORIZED_EMAIL);
            }
            throw new BusinessException(json);
        }
        SPREADSHEET_CLIENT_CACHE.put(spreadId, spreadsheet);
        return spreadsheet;

    }

    public Spreadsheet afterSpreadsheetLoaded(Spreadsheet spreadsheet) {
        try {
            Settings settings = Settings.load();
            spreadsheet.setSpreadsheetCountry(settings.getSpreadsheetCountry(spreadsheet.getSpreadsheetId()));
            spreadsheet.setSpreadsheetType(settings.getSpreadsheetType(spreadsheet.getSpreadsheetId()));
        } catch (Exception e) {
            //ignore
        }

        return spreadsheet;
    }

    public void clearCache() {
        SPREADSHEET_CLIENT_CACHE.clear();
    }


    public void preloadAllSpreadsheets() {
        try {
            List<String> spreadsheetIds = Settings.load().listAllSpreadsheets();
            for (String spreadsheetId : spreadsheetIds) {
                try {
                    reloadSpreadsheet(spreadsheetId);
                } catch (Exception e) {
                    LOGGER.error("{} is invalid. {}", spreadsheetId, e);
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }

    /**
     * <pre>
     * Possible Spreadsheet url examples:
     * https://spreadsheets.google.com/feeds/spreadsheets/1Uij57nwM7Djh8wTEe8eftL9dBBj7UkQ4fprbcbC-f1M
     * https://spreadsheets.google.com/feeds/cells/1WPxUMWjwj5kQ--lU7JyYI_NYQcqrbxMcLPaatLFzfOA/oj8w1em/private/full
     * https://docs.google.com/spreadsheets/d/1nqqqUMx5UaDDlJfpGuu5wbbI6WR6kKssvXBtrLRUlQQ/edit#gid=299424004
     * http://drive.google.com/open?id=13eW9K-DGhPQvBdZCnOIjJzn477JAppmw8B9H3ELB90M
     * </pre>
     */
    private static final String URL_PREFIX =
            "https?://(spreadsheets.google.com/feeds/(cells|spreadsheets)/|docs.google.com/spreadsheets/d/|drive.google.com/open[?]id=)";

    public static String getSpreadId(String url) {
        String str = StringUtils.defaultString(url).replaceFirst(URL_PREFIX, StringUtils.EMPTY);
        return str.contains("/") ? str.substring(0, str.indexOf("/")) : str;
    }

    @Repeat(expectedExceptions = {BusinessException.class, JSONException.class})
    private boolean markColor(String sheetUrl, String sheetName, int row, String notation, OrderColor color) {
        Map<String, String> params = new HashMap<>();
        String spreadId = getSpreadId(sheetUrl);
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_ROW, String.valueOf(row));
        if (StringUtils.isNotBlank(notation)) {
            params.put("r", notation);
        }
        params.put(PARAM_METHOD, "BGCOLOR");
        params.put("code", color.code());

        String result = this.processResult(this.get(params));
        if (!SUCCESS.equals(result)) {
            WaitTime.Shortest.execute();
            result = this.processResult(this.get(params));
        }
        return SUCCESS.equals(result);
    }


    /**
     * @see #markColor(String, String, int, String, OrderColor)
     */
    public boolean markColor(String sheetUrl, String sheetName, int row, OrderColor color) {
        return this.markColor(sheetUrl, sheetName, row, String.format("A%d:AO%d", row, row), color);
    }

    @Repeat(expectedExceptions = {BusinessException.class, JSONException.class})
    public void commitShippingConfirmationLog(String spreadId, String sheetName, int row, String log) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_ROW, String.valueOf(row));
        params.put("log", log);
        params.put(PARAM_METHOD, "CommitShippingConfirmationLog");
        String result = this.processResult(this.get(params));
        if (!SUCCESS.equals(result)) {
            throw new BusinessException(String.format("Failed to commit shipping confirmation log to row %d of sheet %s: %s",
                    row, sheetName, result));
        }
    }

    protected String processResult(String result) {
        if (Strings.containsAnyIgnoreCase(StringUtils.defaultString(result), "<html")) {
            Document doc = Jsoup.parse(result);
            return doc.select("body").get(0).text().trim();
        }
        return StringUtils.defaultString(result).trim();
    }

    @Inject
    private OrderHelper orderHelper;
    private static final String DEFAULT_RANGE = "A-AO";

    public List<Order> readOrders(OrderSubmissionTask task) {
        List<Order> orders = readOrders(task.convertToRuntimeSettings());
        orders.forEach(order -> order.setTask(task));
        return orders;
    }

    public List<Order> readOrders(RuntimeSettings settings) {
        try {
            List<Order> orders = readOrders(settings.getSpreadsheetId(), settings.getSheetName());

            orders = OrderFilter.filterOrders(orders, settings.getAdvancedSubmitSetting());
            orders.forEach(it -> {
                it.setContext(settings.context());
                it.setRuntimeSettings(settings);
            });

            if (org.apache.commons.collections.CollectionUtils.isEmpty(orders)) {
                return orders;
            }

            AdvancedSubmitSetting advs = settings.getAdvancedSubmitSetting();
            // 限制做多少条时，需要按照条件过滤完之后取子集，不能直接在AppScript端取子集
            int size = orders.size();
            if (advs.getSubmitRange() == ConfigEnums.SubmitRange.LimitCount && size > advs.getCountLimit()) {
                return orders.subList(0, advs.getCountLimit());
            }


            return orders;

        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    @Inject private OrderService orderService;

    @Repeat(expectedExceptions = BusinessException.class)
    public List<Order> readOrders(String spreadId, String sheetName) {
        final long start = System.currentTimeMillis();
        List<Order> orders;
        //try {
        //    orders = orderService.fetchOrders(spreadId, sheetName);
        //    LOGGER.info("Read {} orders from sheet {} via sheet api in {}", orders.size(), sheetName, Strings.formatElapsedTime(start));
        //} catch (Exception e) {
        //    LOGGER.error("Fail to read orders via sheet api for {} {}", spreadId, sheetName, e);
        try {
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_SPREAD_ID, getSpreadId(spreadId));
            params.put(PARAM_SHEET_NAME, sheetName);
            params.put(PARAM_METHOD, "READ");
            params.put("r", DEFAULT_RANGE);
            String json = this.processResult(this.get(params));
            orders = this.parse(json);
            LOGGER.info("Read {} orders from sheet {} via app script in {}", orders.size(), sheetName, Strings.formatElapsedTime(start));
        } catch (Exception e) {
            orders = orderService.fetchOrders(spreadId, sheetName);
            LOGGER.info("Read {} orders from sheet {} via sheet api in {}", orders.size(), sheetName, Strings.formatElapsedTime(start));
        }

        orders.forEach(it -> {
            it.setSheetName(sheetName);
            it.setSpreadsheetId(spreadId);
        });

        //todo don't need to read every time
        try {
            FwdAddressUtils.getLastFWDIndex(spreadId, sheetName, orders);
        } catch (Exception e) {
            LOGGER.error("", e);
        }


        return orders;
    }

    @Data
    public static class ReadResult {
        boolean valid() {
            return ArrayUtils.isNotEmpty(orders) && ArrayUtils.isNotEmpty(colors);
        }

        @Getter
        private int startRow;
        @Getter
        private int endRow;
        @Getter
        private String[][] orders;
        @Getter
        private String[][] colors;
    }

    public List<Order> parse(String json) {
        ReadResult result;
        try {
            result = JSONArray.parseObject(json, ReadResult.class);
        } catch (JSONException e) {
            throw new JSONException("Failed to parse order data in AppScript response result: " + json);
        }

        if (result == null || !result.valid()) {
            throw new JSONException("There is no order data in AppScript response result: " + json);
        }

        List<Order> orders = new ArrayList<>();
        int startRow = result.startRow, endRow = result.endRow;
        if (startRow <= 0 && endRow <= 0) {
            return orders;
        }

        String[][] array = result.orders;
        String[][] colors = result.colors;
        for (int i = 0; i < array.length; i++) {
            Order order = new Order();
            order.row = startRow + i;
            order.color = colors[i][0];

            String[] columns = array[i];
            int maxCol = columns.length >= OrderColumn.TRACKING_NUMBER.number() ? OrderColumn.TRACKING_NUMBER.number() : columns.length;
            for (int j = OrderColumn.STATUS.number(); j <= maxCol; j++) {
                orderHelper.setColumnValue(j, columns[j - 1], order);
            }

            if (!Regex.AMAZON_ORDER_NUMBER.isMatched(order.order_id)) {
                continue;
            }

            try {
                orderHelper.autoCorrect(order);
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to correct attributes of order {}: {}", order.order_id, Strings.getExceptionMsg(e));
                //continue;
            }
            orders.add(order);
        }
        return orders;
    }

    protected String getBaseUrl() {
        return APP_SCRIPT_URL;
    }

    protected String get(Map<String, String> params) {
        String params4Url = this.params2Url(params);
        String url = getBaseUrl() + params4Url;
        try {
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }

    protected String params2Url(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            sb.append("?");
            int i = 0;
            for (Entry<String, String> entry : params.entrySet()) {
                if (i++ > 0) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=");
                String value = Strings.encode(entry.getValue());
                sb.append(value);
            }
        }
        return sb.toString();
    }


    public List<Order> getOrdersFromWorksheet(Worksheet worksheet) {

        List<Order> orders;


        try {
            orders = this.readOrders(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName());
        } catch (Exception e) {
            LOGGER.error("读取订单数据失败: {} - {}", worksheet.getSheetName(), e);

            if (e.getMessage().contains("Cannot read sheet according to provided sheet id and name")) {
                throw new NoWorksheetFoundException(e.getMessage());
            }

            throw new BusinessException(e.getMessage());
        }

        if (CollectionUtils.isEmpty(orders)) {
            throw new NoOrdersFoundInWorksheetException("No valid orders found for sheet  " + worksheet.toString() + ".");
        }

        return orders;
    }


    protected String exec(Map<String, String> params) {
        String json = null;
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            json = this.processResult(this.get(params));
            if (SUCCESS.equals(json)) {
                return json;
            } else if (params.get("method").equals("READSYNCREPORT") || params.get("method").equals("READWATCHDOGACCOUNTLIST")) {
                return json;
            }
        }
        String method = StringUtils.defaultString(params.get(PARAM_METHOD));
        throw new BusinessException(String.format("Failed to invoke method '%s' after %s attempts. Error detail: %s.", method, Constants.MAX_REPEAT_TIMES, StringUtils.defaultString(json)));
    }
}