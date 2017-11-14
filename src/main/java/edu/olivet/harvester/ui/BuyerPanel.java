package edu.olivet.harvester.ui;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.Cookie;
import com.teamdev.jxbrowser.chromium.CookieStorage;
import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 9:16 AM
 */
public class BuyerPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyerPanel.class);

    /**
     * 面板显示编号，从1开始
     */
    @Getter
    private final int id;

    @Getter
    private final Country country;

    @Getter
    private final Account buyer;

    @Getter
    private BrowserView browserView;

    @Getter
    @Setter
    /**
     * current proccessing order
     */
    private Order order;

    public BuyerPanel(int id, Country country, Account buyer, double zoomLevel) {
        super(new BorderLayout());

        this.id = id;
        this.country = country;
        this.buyer = buyer;
        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel);
        this.add(browserView, BorderLayout.CENTER);

        //to home page
        //toHomePage();
    }

    public BuyerPanel(Order order) {
        this(0, OrderHelper.getFulfillementCountry(order), OrderHelper.getBuyer(order), 1);
    }

    private String profilePathName() {
        return this.buyer.key() + Constants.HYPHEN + this.id;
    }

    public void setCookies(Country country, java.util.List<Cookie> cookies) {
        Browser browser = browserView.getBrowser();

        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(country.baseUrl()));
        String url = browser.getURL();
        for (Cookie cookie : cookies) {
            browser.getCookieStorage().setCookie(
                    url,
                    cookie.getName(),
                    cookie.getValue(),
                    cookie.getDomain(),
                    cookie.getPath(),
                    cookie.getExpirationTime(),
                    cookie.isSecure(),
                    cookie.isHTTPOnly()
            );
        }
    }

    public @Nullable CookieStorage loginBuyerAccount() {
        Browser browser = browserView.getBrowser();
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(country.baseUrl()));

        return null;
    }

    public void toHomePage() {
        Browser browser = browserView.getBrowser();
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(country.baseUrl()));

    }

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");


    private String text(DOMElement ele, String selector) {
        DOMElement child = ele.findElement(By.cssSelector(selector));
        return child == null ? StringUtils.EMPTY : child.getTextContent().trim();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setTitle("Seller Panel Demo");
        frame.setVisible(true);
        Settings settings = Settings.load();
        BuyerPanel buyerPanel = new BuyerPanel(1, Country.US, settings.getConfigByCountry(Country.US).getBuyer(), -1.3);
        frame.getContentPane().add(buyerPanel);

        UITools.setDialogAttr(frame, true);
        buyerPanel.toHomePage();


        // System.exit(0);
    }
}