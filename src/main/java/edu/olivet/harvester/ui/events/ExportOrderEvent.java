package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.export.OrderExporter;
import edu.olivet.harvester.export.model.OrderExportParams;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.OrderExportSettingDialog;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class ExportOrderEvent implements HarvesterUIEvent {

    //private static final Logger LOGGER = LoggerFactory.getLogger(ExportOrderEvent.class);


    @Inject
    private OrderExporter orderExporter;


    public void execute() {
        OrderExportSettingDialog dialog = UITools.setDialogAttr(new OrderExportSettingDialog());

        if (dialog.isOk()) {
            OrderExportParams orderExportParams  = dialog.getOrderExportParams();
            orderExporter.setMessagePanel(new ProgressDetail(Actions.ExportOrders));
            orderExporter.exportOrders(orderExportParams);
        }
    }
}
