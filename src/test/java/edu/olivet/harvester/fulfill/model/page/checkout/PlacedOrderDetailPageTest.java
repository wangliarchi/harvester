package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.ui.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 5:55 PM
 */
public class PlacedOrderDetailPageTest extends BaseTest {
    PlacedOrderDetailPage placedOrderDetailPage;


    public void prepareBrowser() {
        String orderDetailPage = "OrderDetails.html";
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        placedOrderDetailPage = new PlacedOrderDetailPage(buyerPanel);

        Browser browser = buyerPanel.getBrowserView().getBrowser();
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + orderDetailPage)));
        WaitTime.Short.execute();
    }

    @Test
    public void testParseOrderId() throws Exception {
        prepareBrowser();
        String orderId = placedOrderDetailPage.parseOrderId();
        assertEquals(orderId, "113-8838701-9669824");
    }

    @Test void testParseTotalCost() {
        prepareBrowser();
        assertEquals(placedOrderDetailPage.parseTotalCost(),"10.58");
    }

    @Test void testParseLastCode() {
        prepareBrowser();
        assertEquals(placedOrderDetailPage.parseLastCode(),"1003");
    }

}