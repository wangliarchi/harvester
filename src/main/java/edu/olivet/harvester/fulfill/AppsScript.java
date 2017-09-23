package edu.olivet.harvester.fulfill;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderColor;
import edu.olivet.harvester.model.OrderEnums.OrderColumn;
import edu.olivet.harvester.model.Spreadsheet;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
class AppsScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppsScript.class);
    private static final String APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycby3oR8IH5Z7uaTi-i_GPUfWeeFV96ejxfyo3dlq8tXZivpW51F0/exec";
    private static final String SUCCESS = "Success";
    private static final String PARAM_SHEET_NAME = "sn";
    private static final String PARAM_SPREAD_ID = "s";
    private static final String PARAM_METHOD = "method";
    private static final String PARAM_ROW = "row";

    @Repeat(expectedExceptions = {BusinessException.class, JSONException.class})
    Spreadsheet getSpreadsheet(String spreadId) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_METHOD, "GETSPREADMETADATA");
        String json = this.processResult(this.get(params));
        return JSON.parseObject(json, Spreadsheet.class);
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

    String getSpreadId(String url) {
        String str = StringUtils.defaultString(url).replaceFirst(URL_PREFIX, StringUtils.EMPTY);
        return str.contains("/") ? str.substring(0, str.indexOf("/")) : str;
    }

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
    boolean markColor(String sheetUrl, String sheetName, int row, OrderColor color) {
        return this.markColor(sheetUrl, sheetName, row, String.format("A%d:AO%d", row, row), color);
    }

    private String processResult(String result) {
        if (Strings.containsAnyIgnoreCase(StringUtils.defaultString(result), "<html")) {
            Document doc = Jsoup.parse(result);
            return doc.select("body").get(0).text().trim();
        }
        return StringUtils.defaultString(result).trim();
    }

    @Inject private OrderHelper orderHelper;
    private static final String DEFAULT_RANGE = "A-AO";

    @Repeat
    List<Order> readOrders(String spreadId, String sheetName) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, getSpreadId(spreadId));
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_METHOD, "READ");
        params.put("r", DEFAULT_RANGE);

        final long start = System.currentTimeMillis();
        String json = this.processResult(this.get(params));
        List<Order> orders = this.parse(json);
        LOGGER.info("Read {} orders from sheet {} via in {}", orders.size(), sheetName, Strings.formatElapsedTime(start));
        return orders;
    }

    @Data
    private static class ReadResult {
        boolean valid() {
            return ArrayUtils.isNotEmpty(orders) && ArrayUtils.isNotEmpty(colors);
        }

        private int startRow;
        private int endRow;
        private String[][] orders;
        private String[][] colors;
    }

    private List<Order> parse(String json) {
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
                continue;
            }
            orders.add(order);
        }
        return orders;
    }

    private String get(Map<String, String> params) {
        String params4Url = this.params2Url(params);
        String url = APP_SCRIPT_URL + params4Url;
        try {
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }

    private String params2Url(Map<String, String> params) {
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
}