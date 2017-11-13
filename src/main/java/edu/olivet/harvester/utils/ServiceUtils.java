package edu.olivet.harvester.utils;

import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;

import java.util.List;

public class ServiceUtils {

    /**
     * 解析Feedback Submission Result（比如Tracking号上传、下架删点、上架等）中的总数、成功数和失败数
     *
     * @param submissionResult Feed文件提交结果
     * @return 结果数组: 0 - 总数，1 - 成功， 2 - 失败
     */
    public static int[] parseFeedSubmissionResult(String submissionResult) {
        List<String> list = RegexUtils.getMatchedList(submissionResult, ": [0-9]{1,4}");
        if (list.size() >= 3) {
            return new int[]{Strings.toInt(list.get(0)), Strings.toInt(list.get(1)), Strings.toInt(list.get(2))};
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




}
