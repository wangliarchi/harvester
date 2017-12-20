package edu.olivet.harvester.ui.panel;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

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

    private double zoomLevel;
    @Getter
    @Setter
    /**
     * current processing order
     */
    private Order order;
    private int pid;

    public BuyerPanel(int id, Country country, Account buyer, double zoomLevel) {
        super(new BorderLayout());

        this.id = id;
        this.country = country;
        this.buyer = buyer;
        this.zoomLevel = zoomLevel;
        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel);
        this.add(browserView, BorderLayout.CENTER);
    }

    public BuyerPanel(Order order) {
        this(0, OrderCountryUtils.getFulfillmentCountry(order), OrderBuyerUtils.getBuyer(order), 1);
    }

    private String profilePathName() {
        return this.buyer.key() + Constants.HYPHEN + this.id;
    }

    public void recreateBrowser() {
        try {
            int pid = (int) browserView.getBrowser().getRenderProcessInfo().getPID();
            browserView.getBrowser().dispose();
            killProcess(pid);
        } catch (Exception e) {
            //
            LOGGER.error("", e);
        }

        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel);
        toHomePage();
        WaitTime.Short.execute();
    }

    public void toHomePage() {
        Browser browser = browserView.getBrowser();
        JXBrowserHelper.loadPage(browser, country.baseUrl());
    }

    public void toWelcomePage() {
        Browser browser = browserView.getBrowser();
        String html = Configs.loadByFullPath("/welcome.html");
        browser.loadHTML(html);

    }

    private void killProcess(int pid) {
        Runtime rt = Runtime.getRuntime();
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            try {
                rt.exec("taskkill " + pid);
            } catch (IOException e) {
                LOGGER.error("",e);
            }
        } else {
            try {
                rt.exec("kill -9 " + pid);
            } catch (IOException e) {
                LOGGER.error("",e);
            }
        }
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