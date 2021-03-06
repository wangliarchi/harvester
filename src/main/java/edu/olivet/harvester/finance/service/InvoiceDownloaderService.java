package edu.olivet.harvester.finance.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mchange.lang.IntegerUtils;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.PrintSettings;
import com.teamdev.jxbrowser.chromium.PrintStatus;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.finance.model.BuyerOrderInvoice;
import edu.olivet.harvester.finance.model.InvoiceTask;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/7/2018 3:06 PM
 */
public class InvoiceDownloaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceDownloaderService.class);
    private DropboxAssistant dropboxAssistant;

    private static final String DROPBOX_TOKEN = "BgIiNZFmjgAAAAAAAAABhre_O2isId2YhqjXzj_HM80sKQJuSPKJ7e6lZSnmCyS8";
    private static final String DROPBOX_ROOT_DIR = "/INVOICES/";

    @Inject
    MessageListener messageListener;
    @Inject DBManager dbManager;
    @Inject
    Now now;

    @Inject
    public void init() {
        dropboxAssistant = new DropboxAssistant("HarvesterFinanceApp", DROPBOX_TOKEN, false, "/");
    }

    public void downloadByCountry(Country country, Account buyer, Date fromDate, Date toDate) {
        downloadByCountry(country, buyer, fromDate, toDate, null);
    }

    public void downloadByCountry(Country country, Account buyer, Date fromDate, Date toDate, InvoiceTask task) {

        messageListener.displayMsg(String.format("Starting downloading invoice  from %s between %s and %s.", buyer.getEmail(), fromDate, toDate));
        //messageListener.addMsgSeparator();
        long start = System.currentTimeMillis();
        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);
        Browser browser = buyerPanel.getBrowserView().getBrowser();

        try {
            login(buyerPanel);
        } catch (Exception e) {
            LOGGER.error(Strings.getExceptionMsg(e));
            TabbedBuyerPanel.getInstance().removeTab(buyerPanel);
            return;
        }


        try {
            int lastPage = 0;
            if (task != null) {
                lastPage = task.getLastDownloadPage();
            }

            goToTheRightPage(buyerPanel, fromDate, toDate, lastPage);


            int totalDownloaded = 0;
            int pageNo = lastPage;
            Date lastDownloadedDate = null;
            boolean pageEnd = false;
            outerloop:
            while (true) {
                if (PSEventListener.stopped()) {
                    break;
                }

                while (PSEventListener.paused()) {
                    WaitTime.Short.execute();
                }

                if (LoginPage.needLoggedIn(browser)) {
                    try {
                        login(buyerPanel);
                        WaitTime.Normal.execute();
                    } catch (Exception e) {
                        LOGGER.error(Strings.getExceptionMsg(e));
                        TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
                        return;
                    }
                }

                String pageUrl = browser.getURL();

                List<DOMElement> orderBoxes = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box-group.order");
                String totalString = JXBrowserHelper.text(browser, "#controlsContainer .num-orders").replaceAll(Regex.NON_DIGITS.val(), "");
                int totalOrders = IntegerUtils.parseInt(totalString, 0);
                if (orderBoxes.size() < 10) {
                    //try reload
                    JXBrowserHelper.loadPage(browser, pageUrl);
                    if (totalOrders > 10) {
                        JXBrowserHelper.waitUntilVisible(browser, ".a-pagination");
                    } else {
                        WaitTime.Normal.execute();
                    }
                    orderBoxes = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box-group.order");
                }

                pageNo = IntegerUtils.parseInt(JXBrowserHelper.text(browser, ".a-pagination a-selected"), pageNo);
                LOGGER.info("\n{} - {} - {} - orders {}", buyer.getEmail(), pageNo, pageUrl, orderBoxes.size());
                for (DOMElement orderElement : orderBoxes) {
                    String dateString = JXBrowserHelper.text(orderElement, ".order-info .a-col-left .a-span3 .value,.order-info .a-col-left .a-span4 .value");
                    if (StringUtils.isBlank(dateString)) {
                        LOGGER.error("No date info found.");
                        continue;
                    }

                    Date date = null;
                    try {
                        date = parseOrderDate(dateString, country);
                    } catch (Exception e) {
                        LOGGER.error("fail to parse date {}", dateString, e);
                    }

                    if (date == null) {
                        LOGGER.error("fail to parse date {}", dateString);
                        messageListener.addMsg("fail to parse date " + dateString, InformationLevel.Negative);
                        continue;
                    }

                    lastDownloadedDate = date;

                    if (date.before(fromDate)) {
                        LOGGER.info("{} before date {}", date, fromDate);
                        JXBrowserHelper.loadPage(browser, pageUrl);
                        break outerloop;
                    }

                    if (date.after(toDate)) {
                        LOGGER.info("{} after date {}", date, toDate);
                        continue;
                    }


                    try {
                        String orderId = JXBrowserHelper.text(orderElement, ".order-info .a-col-right .a-size-mini .value");
                        //String totalText = JXBrowserHelper.text(orderElement, ".order-info .a-col-left .a-span2 .value");
                        BuyerOrderInvoice invoice = new BuyerOrderInvoice();
                        invoice.setBuyerEmail(buyer.getEmail());
                        invoice.setCountry(country.name());
                        invoice.setPurchaseDate(date);
                        invoice.setOrderId(orderId);
                        String filePath = getLocalFilePath(invoice);
                        File file = new File(filePath);
                        if (file.exists()) {
                            LOGGER.info("Invoice for order {} from {} {} already downloaded", orderId, buyer.getEmail(), country.name());
                            continue;
                        }

                        invoice.setCardNo("");
                        //Money total = Money.fromText(totalText, country);
                        invoice.setOrderTotal(0);


                        downloadInvoice(invoice, browser);
                        totalDownloaded++;
                    } catch (Exception e) {
                        LOGGER.error("", e);
                    }

                    WaitTime.Short.execute();
                }

                if (task != null) {
                    task.setLastDownloadPage(pageNo);
                    if (lastDownloadedDate != null) {
                        task.setLastDownloadDate(lastDownloadedDate);
                    }

                    dbManager.insertOrUpdate(task, InvoiceTask.class);
                }

                //next page
                DOMElement nextPage = JXBrowserHelper.selectElementByCssSelector(browser, ".a-pagination .a-last a");
                if (nextPage == null) {
                    JXBrowserHelper.loadPage(browser, pageUrl);
                    WaitTime.Normal.execute();
                    //next page
                    nextPage = JXBrowserHelper.selectElementByCssSelector(browser, ".a-pagination .a-last a");
                    if (nextPage == null) {
                        LOGGER.info("\n{} - page end", buyer.getEmail());
                        pageEnd = true;
                        break;
                    }
                }


                JXBrowserHelper.insertChecker(browser);
                String nextPageUrl = buyerPanel.getCountry().baseUrl() + nextPage.getAttribute("href");
                nextPage.click();
                JXBrowserHelper.waitUntilNewPageLoaded(browser);


                if (LoginPage.needLoggedIn(browser)) {
                    try {
                        login(buyerPanel);
                        JXBrowserHelper.loadPage(browser, nextPageUrl);
                    } catch (Exception e) {
                        LOGGER.error(Strings.getExceptionMsg(e));
                        return;
                    }
                }

                WaitTime.Normal.execute();
            }


            //pageNo
            if (task != null) {
                task.setLastDownloadPage(pageNo);
                if (lastDownloadedDate != null) {
                    task.setLastDownloadDate(lastDownloadedDate);
                    if (lastDownloadedDate.before(fromDate)) {
                        task.setStatus("Done");
                    }
                }

                if (pageEnd) {
                    task.setStatus("Done");
                }

                dbManager.insertOrUpdate(task, InvoiceTask.class);
            }

            messageListener.addMsg("Total " + totalDownloaded + " invoices downloaded for " + buyer.getEmail() +
                    " from country " + country + ", took " + Strings.formatElapsedTime(start), InformationLevel.Positive);

        } catch (Exception e) {
            LOGGER.error("", e);
        }
        TabbedBuyerPanel.getInstance().removeTab(buyerPanel);
    }


    protected void login(BuyerPanel buyerPanel) {
        LoginPage loginPage = new LoginPage(buyerPanel);
        loginPage.execute(null);
        while (LoginPage.needLoggedIn(buyerPanel.getBrowserView().getBrowser())) {
            loginPage = new LoginPage(buyerPanel);
            loginPage.execute(null);
            WaitTime.Short.execute();
        }
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
        String accountNo = BuyerAccountSettingUtils.load().getByEmail(invoice.getBuyerEmail()).getAccountNo();
        return DROPBOX_ROOT_DIR + Settings.load().getSid() + "/" + invoice.getCountry().toUpperCase() + "/" +
                accountNo + "-" + invoice.getBuyerEmail().toLowerCase() + "/" + dateString;
    }

    public void downloadInvoice(BuyerOrderInvoice invoice, Browser browser) {
        String filePath = getLocalFilePath(invoice);

        String invoiceUrl = String.format("%s/gp/css/summary/print.html/ref=oh_aui_pi_o00_?ie=UTF8&orderID=%s",
                Country.valueOf(invoice.getCountry()).baseUrl(), invoice.getOrderId());

        ///gp/css/summary/print.html/ref=oh_aui_ajax_pi?ie=UTF8&orderID=305-3766690-3941945
        JXBrowserHelper.loadPage(browser, invoiceUrl);

        if (!Strings.containsAnyIgnoreCase(browser.getHTML(), "transaction", "Transaktionen", "Transacciones", "Transazioni")) {
            LOGGER.info("No invoice available for {} yet", invoice.getOrderId());
            return;
        }
        browser.setPrintHandler(printJob -> {
            File file = new File(filePath);
            Tools.createFileIfNotExist(file);
            PrintSettings settings = printJob.getPrintSettings();
            settings.setPrintToPDF(true);
            settings.setPDFFilePath(file.getAbsolutePath());
            settings.setPrintBackgrounds(true);

            printJob.addPrintJobListener(event -> {
                LOGGER.info(invoice.getBuyerEmail() + " invoice PDF saved to {}", filePath);
                messageListener.addMsg(invoice.getBuyerEmail() + " invoice PDF saved to " + filePath);
                //upload to dropbox
                for (int i = 0; i < 3; i++) {
                    try {
                        String dropboxFilePath = getDropboxFilePath(invoice);
                        dropboxAssistant.upload(new File(filePath), dropboxFilePath);
                        LOGGER.info(invoice.getBuyerEmail() + " uploaded file to {}", dropboxFilePath);
                        messageListener.addMsg(invoice.getBuyerEmail() + " invoice PDF uploaded to dropbox folder " + dropboxFilePath);
                        return;
                    } catch (Exception e) {
                        //
                        LOGGER.error("", e);
                        WaitTime.Longest.execute();
                    }
                }

                messageListener.addMsg(invoice.getBuyerEmail() + " invoice PDF " + file + " failed to upload to dropbox folder", InformationLevel.Negative);
            });
            return PrintStatus.CONTINUE;
        });

        browser.print();
    }


    public static Date parseOrderDate(String dateString, Country country) {
        dateString = dateString.replaceAll("[^\\p{L}\\p{Nd} ]+", "").trim();
        dateString = dateString.replace(" de ", " ");
        String[] dateStringParts = dateString.split(" ");
        List<String> list = Lists.newArrayList(dateStringParts).stream().map(StringUtils::capitalize).collect(Collectors.toList());
        list.removeIf(StringUtils::isBlank);
        dateString = StringUtils.join(list, " ");


        List<String> patterns = Lists.newArrayList("MMMMM dd yyyy", "dd MMMMM yyyy");
        Date date;
        for (String pattern : patterns) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, country.locale());
            try {
                date = dateFormat.parse(dateString);
                return date;
            } catch (ParseException e) {
                //LOGGER.error("", e);
                //ignore
            }

            if (country != Country.US) {
                dateFormat = new SimpleDateFormat(pattern, Country.US.locale());
                try {
                    date = dateFormat.parse(dateString);
                    return date;
                } catch (ParseException e) {
                    //ignore
                }

                dateFormat = new SimpleDateFormat(pattern, new Locale("pl", "PL"));
                try {
                    return dateFormat.parse(dateString);
                } catch (ParseException e) {
                    //ignore
                }
            }
        }

        return null;
    }


    public void goToTheRightPage(BuyerPanel buyerPanel, Date fromDate, Date toDate, int lastDownloadedPage) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        Country country = buyerPanel.getCountry();
        String url = country.baseUrl() + "/gp/your-account/order-history?opt=ab&digitalOrders=1&unifiedOrders=1&returnTo=&orderFilter=";
        //default to last 6 month
        int totalDays;
        if (fromDate.after(DateUtils.addMonths(now.get(), -6))) {
            url += "months-6";
            totalDays = 180;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(fromDate);
            int year = calendar.get(Calendar.YEAR);
            url += "year-" + year;
            totalDays = 365;
        }

        //go to the first page
        JXBrowserHelper.loadPage(browser, url);
        WaitTime.Shortest.execute();


        //find page numbers
        List<DOMElement> pagination = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-pagination .a-normal a");

        if (pagination.size() == 0) {
            return;
        }
        DOMElement lastPage = pagination.get(pagination.size() - 1);
        int pages = IntegerUtils.parseInt(lastPage.getInnerText(), 1);

        if (lastDownloadedPage > 0 && lastDownloadedPage <= pages) {
            String pageUrl = url + "&startIndex=" + (lastDownloadedPage - 1) * 10;
            JXBrowserHelper.loadPage(browser, pageUrl);
        }

        //first page
        List<DOMElement> orderBoxes = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box-group.order");
        for (DOMElement orderElement : orderBoxes) {
            String dateString = JXBrowserHelper.text(orderElement, ".order-info .a-col-left .a-span3 .value,.order-info .a-col-left .a-span4 .value");
            Date date = parseOrderDate(dateString, country);
            if (date.before(toDate)) {
                return;
            }
        }
        if (pages <= 5) {
            return;
        }

        //二分法
        int lastPageNo = 0;
        int page = pages / 2;
        int lastType = 2;

        int days = Dates.daysBetween(toDate, now.get());
        if (days <= 5) {
            page = pages * days / totalDays;
        }

        while (true) {
            String pageUrl = url + "&startIndex=" + (page - 1) * 10;
            JXBrowserHelper.loadPage(browser, pageUrl);


            if (LoginPage.needLoggedIn(browser)) {
                try {
                    login(buyerPanel);
                    JXBrowserHelper.loadPage(browser, pageUrl);
                    WaitTime.Normal.execute();
                } catch (Exception e) {
                    LOGGER.error(Strings.getExceptionMsg(e));
                    return;
                }
            }


            WaitTime.Shortest.execute();

            messageListener.addMsg(buyerPanel.getBuyer().getEmail() + " currently on page " + page + ", total " + pages + " pages");

            lastType = checkPurchaseDateOnPage(buyerPanel, toDate, lastType);


            if (lastType == 0) {
                break;
            }

            int newPage;
            if (lastType == 2) {
                //lastPageNo
                newPage = page + Math.abs(page - lastPageNo) / 2;
                if (newPage <= page) {
                    newPage = page + 1;
                    lastType = 1;
                }
            } else if (lastType == -2) {
                newPage = page - Math.abs(page - lastPageNo) / 2;
                if (newPage >= page) {
                    newPage = page - 1;
                    lastType = -1;
                }
            } else if (lastType == 1) {
                newPage = page + 1;
            } else {
                newPage = page - 1;
            }

            if (newPage <= 1 || newPage >= pages - 1) {
                break;
            }

            lastPageNo = page;
            page = newPage;
        }

    }

    public int checkPurchaseDateOnPage(BuyerPanel buyerPanel, Date toDate, int lastType) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        Country country = buyerPanel.getCountry();
        List<DOMElement> orderBoxes = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box-group.order");


        //2017-04-01 - 2017-05-01
        //check first order
        String dateString = JXBrowserHelper.text(orderBoxes.get(0), ".order-info .a-col-left .a-span3 .value,.order-info .a-col-left .a-span4 .value");
        Date firstPurchaseDate = parseOrderDate(dateString, country);

        dateString = JXBrowserHelper.text(orderBoxes.get(orderBoxes.size() - 1), ".order-info .a-col-left .a-span3 .value,.order-info .a-col-left .a-span4 .value");
        Date lastPurchaseDate = parseOrderDate(dateString, country);
        messageListener.addMsg("first order " + firstPurchaseDate + ", last order " + lastPurchaseDate + ", type " + lastType);
        //last 2017-07-01, 还要往后翻，继续2分
        if (lastPurchaseDate.after(toDate)) {
            return 2;
        }

        //first 2017-03-01，要往前翻，继续2分
        if (firstPurchaseDate.before(toDate)) {
            if (lastType == 1) {
                return 0;
            }

            return -2;
        }

        //first 2017-05-01,往前翻一页
        if (firstPurchaseDate.equals(toDate)) {
            if (lastType == 1) {
                return 0;
            }
            return -1;
        }

        //first 2017-05-02
        if (firstPurchaseDate.after(toDate)) {
            if (lastType == -1) {
                return 0;
            }

            //last 2017-05-01
            if (lastPurchaseDate.equals(toDate) || lastPurchaseDate.before(toDate)) {
                return 0;
            }

            //last 2017-05-02，往后翻
            return 1;
        }

        return 0;
    }

    public static void main(String[] args) {
        InvoiceDownloaderService invoiceDownloaderService = ApplicationContext.getBean(InvoiceDownloaderService.class);
        Date date = invoiceDownloaderService.parseOrderDate("27 października 2017", Country.DE);
        System.out.println(date);
    }
}
