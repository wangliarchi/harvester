package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.finance.InvoiceDownloader;
import edu.olivet.harvester.finance.model.DownloadParams;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.DownloadInvoiceDialog;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class DownloadInvoiceEvent implements HarvesterUIEvent {

    //private static final Logger LOGGER = LoggerFactory.getLogger(ExportOrderEvent.class);


    @Inject
    private InvoiceDownloader invoiceDownloader;


    public void execute() {
        DownloadInvoiceDialog dialog = UITools.setDialogAttr(new DownloadInvoiceDialog());

        if (dialog.isOk()) {
            DownloadParams downloadParams  = dialog.getDownloadParams();
            //invoiceDownloader.setMessagePanel(new ProgressDetail(Actions.DownloadInvoice));
            invoiceDownloader.download(downloadParams);
        }
    }
}
