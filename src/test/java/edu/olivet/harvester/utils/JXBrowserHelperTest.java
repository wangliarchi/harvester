package edu.olivet.harvester.utils;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

public class JXBrowserHelperTest extends BaseTest {

    private BuyerPanel buyerPanel;

    private void prepareBrowser() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
    }
    @Test
    public void waitUntilVisible() {
        prepareBrowser();
        String pageUrl = "28.1-AmazoncomCheckout.html";
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + pageUrl)));
        WaitTime.Short.execute();

        String continueBtnSelector = ".save-gift-button-box .a-button-primary .a-button-text," +
                ".save-gift-button-box .a-button-primary .a-button-input," +
                ".popover-gift-bottom .a-button-primary .a-button-text," +
                ".popover-gift-bottom .a-button-primary .a-button-input";

        JXBrowserHelper.waitUntilVisible(browser,continueBtnSelector);
    }

}