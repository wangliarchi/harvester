package edu.olivet.harvester.spreadsheet.utils;

import com.google.api.services.sheets.v4.model.Color;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/24/17 10:30 AM
 */
public class SheetUtils {


    public static String getTodaySheetName() {
        return getSheetNameByDate(System.currentTimeMillis());
    }

    public static String getSheetNameByDate(long millis) {
        return FastDateFormat.getInstance("MM/dd").format(millis);
    }

    public static String getSheetNameByDate(Date date) {
        return DateFormat.FULL_MONTH_DAY.format(date);
    }

    public static String colorToHex(Color color) {
        if (color == null) {
            return "#FFFFFF";
        }
        int r = color.getRed() != null ? (int) (color.getRed() * 255) : 0;
        int g = color.getGreen() != null ? (int) (color.getGreen() * 255) : 0;
        int b = color.getBlue() != null ? (int) (color.getBlue() * 255) : 0;
        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static OrderItemType getTypeFromSpreadsheetName(String spreadName) {
        return Strings.containsAnyIgnoreCase(spreadName, "product") ? OrderItemType.PRODUCT : OrderItemType.BOOK;
    }

    public static Country getCountryFromSpreadsheetName(String spreadsheetTitle) {
        String str = StringUtils.defaultString(spreadsheetTitle).toUpperCase()
                .replaceFirst("ORDER" + ".*", StringUtils.EMPTY);

        List<Country> marketplaces =
                Arrays.asList(Country.US, Country.CA, Country.UK, Country.JP, Country.IN, Country.MX, Country.AU);

        for (Country country : marketplaces) {
            // EU spreadsheet might be named 'UK' or 'EU'
            String regex = country.europe() ? "(UK|EU)" : country.name();
            if (RegexUtils.containsRegex(str, regex)) {
                return country;
            }
        }

        throw new BusinessException("Cant get country info from spreadsheet name " + spreadsheetTitle);
    }
}
