package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.service.InventoryReportManager;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.ChooseMarketplaceDialog;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class DownloadInventoryEvent implements HarvesterUIEvent {

    //private static final Logger LOGGER = LoggerFactory.getLogger(ExportOrderEvent.class);


    @Inject
    private InventoryReportManager inventoryReportManager;



    public void execute() {
        ChooseMarketplaceDialog dialog = UITools.setDialogAttr(new ChooseMarketplaceDialog());
        MessagePanel messagePanel = new ProgressDetail(Actions.DownloadInventory);
        if (dialog.isOk()) {
            List<Country> countries = dialog.getSelectedMarketplaceNames();
            messagePanel.displayMsg(countries + " selected to download inventory from amazon seller center.");
            inventoryReportManager.setMessagePanel(messagePanel);
            for (Country country : countries) {
                Long start = System.currentTimeMillis();
                try {
                    inventoryReportManager.download(country.zoneCode());
                    messagePanel.displayMsg("finished sync inventory in " + Strings.formatElapsedTime(start));
                } catch (Exception e) {
                    messagePanel.displayMsg(Strings.getExceptionMsg(e), InformationLevel.Negative);
                }
            }
        }
    }
}
