package edu.olivet.harvester.finance;

import com.dropbox.core.DbxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.finance.model.BuyerOrderInvoice;
import edu.olivet.harvester.finance.model.DownloadParams;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/1/2018 1:37 PM
 */
@Singleton
public class InvoiceDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceDownloader.class);

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    private DropboxAssistant dropboxAssistant;

    @Inject
    public void init() {
        dropboxAssistant = new DropboxAssistant("Harvester",
                "mK4nH97UhfAAAAAAAAAAEY-qCoMmyzdSMGhF489kylnDO5RGUtxIgl6BojCG1Rmj", false, "/");
    }

    public void download(DownloadParams downloadParams) {

        for (Account account : downloadParams.getBuyerAccounts()) {
            long start = System.currentTimeMillis();
            messagePanel.wrapLineMsg(String.format("Starting download invoice  from %s at %s.",
                    account, Dates.toDateTime(start)), LOGGER);
            for (Country country : SellerHuntUtils.countriesToHunt()) {
                downloadByCountry(country, account, downloadParams.getFromDate(), downloadParams.getToDate());
                break;
            }
        }
    }

    public void downloadByCountry(Country country, Account buyer, Date fromDate, Date toDate) {
        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        Browser browser = buyerPanel.getBrowserView().getBrowser();

        LoginPage loginPage = new LoginPage(buyerPanel);
        loginPage.execute(null);
        //default to last 6 month
        //todo if before 6 month
        String url = country.baseUrl() + "/gp/your-account/order-history?opt=ab&digitalOrders=1&unifiedOrders=1&returnTo=&orderFilter=months-6";
        JXBrowserHelper.loadPage(browser, url);

        String pattern = "MMMMM dd, yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, country.locale());
        while (true) {
            List<DOMElement> orderBoxes = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box-group.order");
            for (DOMElement orderElement : orderBoxes) {
                String dateString = JXBrowserHelper.text(orderElement, ".order-info .a-col-left .a-span3 .value");
                Date date;
                try {
                    date = dateFormat.parse(dateString);
                } catch (ParseException e) {
                    //
                    LOGGER.error("fail to parse date {}", dateString);
                    continue;
                }

                if (date.after(toDate) || date.before(fromDate)) {
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
                }

                invoice.setCardNo("");
                Money total = Money.fromText(totalText, country);
                invoice.setOrderTotal(total.toUSDAmount().floatValue());

                downloadInvoice(invoice, browser);

                return;

            }
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
        return Directory.APP_DATA + "/finance/invoice/pdf/" + invoice.getCountry() + "/" +
                invoice.getBuyerEmail() + "/" + dateString + "/" + invoice.getOrderId() + ".pdf";
    }

    public String getDropboxFilePath(BuyerOrderInvoice invoice) {
        //20151203_3-December-2015
        String dateString = FastDateFormat.getInstance("yyyyMMdd_d-MMMMM-yyyy").format(invoice.getPurchaseDate());
        return "Apps/Finance/INVOICE/" + Settings.load().getSid() + invoice.getCountry() + "/" +
                invoice.getBuyerEmail() + "/" + dateString + "/" + invoice.getOrderId() + ".pdf";
    }

    public void downloadInvoice(BuyerOrderInvoice invoice, Browser browser) {
        String filePath = getLocalFilePath(invoice);

        String invoiceUrl = String.format("%s/gp/css/summary/print.html/ref=oh_aui_pi_o00_?ie=UTF8&orderID=%s",
                Country.valueOf(invoice.getCountry()).baseUrl(), invoice.getOrderId());
        JXBrowserHelper.loadPage(browser, invoiceUrl);

        if (!Strings.containsAnyIgnoreCase(browser.getHTML(), "transactions")) {
            LOGGER.error("No invoice available for {} yet", invoice.getOrderId());
            return;
        }
        JXBrowserHelper.printToPDF(browser, filePath);
        LOGGER.info("Invoice PDF saved to {}", filePath);

        //upload to dropbox
        //
        try {
            dropboxAssistant.upload(new File(filePath), getDropboxFilePath(invoice));
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
