package edu.olivet.harvester.export.service;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/15/17 2:42 PM
 */
public class ExportStatService {
    protected final Logger LOGGER = LoggerFactory.getLogger(ExportStatService.class);

    private static final String APPS_URL = "https://script.google.com/macros/s/AKfycbzyMGXmIJESIts92UZONd_kgsUO7srxp4e9e5lL0C8hk-DCRtQ/exec";

    @Inject
    AppScript appScript;
    @Inject
    SheetAPI sheetAPI;

    @Repeat(expectedExceptions = BusinessException.class)
    public Date getOrderExportFromDate(Country country) {
        String sid = Settings.load().getSid();
        String account = sid + country.name();

        try {
            String url = APPS_URL + "?method=initExport&account=" + account + "&cm=" + SystemUtils.USER_NAME;
            String result = Jsoup.connect(url).ignoreContentType(true).timeout(12000).execute().body().trim();
            if (Strings.containsAnyIgnoreCase(result, "running")) {
                throw new BusinessException("Order export process is running.");
            }

            if (StringUtils.isNotBlank(result)) {
                return Dates.parseDate(result);
            }

            return lastOrderDate(country);
        } catch (Exception e) {
            LOGGER.error("Fail to init order export for {}", country, e);
            throw new BusinessException(e);
        }

    }

    @Repeat(expectedExceptions = BusinessException.class)
    public void updateStat(Country country, Date lastDate, int total) {
        String sid = Settings.load().getSid();
        String account = sid + country.name();
        Date nowDate = new Date();
        try {
            String url = APPS_URL + "?method=UpdateStats&account=" + account + "&cm=" + SystemUtils.USER_NAME +
                    "&lastRunAt=" + DateFormat.DATE_TIME_STR.format(nowDate) +
                    "&lastUpdatedAt=" + (lastDate == null ? "" : DateFormat.DATE_TIME_STR.format(lastDate)) + "&total=" + total;
            Jsoup.connect(url).ignoreContentType(true).timeout(12000).execute();

        } catch (Exception e) {
            LOGGER.error("Fail to update order export stats for {}", country, e);
            throw new BusinessException(e);
        }

    }

    public Date lastOrderDate(Country country) {
        List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();
        Date date = DateUtils.addDays(new Date(), -6);
        for (String spreadsheetId : spreadsheetIds) {
            try {
                Date lastDate = lastOrderDate(spreadsheetId);
                if (lastDate.after(date)) {
                    date = lastDate;
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        return date;
    }

    public Date lastOrderDate(String spreadsheetId) {
        Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(spreadsheetId);
        List<Sheet> sheetList = spreadsheet.getSheets();
        String sheetName = null;
        for (Sheet sheet : sheetList) {
            if (RegexUtils.Regex.COMMON_ORDER_SHEET_NAME.isMatched(sheet.getProperties().getTitle())) {
                sheetName = sheet.getProperties().getTitle();
                break;
            }
        }

        if (StringUtils.isBlank(sheetName)) {
            throw new BusinessException("No valid sheet found from " + spreadsheetId);
        }
        List<Order> orders = appScript.readOrders(spreadsheetId, sheetName);

        if (CollectionUtils.isEmpty(orders)) {
            throw new BusinessException("No order found on " + sheetName + " of " + spreadsheetId);
        }
        orders.sort(Comparator.comparing(Order::getPurchaseDate).reversed());

        return orders.get(0).getPurchaseDate();
    }

    public static void main(String[] args) {
        ExportStatService exportStatService = ApplicationContext.getBean(ExportStatService.class);

        exportStatService.getOrderExportFromDate(Country.US);
    }
}
