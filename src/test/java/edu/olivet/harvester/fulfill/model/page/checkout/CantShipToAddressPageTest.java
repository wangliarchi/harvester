package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;


public class CantShipToAddressPageTest extends BaseTest {

    private CantShipToAddressPage cantShipToAddressPage;
    private BuyerPanel buyerPanel;

    private void prepareBrowser() {

        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        cantShipToAddressPage = new CantShipToAddressPage(buyerPanel);


    }

    @Test(expectedExceptions = OrderSubmissionException.class)
    public void execute() {
        prepareBrowser();
        String pageUrl = "CantShip1.html";
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + pageUrl)));
        WaitTime.Short.execute();

        cantShipToAddressPage.execute(null);
    }

    @Test(expectedExceptions = OrderSubmissionException.class)
    public void executeSelectBox() {
        prepareBrowser();
        String pageUrl = "CantShip.html";
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + pageUrl)));
        WaitTime.Short.execute();
        Order order = prepareOrder();
        order.recipient_name = "valerie lippincott";
        cantShipToAddressPage.execute(order);
    }

}