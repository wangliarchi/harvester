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
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
    private DropboxAssistant dropboxAssistant;

    @Inject
    public void init() {
        dropboxAssistant = new DropboxAssistant("HarvesterFinance",
                "mK4nH97UhfAAAAAAAAAAFGDWK02NaPGHekvzVn0EbWGGm79VFZ7eY3Xkc_zcnRpa", false, "/");
    }

    public void execute() {
        DownloadParams downloadParams = new DownloadParams();
        downloadParams.setBuyerAccounts(BuyerAccountSettingUtils.load().getAccountSettings().stream().map(it->it.getBuyerAccount()).collect(Collectors.toList()));
        downloadParams.setFromDate(DateUtils.addDays(new Date(),-10));
        downloadParams.setToDate(DateUtils.addDays(new Date(),-3));
        download(downloadParams);
    }
    public void download(DownloadParams downloadParams) {
        for (Account account : downloadParams.getBuyerAccounts()) {
            long start = System.currentTimeMillis();
            messageListener.addMsg(String.format("Starting download invoice  from %s at %s.",
                    account, Dates.toDateTime(start)), InformationLevel.Positive);
            for (Country country : SellerHuntUtils.countriesToHunt()) {
                try {
                    downloadByCountry(country, account, downloadParams.getFromDate(), downloadParams.getToDate());
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    public void downloadByCountry(Country country, Account buyer, Date fromDate, Date toDate) {
        long start = System.currentTimeMillis();
        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        Browser browser = buyerPanel.getBrowserView().getBrowser();

        LoginPage loginPage = new LoginPage(buyerPanel);
        loginPage.execute(null);
        //default to last 6 month
        //todo if before 6 month
        String url = country.baseUrl() + "/gp/your-account/order-history?opt=ab&digitalOrders=1&unifiedOrders=1&returnTo=&orderFilter=months-6";
        JXBrowserHelper.loadPage(browser, url);

        List<String> patterns = Lists.newArrayList("MMMMM dd yyyy", "dd MMMMM yyyy");

        int totalDownloaded = 0;
        while (true) {
            String pageUrl = browser.getURL();
            List<DOMElement> orderBoxes = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box-group.order");
            for (DOMElement orderElement : orderBoxes) {
                String dateString = JXBrowserHelper.text(orderElement, ".order-info .a-col-left .a-span3 .value,.order-info .a-col-left .a-span4 .value");
                dateString = dateString.replaceAll("[^\\p{L}\\p{Nd} ]+", "").trim();
                dateString = dateString.replace(" de ", " ");
                String[] dateStringParts = dateString.split(" ");
                List<String> list = Lists.newArrayList(dateStringParts).stream().map(it->StringUtils.capitalize(it)).collect(Collectors.toList());
                list.removeIf(StringUtils::isBlank);
                dateString = StringUtils.join(list," ");

                Date date = null;
                for (String pattern : patterns) {

                    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, country.locale());
                    try {
                        date = dateFormat.parse(dateString);
                        break;
                    } catch (ParseException e) {
                        //ignore
                    }

                    if(country != Country.US) {
                        dateFormat = new SimpleDateFormat(pattern, Country.US.locale());
                        try {
                            date = dateFormat.parse(dateString);
                            break;
                        } catch (ParseException e) {
                            //ignore
                        }
                    }

                }

                if (date == null) {
                    LOGGER.error("fail to parse date {}", dateString);
                    messageListener.addMsg("fail to parse date " + dateString);
                    continue;
                }

                if (date.before(fromDate)) {
                    LOGGER.error("{} before date {}", date,fromDate);
                    break;
                }
                if (date.after(toDate)) {
                    LOGGER.error("{} after date {}", date,toDate);
                    continue;
                }

                String orderId = JXBrowserHelper.text(orderElement, ".order-info .a-col-right .a-size-mini .value");
                String totalText = JXBrowserHelper.text(orderElement, ".order-info .a-col-left .a-span2 .value");
                BuyerOrderInvoice invoice = new BuyerOrderInvoice();
                invoice.setBuyerEmail(buyer.getEmail());
                invoice.setCountry(country.name());
                invoice.setPurchaseDate(date);
                invoice.setOrderId(orderId);
                String filePath = getLocalFilePath(invoice);
                File file = new File(filePath);
                if (file.exists()) {
                    LOGGER.error("Invoice for order {} from {} {} already downloaded", orderId, buyer.getEmail(), country.name());
                    continue;
                }

                invoice.setCardNo("");
                Money total = Money.fromText(totalText, country);
                invoice.setOrderTotal(total.toUSDAmount().floatValue());

                try {
                    downloadInvoice(invoice, browser);
                    totalDownloaded++;
                } catch (Exception e) {
                    LOGGER.error("", e);
                }

                WaitTime.Short.execute();
            }


            if(LoginPage.needLoggedIn(browser)) {
                loginPage = new LoginPage(buyerPanel);
                loginPage.execute(null);
            }

            JXBrowserHelper.loadPage(browser, pageUrl);
            WaitTime.Short.execute();
            //next page
            DOMElement nextPage = JXBrowserHelper.selectElementByCssSelector(browser, ".a-pagination .a-last a");
            if (nextPage == null) {
                break;
            }
            JXBrowserHelper.insertChecker(browser);
            nextPage.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
            WaitTime.Short.execute();
        }

        messageListener.addMsg("Total " + totalDownloaded + " invoices downloaded for " + buyer.getEmail() +
                " from country " + country + ", took " + Strings.formatElapsedTime(start));
    }

    /**
     * <pre>
     *  扫出的文件保存到本地并上传到Dropbox，
     *  本地：c/orderman/finance/invoice/pdf/国家/buyer编号_buyer账户名@/日期/文件；
     *  例如 c/orderman/finance/invoice/pdf/CA/1_yacheng32@/日期/;
     * 并同时上传到Dropbox中的INVOICE，
     * 文件路径为Apps/Finance/INVOICE/账户编号国家/buyer编号_buyer账户名@/日期（两种表述法合成在一起）/文件；
     * 例如 Apps/Finance/INVOICE/32US/1_yacheng32@/20151203_3-December-2015/104-0172009-2020224.pdf"
     * </pre>
     */
    public String getLocalFilePath(BuyerOrderInvoice invoice) {
        //20151203_3-December-2015
        String dateString = FastDateFormat.getInstance("yyyyMMdd_d-MMMMM-yyyy").format(invoice.getPurchaseDate());
        return Directory.APP_DATA + "/finance/invoice/pdf/" + invoice.getCountry().toUpperCase() + "/" +
                invoice.getBuyerEmail().toLowerCase() + "/" + dateString + "/" + invoice.getOrderId() + ".pdf";
    }

    public String getDropboxFilePath(BuyerOrderInvoice invoice) {
        //20151203_3-December-2015
        String dateString = FastDateFormat.getInstance("yyyyMMdd_d-MMMMM-yyyy").format(invoice.getPurchaseDate());
        return "/2018-ORDER INVOICES/" + Settings.load().getSid() + invoice.getCountry().toUpperCase() + "/" +
                invoice.getBuyerEmail().toLowerCase() + "/" + dateString;
    }

    public void downloadInvoice(BuyerOrderInvoice invoice, Browser browser) {
        String filePath = getLocalFilePath(invoice);

        String invoiceUrl = String.format("%s/gp/css/summary/print.html/ref=oh_aui_pi_o00_?ie=UTF8&orderID=%s",
                Country.valueOf(invoice.getCountry()).baseUrl(), invoice.getOrderId());

        ///gp/css/summary/print.html/ref=oh_aui_ajax_pi?ie=UTF8&orderID=305-3766690-3941945
        JXBrowserHelper.loadPage(browser, invoiceUrl);

        if (!Strings.containsAnyIgnoreCase(browser.getHTML(), "transactions")) {
            LOGGER.error("No invoice available for {} yet", invoice.getOrderId());
            return;
        }
        browser.setPrintHandler(printJob -> {
            File file = new File(filePath);
            Tools.createFileIfNotExist(file);
            PrintSettings settings = printJob.getPrintSettings();
            settings.setPrintToPDF(true);
            settings.setPDFFilePath(file.getAbsolutePath());
            settings.setPrintBackgrounds(true);

            printJob.addPrintJobListener(new PrintJobListener() {
                @Override
                public void onPrintingDone(PrintJobEvent event) {
                    LOGGER.info("Invoice PDF saved to {}", filePath);
                    messageListener.addMsg("Invoice PDF saved to " + filePath);
                    //upload to dropbox
                    try {
                        String dropboxFilePath = getDropboxFilePath(invoice);
                        dropboxAssistant.upload(new File(filePath), dropboxFilePath);
                        LOGGER.info("Uploaded file to {}", dropboxFilePath);
                        messageListener.addMsg("Invoice PDF uploaded to dropbox folder " + dropboxFilePath);
                    } catch (Exception e) {
                        messageListener.addMsg("Invoice PDF " + file + " failed to upload to dropbox folder", InformationLevel.Negative);
                    }
                }
            });
            return PrintStatus.CONTINUE;
        });

        browser.print();
    }


}
