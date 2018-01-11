package edu.olivet.harvester.feeds.helper;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/11/2018 9:59 PM
 */
public class ShipDateUtils {
    public String getSheetNameByDate(long millis) {
        return FastDateFormat.getInstance("MM/dd").format(millis);
    }

    @Inject Now now;

    /**
     * <pre>
     * 1. 如果sheet date 比latest expected shipping date 晚，使用 earliest expected shipping date
     * 原因是 有导单比较晚的情况
     * 2. 如果purchase date 比sheet date 晚， 使用purchase date
     * 3. 其他情况直接使用sheet date
     * </pre>
     */
    public Date getShipDate(Order order, Date defaultDate) {
        Date shipDate = defaultDate;
        String[] shipDates = new String[2];


        if (StringUtils.isNotBlank(order.expected_ship_date)) {
            if (Strings.containsAnyIgnoreCase(order.expected_ship_date, " - ")) {
                shipDates = order.expected_ship_date.split("\\s-\\s");
            } else {
                shipDates = StringUtils.split(order.expected_ship_date, " ");
            }
        }

        //如果sheet date 比latest expected shipping date 晚，使用 earliest expected shipping date
        if (shipDates.length == 2 && StringUtils.isNotBlank(shipDates[0]) && StringUtils.isNotBlank(shipDates[1])) {
            try {
                Date earliestShipDate = Dates.parseDate(shipDates[0]);
                Date latestShipDate = Dates.parseDate(shipDates[1]);
                if (shipDate.after(DateUtils.addDays(latestShipDate, -1))) {
                    shipDate = earliestShipDate;
                }
            } catch (Exception e) {
                //
            }
        }

        //if ship date is earlier than purchased date...
        try {
            Date purchaseDate = order.getPurchaseDate();
            if (purchaseDate.after(shipDate)) {
                //shipDate = DateUtils.addHours(purchaseDate, 12);
                shipDate = Dates.beginOfDay(new DateTime(purchaseDate)).toDate();
                shipDate = DateUtils.addHours(shipDate, 8);
            }
        } catch (Exception e) {
            //
        }

        //if ship date is earlier than NOW
        Date nowDate = now.get();
        if (shipDate.after(nowDate)) {
            //shipDate = DateUtils.addHours(nowDate, -3);
            shipDate = Dates.beginOfDay(new DateTime(nowDate)).toDate();
            shipDate = DateUtils.addHours(shipDate, 8);
        }
        return shipDate;
    }


    public String getShipDateString(Order order, Date defaultDate) {
        return formatShipDate(getShipDate(order, defaultDate));
    }

    public String formatShipDate(Date shipDate) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        return df.format(shipDate);
    }
}
