package edu.olivet.harvester.finance.worker;

import com.google.common.collect.Lists;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.PrintSettings;
import com.teamdev.jxbrowser.chromium.PrintStatus;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.finance.model.BuyerOrderInvoice;
import edu.olivet.harvester.finance.service.InvoiceDownloaderService;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/2/2018 10:26 AM
 */
public class InvoiceDownloadWorker extends SwingWorker<Void, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceDownloadWorker.class);
    private final MessageListener messageListener;
    private final CountDownLatch latch;
    private final List<Account> buyers;
    private final Date fromDate;
    private final Date toDate;
    private final InvoiceDownloaderService invoiceDownloaderService;


    public InvoiceDownloadWorker(List<Account> buyers, Date fromDate, Date toDate, MessageListener messageListener, CountDownLatch latch) {
        this.messageListener = messageListener;
        this.latch = latch;
        this.buyers = buyers;
        this.fromDate = fromDate;
        this.toDate = toDate;
        invoiceDownloaderService = ApplicationContext.getBean(InvoiceDownloaderService.class);

    }

    @Override
    protected Void doInBackground() throws Exception {
        for (Account buyer : buyers) {
            long start = System.currentTimeMillis();
            publish(String.format("Starting downloading invoice  from %s at %s.", buyer.getEmail(), Dates.toDateTime(start)));
            for (Country country : SellerHuntUtils.countriesToHunt()) {
                try {
                    invoiceDownloaderService.downloadByCountry(country, buyer, fromDate, toDate);
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
                WaitTime.Shortest.execute();
            }
            publish(String.format("Finished downloading invoice  from %s, took %s.", buyer.getEmail(), Strings.formatElapsedTime(start)));
        }
        return null;
    }

    @Override
    protected void process(final List<String> chunks) {
        messageListener.addMsg(chunks, InformationLevel.Positive);
    }

    @Override
    protected void done() {
        latch.countDown();
    }


}
