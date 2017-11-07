package edu.olivet.harvester.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;

import java.util.List;
import java.util.TimeZone;

public class ServiceUtils {

    /**
     * 解析Feedback Submission Result（比如Tracking号上传、下架删点、上架等）中的总数、成功数和失败数
     * @param submissionResult  Feed文件提交结果
     * @return  结果数组: 0 - 总数，1 - 成功， 2 - 失败
     */
    public static int[] parseFeedSubmissionResult(String submissionResult) {
        List<String> list = RegexUtils.getMatchedList(submissionResult, ": [0-9]{1,4}");
        if (list.size() >= 3) {
            return new int[] {Strings.toInt(list.get(0)), Strings.toInt(list.get(1)), Strings.toInt(list.get(2))};
        }
        int[] counts = new int[3];
        list = RegexUtils.getMatchedList(submissionResult, "Number of records.*[0-9]{1,4}");
        if (list.size() >= 2) {
            counts[0] = Strings.toInt(list.get(0));
            counts[1] = Strings.toInt(list.get(1));
        }
        counts[2] = counts[0] - counts[1];
        return counts;
    }

    /**
     * 获取指定国家对应的时区，主要用于<strong>没有太大时区跨度的国家</strong>
     */
    public static TimeZone getTimeZone(Country country) {
        if (country == Country.UK || country == Country.ES) {
            // 欧洲西部时区
            return TimeZone.getTimeZone("WET");
        } else if (country.europe()) {
            // 欧洲中部时区
            return TimeZone.getTimeZone("CET");
        } else if (country == Country.JP) {
            return TimeZone.getTimeZone("FET");
        } else if (country == Country.IN) {
            return TimeZone.getTimeZone("IDT");
        } else if (country == Country.US || country == Country.MX || country == Country.CA) {
            return TimeZone.getTimeZone("PST");
        } else {
            return TimeZone.getDefault();
        }
    }
}
