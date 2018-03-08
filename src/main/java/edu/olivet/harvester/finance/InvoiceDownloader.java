package edu.olivet.harvester.finance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.finance.model.DownloadParams;
import edu.olivet.harvester.finance.model.InvoiceTask;
import edu.olivet.harvester.finance.service.InvoiceDownloaderService;
import edu.olivet.harvester.finance.worker.InvoiceDownloadWorker;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.common.ThreadHelper;
import lombok.Setter;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/1/2018 1:37 PM
 */
@Singleton
public class InvoiceDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceDownloader.class);

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();
    @Inject
    MessageListener messageListener;
    @Inject InvoiceDownloaderService invoiceDownloaderService;

    final int JOB_NUMBER = 2;


    public void execute() {
        DownloadParams downloadParams = new DownloadParams();
        downloadParams.setBuyerAccounts(BuyerAccountSettingUtils.load().getAccountSettings().stream().map(BuyerAccountSetting::getBuyerAccount).collect(Collectors.toList()));
        downloadParams.setFromDate(DateUtils.addDays(new Date(), -10));
        downloadParams.setToDate(DateUtils.addDays(new Date(), -1));
        download(downloadParams);
    }


    public void download(List<InvoiceTask> tasks) {
        if (PSEventListener.isRunning()) {
            UITools.error("Other tasks are running, please try later");
            return;
        }

        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
        ProgressUpdater.setTotal(tasks.size());
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.start();

        for (InvoiceTask task : tasks) {
            if (PSEventListener.stopped()) {
                break;
            }
            try {
                Account buyer = BuyerAccountSettingUtils.load().getByEmail(task.getBuyerAccount()).getBuyerAccount();
                Date fromDate = task.getFromDate();
                Date toDate = task.getLastDownloadDate();
                //if (toDate.equals(fromDate)) {
                //    toDate = task.getToDate();
                //}
                invoiceDownloaderService.downloadByCountry(Country.fromCode(task.getCountry()), buyer, fromDate, toDate, task);
                ProgressUpdater.success();
            } catch (Exception e) {
                LOGGER.error("", e);
                ProgressUpdater.failed();
            }
        }

        PSEventListener.end();
    }

    public void download(DownloadParams downloadParams) {
        if (PSEventListener.isRunning()) {
            UITools.error("Other tasks are running, please try later");
            return;
        }

        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.start();

        List<InvoiceDownloadWorker> jobs = new ArrayList<>(JOB_NUMBER);
        final CountDownLatch latch = new CountDownLatch(JOB_NUMBER);
        List<List<Account>> list = ThreadHelper.assign(downloadParams.getBuyerAccounts(), JOB_NUMBER);


        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
        ProgressUpdater.setTotal(downloadParams.getBuyerAccounts().size());

        for (List<Account> accounts : list) {
            jobs.add(new InvoiceDownloadWorker(accounts, downloadParams.getFromDate(), downloadParams.getToDate(), messageListener, latch));
        }

        // SwingWorker线程执行
        for (InvoiceDownloadWorker job : jobs) {
            job.execute();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PSEventListener.end();
    }


}
