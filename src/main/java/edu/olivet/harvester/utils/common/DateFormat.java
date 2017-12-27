package edu.olivet.harvester.utils.common;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nutz.lang.Lang;

import java.text.ParseException;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 9/29/17 10:30 AM
 */
public enum DateFormat {
    /** 记录当前时间的日期格式 */
    DATE_TIME("yyyy-MM-dd HH:mm:ss"),

    DATE_TIME_STR("yyyy-MM-dd_HH:mm:ss"),
    /** 记录当前时间的日期格式 */
    DATE_TIME_SHORT("M/d HH:mm"),
    /** 以当前时间为文件命名格式 */
    DATE_TIME_AS_FILENAME("yyyyMMdd_HHmmss"),
    /** 最终Confirm文件的ship-date列格式，形如:2014-10-8 */
    SHIP_DATE("yyyy-M-d"),
    FULL_DATE("yyyy-MM-dd"),
    /** 仅包含年的日期格式 */
    YEAR("yyyy"),
    /** 仅包含月日的日期格式 */
    MONTH_DAY("M/d"),
    /** 包含月日且前导零补齐的日期格式 */
    FULL_MONTH_DAY("MM/dd"),
    /** 美国Feedback日期格式 */
    US_FEEDBACK_DATE("M/d/yy"),
    /** 英国Feedback日期格式 */
    UK_FEEDBACK_DATE("dd/MM/yyyy"),
    /** Gmail查询日期格式，可以用于形如after: before: 等场景 */
    GMAIL_QUERY_DATE("yyyy/mm/dd"),
    /** 年月格式 */
    MONTH_YEAR("MM/yy");

    private final String pattern;

    public String pattern() {
        return this.pattern;
    }

    public FastDateFormat getInstance() {
        return FastDateFormat.getInstance(pattern);
    }

    public Date parse(String src) {
        try {
            return this.getInstance().parse(src);
        } catch (ParseException e) {
            throw Lang.wrapThrow(e);
        }
    }

    public String format(Date date) {
        return this.getInstance().format(date);
    }

    public String format(long timestamp) {
        return this.getInstance().format(timestamp);
    }

    DateFormat(String pattern) {
        this.pattern = pattern;
    }


}