package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/9/18 2:02 PM
 */
public class ShoppingCartPageTest extends BaseTest {
    ShoppingCartPage shoppingCartPage;
    BuyerPanel buyerPanel;

    public void prepareBrowser() {

        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        shoppingCartPage = new ShoppingCartPage(buyerPanel);


    }

    @Test
    public void testProcessToCheckout() throws Exception {
        prepareBrowser();
        String pageUrl = "ShoppingCartWithPopup.html";
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + pageUrl)));
        WaitTime.Short.execute();

        shoppingCartPage.processToCheckout();
    }


}