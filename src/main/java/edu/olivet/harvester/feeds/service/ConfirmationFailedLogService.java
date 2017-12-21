package edu.olivet.harvester.feeds.service;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.utils.Settings;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 10:00 AM
 */
public class ConfirmationFailedLogService {
    private static final String FAILED_LOG_APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwaEZnIya4MWuXHpfIk_aOzTY3KK6EygIROXqSGRw-gU1sc_iD2/exec";

    @Repeat
    public static void logFailed(Country country, String sheetName, String errorMsg) {
        String sid = Settings.load().getSid() + country.name();
        String url = FAILED_LOG_APP_SCRIPT_URL + "?s=" + Strings.encode(sid) + "&sn=" + Strings.encode(sheetName) + "&m=" + Strings.encode(errorMsg);
        try {
            Jsoup.connect(url).ignoreContentType(true).timeout(12000).execute().body().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        logFailed(Country.US, "12/21", "test error msg");
    }
}
