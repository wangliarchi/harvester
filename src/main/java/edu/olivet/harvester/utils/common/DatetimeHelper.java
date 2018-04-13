package edu.olivet.harvester.utils.common;

import com.google.common.collect.Lists;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

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
    private static String formatTime(long cost) {
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
        if (maxAllowed <= 0) {
            maxAllowed = 5;
        }
        //noinspection ConstantConditions
        int randSeconds = new Random().ints(1, 0, maxAllowed).findFirst().getAsInt();
        return from.plusSeconds(randSeconds);
    }

    public static Date parseEdd(String eddText, Country country, Date now) {
        List<String> formatPatterns = Lists.newArrayList("MMM dd", "dd MMM", "MMMMM dd", "dd MMMMM");
        String[] eddParts = StringUtils.split(eddText, ",");
        ArrayUtils.reverse(eddParts);

        for (String part : eddParts) {
            String[] parts = StringUtils.split(part, "-");

            String dateString = parts[parts.length - 1].trim();
            dateString = dateString.replaceAll("[^\\p{L}\\p{Nd} ]+", "").trim();
            if (dateString.length() <= 2) {
                dateString = parts[parts.length - 2].trim();
                String[] dateStringParts = dateString.split(" ");
                dateString = StringUtils.join(Arrays.copyOf(dateStringParts, dateStringParts.length - 1), " ") + " " + parts[parts.length - 1].trim();
            }

            dateString = dateString.replaceAll("[^\\p{L}\\p{Nd} ]+", "").trim();
            dateString = dateString.replace(" de ", " ");

            String[] dateStringParts = dateString.split(" ");
            List<String> list = Lists.newArrayList(dateStringParts);
            list.removeIf(StringUtils::isBlank);

            dateString = list.get(list.size() - 2) + " " + list.get(list.size() - 1);
            int index = 3;
            while (!RegexUtils.match(dateString, ".*\\d+.*") && list.size() >= index) {
                dateString = list.get(list.size() - index) + " " + dateString;
                index++;
            }


            for (String pattern : formatPatterns) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, country.locale());
                try {
                    Date date = dateFormat.parse(dateString);
                    int years = Dates.getYear(now) - 1970;
                    if (Dates.getField(date, Calendar.MONTH) < Dates.getField(now, Calendar.MONTH)) {
                        years += 1;
                    }

                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    c.add(Calendar.YEAR, years);
                    date = c.getTime();
                    return date;
                } catch (ParseException e) {
                    //LOGGER.error("", e);
                    //ignore
                    //throw new BusinessException(e);
                }
            }

            if (Strings.containsAnyIgnoreCase(part.toLowerCase(), "day", "Werktage", "lavorativi", "ouvrés", "días", "jour", "Tage", "dias", "giorni")) {
                try {
                    String[] dayParts = StringUtils.split(part, "-");
                    String daysString = dayParts[dayParts.length - 1].trim();

                    daysString = daysString.replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
                    int days = IntegerUtils.parseInt(daysString, 1);

                    return afterWorkDays(days, now);

                } catch (Exception e) {
                    //
                }
            }
        }

        if (country != Country.US) {
            return parseEdd(eddText, Country.US, now);
        }

        throw new BusinessException("Cant parse shipping option edd " + eddText);
    }

    public static Date afterWorkDays(int days, Date now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        for (int i = 0; i <= days; ) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            //here even sat and sun are added
            //but at the end it goes to the correct week day.
            //because i is only increased if it is week day
            if (calendar.get(Calendar.DAY_OF_WEEK) <= 5) {
                i++;
            }

        }

        return calendar.getTime();
    }

    public static Date getEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    public static Date getStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        //LocalDateTime startOfDay = localDateTime.atStartOfDay();
        return localDateTimeToDate(startOfDay);
    }

    private static Date localDateTimeToDate(LocalDateTime startOfDay) {
        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }

    public static Date firstDayOfCurrentYear() {
        return firstDayOfYear(Calendar.getInstance().get(Calendar.YEAR));
    }

    public static Date firstDayOfYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        return cal.getTime();
    }


    public static void main(String[] args) {
        LocalTime from = LocalTime.of(12, 0);
        LocalTime to = LocalTime.of(12, 10);
        LocalTime rand = DatetimeHelper.randomTimeBetween(from, to);
        System.out.println(rand);
        System.out.println(DatetimeHelper.parseEdd("1 business day", Country.US, new Date()));
    }
}
