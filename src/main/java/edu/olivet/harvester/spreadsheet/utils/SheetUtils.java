package edu.olivet.harvester.spreadsheet.utils;

import com.google.api.services.sheets.v4.model.Color;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import org.apache.commons.lang3.time.FastDateFormat;

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

}
