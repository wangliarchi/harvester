package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 12:08 PM
 */
public class CurrencyUtils {
    public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");
    public static final String FULL_STOP = ".";

    /**
     * 提取Payment页面字符串中金额信息，通过正则表达式替换非(数字,.)之外的内容
     * 日本：	￥ 600
     *
     * @param source 原始字符串
     */
    public static String getCurrencyValue(String source) {
        return _getCurrencyValue(source, false);
    }

    public static String getFirstCurrencyValue(String source) {
        return _getCurrencyValue(source, true);
    }

    private static String _getCurrencyValue(String source, boolean first) {

        //  日本gift卡余额部分内容比较多，需要去掉余额后面的非数字部分。
        if (source.contains("&nbsp")) {
            int index = source.indexOf("&nbsp");
            source = source.substring(0, index);
            source = source.trim();
        }

        // 日本页面没有小数点，填小数点以便后面的程序能够正确运行。
        if (source.contains("￥") && !source.contains(".")) {
            source = source + ".00";
        }


        List<String> list = RegexUtils.getMatchedList(StringUtils.defaultString(source), RegexUtils.Regex.AMOUNT.val());
        if (CollectionUtils.isEmpty(list)) {
            return DOUBLE_FORMAT.format(Constants.ZERO);
        }
        String result = first ? list.get(0) : list.get(list.size() - 1);
        boolean euro = source.contains(Country.DE.currencySymbol());

        return handlePunctuation(result, euro);
    }

    private static String handlePunctuation(String result, boolean euro) {
        if (!euro) {
            return result.replace(Constants.COMMA, StringUtils.EMPTY);
        } else {
            result = result.replace(Constants.COMMA, FULL_STOP);
            if (result.lastIndexOf(FULL_STOP) != result.indexOf(FULL_STOP)) {
                result = result.replaceFirst("\\.", StringUtils.EMPTY);
            }
            return result;
        }
    }
}
