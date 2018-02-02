package edu.olivet.harvester.finance;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.events.PrintJobEvent;
import com.teamdev.jxbrowser.chromium.events.PrintJobListener;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.finance.model.BuyerOrderInvoice;
import edu.olivet.harvester.finance.model.DownloadParams;
import edu.olivet.harvester.finance.worker.InvoiceDownloadWorker;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.hunt.service.HuntWorker;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.ThreadHelper;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.nutz.aop.interceptor.async.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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


    @Inject
    public void init() {

    }

    public void execute() {
        DownloadParams downloadParams = new DownloadParams();
        downloadParams.setBuyerAccounts(BuyerAccountSettingUtils.load().getAccountSettings().stream().map(it -> it.getBuyerAccount()).collect(Collectors.toList()));
        downloadParams.setFromDate(DateUtils.addDays(new Date(), -10));
        downloadParams.setToDate(DateUtils.addDays(new Date(), -3));
        download(downloadParams);
    }

    public void download(DownloadParams downloadParams) {
        final int JOB_NUMBER = 2;
        List<InvoiceDownloadWorker> jobs = new ArrayList<>(JOB_NUMBER);
        final CountDownLatch latch = new CountDownLatch(JOB_NUMBER);
        List<List<Account>> list = ThreadHelper.assign(downloadParams.getBuyerAccounts(), JOB_NUMBER);

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

    }


}
