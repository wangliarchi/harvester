package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

public class PaymentMethodOnePageTest extends BaseTest {
    private BuyerPanel buyerPanel;

    private void prepareBrowser() {

        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
    }

    @Test
    public void enterPromoCode() {
        prepareBrowser();
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        String pageUrl = "promocodeinvalid.html";
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + pageUrl)));
        WaitTime.Shortest.execute();
        DOMElement error = JXBrowserHelper.selectVisibleElement(browser, "#spc-gcpromoinput.a-form-error,#gcpromoinput.a-form-error");
        assertNotNull(error);
    }

}