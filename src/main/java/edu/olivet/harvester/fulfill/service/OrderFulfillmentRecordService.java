package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.utils.common.DatetimeHelper;
import org.nutz.dao.Cnd;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/23/2018 2:21 PM
 */
public class OrderFulfillmentRecordService {
    @Inject private DBManager dbManager;

    public float totalCost(String spreadsheetId, Date date) {
        List<OrderFulfillmentRecord> records = dbManager.query(OrderFulfillmentRecord.class,
                Cnd.where("spreadsheetId", "=", spreadsheetId)
                        .and("fulfillDate", ">=", DatetimeHelper.getStartOfDay(date))
                        .and("fulfillDate", "<=", DatetimeHelper.getEndOfDay(date))
                        .asc("fulfillDate"));

        return (float) records.stream().mapToDouble(it -> Float.parseFloat(it.getCost())).sum();
    }

    public static void main(String[] args) {
        OrderFulfillmentRecordService orderFulfillmentRecordService = ApplicationContext.getBean(OrderFulfillmentRecordService.class);
        float total = orderFulfillmentRecordService.totalCost("1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI", Dates.parseDate("02/14/2018"));
        System.out.println(total);
    }
}
