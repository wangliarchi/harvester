package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.feeds.model.OrderConfirmationLog;
import edu.olivet.harvester.ui.Actions;
import org.nutz.dao.Cnd;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 11:05 AM
 */
public class OrderConfirmationHistoryEvent implements HarvesterUIEvent {

    @Inject
    private DBManager dbManager;

    @Override
    public void execute() {
        List<OrderConfirmationLog> list = dbManager.query(OrderConfirmationLog.class,
                Cnd.where("context", "!=", "").desc("uploadTime"));
        ListModel<OrderConfirmationLog> dialog = new ListModel<>(Actions.OrderConfirmationHistory.label(), list, OrderConfirmationLog.COLUMNS, null, OrderConfirmationLog.WIDTHS);
        UITools.displayListDialog(dialog);
    }
}
