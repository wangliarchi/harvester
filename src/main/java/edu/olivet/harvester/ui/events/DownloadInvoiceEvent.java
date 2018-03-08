package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.finance.InvoiceDownloader;
import edu.olivet.harvester.finance.model.DownloadParams;
import edu.olivet.harvester.finance.model.InvoiceTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.DownloadInvoiceDialog;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.nutz.dao.Cnd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class DownloadInvoiceEvent implements HarvesterUIEvent {

    //private static final Logger LOGGER = LoggerFactory.getLogger(ExportOrderEvent.class);


    @Inject
    private InvoiceDownloader invoiceDownloader;

    @Inject DBManager dbManager;

    public void runTasks() {
        List<InvoiceTask> list = dbManager.query(InvoiceTask.class, Cnd.where("status", "!=", "Done"));
        if (CollectionUtils.isEmpty(list)) {
            UITools.error("No pending tasks found.");
            return;
        }
        invoiceDownloader.download(list);
    }

    public void list() {
        List<InvoiceTask> list = dbManager.query(InvoiceTask.class, Cnd.where("status", "!=", "").desc("dateCreated"));
        ListModel<InvoiceTask> dialog = new ListModel<>(Actions.InvoiceTasks.label(), list, InvoiceTask.COLUMNS, null, InvoiceTask.WIDTHS);
        UITools.displayListDialog(dialog);
    }

    public void execute() {
        DownloadInvoiceDialog dialog = UITools.setDialogAttr(new DownloadInvoiceDialog());

        if (dialog.isOk()) {
            DownloadParams downloadParams = dialog.getDownloadParams();
            //invoiceDownloader.setMessagePanel(new ProgressDetail(Actions.DownloadInvoice));
            List<InvoiceTask> tasks = new ArrayList<>();
            if (downloadParams.isTaskMode()) {
                for (Account account : downloadParams.getBuyerAccounts()) {
                    List<InvoiceTask> list = dbManager.query(InvoiceTask.class, Cnd.where("buyerAccount", "=", account.getEmail())
                            .and("fromDate", "=", downloadParams.getFromDate())
                            .and("toDate", "=", downloadParams.getToDate())
                            .and("status", "!=", "Done"));

                    if (CollectionUtils.isNotEmpty(list)) {
                        boolean confirmed = UITools.confirmed("Same task existed, are you sure you want to create again?");
                        if (!confirmed) {
                            continue;
                        }
                    }

                    for (Country country : SellerHuntUtils.countriesToHunt()) {
                        InvoiceTask invoiceTask = new InvoiceTask();
                        invoiceTask.setCountry(country.name());
                        invoiceTask.setBuyerAccount(account.getEmail());
                        invoiceTask.setFromDate(downloadParams.getFromDate());
                        invoiceTask.setToDate(downloadParams.getToDate());
                        invoiceTask.setLastDownloadDate(downloadParams.getToDate());
                        invoiceTask.setStatus("New");
                        invoiceTask.setId(DigestUtils.sha256Hex(invoiceTask.toString()));
                        invoiceTask.setDateCreated(new Date());
                        dbManager.insert(invoiceTask, InvoiceTask.class);

                        tasks.add(invoiceTask);
                    }
                }

                if (CollectionUtils.isNotEmpty(tasks)) {
                    if (UITools.confirmed("Tasks created, start to run now?")) {
                        invoiceDownloader.download(tasks);
                    }
                } else {
                    UITools.error("No tasks created");
                }
            } else {
                invoiceDownloader.download(downloadParams);
            }
        }
    }
}
