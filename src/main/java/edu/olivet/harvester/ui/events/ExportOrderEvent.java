package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.export.OrderExporter;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.ChooseMarketplaceDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class ExportOrderEvent implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportOrderEvent.class);

    @Inject
    private OrderExporter orderExporter;


    public void execute() {
        long start = System.currentTimeMillis();
        ChooseMarketplaceDialog dialog = UITools.setDialogAttr(new ChooseMarketplaceDialog());

        if (dialog.isOk()) {
            List<String> selectedMarketplaces = dialog.getSelectedMarketplaceNames();
            orderExporter.setMessagePanel(new ProgressDetail(Actions.ExportOrders));
            orderExporter.exportOrdersForSelectedMarketplaces(selectedMarketplaces);
        }
    }
}
