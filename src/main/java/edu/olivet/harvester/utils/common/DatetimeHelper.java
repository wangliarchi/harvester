package edu.olivet.harvester.utils.common;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.temporal.TemporalField;
import java.util.Random;

import static java.time.temporal.ChronoField.SECOND_OF_DAY;

public class DatetimeHelper {

    public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");

    public static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("0.000");

    /**
     * 格式化从某一时间点开始到现在为止的消耗时间
     *
     * @param start 开始时间点的时间戳
     * @return 格式化好的消耗时间
     */
    public static String formatCostTime(long start) {
        long cost = System.currentTimeMillis() - start;
        return formatTime(cost);
    }

    private static final String[] TIME_UNITS = {"h", "m", "s", "ms"};
    private static final long[] TIME_RANGES = {1000 * 60 * 60, 1000 * 60, 1000};

    /**
     * 格式化消耗时间
     *
     * @param cost 消耗时间
     * @return 格式化好的消耗时间
     */
    public static String formatTime(long cost) {
        for (int i = 0; i < TIME_RANGES.length; i++) {
            if (cost >= TIME_RANGES[i]) {
                double result = (double) cost / TIME_RANGES[i];
                return DOUBLE_FORMAT.format(result) + " " + TIME_UNITS[i];
            }
        }

        return DOUBLE_FORMAT.format(cost) + " MS";
    }

    public static LocalTime randomTimeBetween(LocalTime from, int allowedRangeInMins) {

        LocalTime fromTime = from.minusMinutes(allowedRangeInMins);
        LocalTime toTime = from.plusMinutes(allowedRangeInMins);
        return randomTimeBetween(fromTime, toTime);
    }

    public static LocalTime randomTimeBetween(LocalTime from, LocalTime to) {

        int maxAllowed = to.get(SECOND_OF_DAY) - from.get(SECOND_OF_DAY);

        int randSeconds = new Random().ints(1, 0, maxAllowed).findFirst().getAsInt();
        LocalTime time = from.plusSeconds(randSeconds);
        return time;
    }


    public static void main(String[] args) {
        LocalTime from = LocalTime.of(12, 0);
        LocalTime to = LocalTime.of(12, 10);
        LocalTime rand = DatetimeHelper.randomTimeBetween(from, to);
        System.out.println(rand);
    }
}
