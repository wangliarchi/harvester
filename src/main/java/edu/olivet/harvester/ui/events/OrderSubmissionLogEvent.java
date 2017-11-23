package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.ui.Actions;
import org.nutz.dao.Cnd;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 11:05 AM
 */
public class OrderSubmissionLogEvent implements HarvesterUIEvent {

    @Inject
    private DBManager dbManager;

    @Override
    public void excute() {
        List<OrderFulfillmentRecord> list = dbManager.query(OrderFulfillmentRecord.class,
                Cnd.where("orderId", "!=", "").desc("fulfillDate"));
        ListModel<OrderFulfillmentRecord> dialog = new ListModel<>(Actions.OrderSubmissionLog.label(), list, OrderFulfillmentRecord.COLUMNS, null, OrderFulfillmentRecord.WIDTHS);
        UITools.displayListDialog(dialog);
    }
}
