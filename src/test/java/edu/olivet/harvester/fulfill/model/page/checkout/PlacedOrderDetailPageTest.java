package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.model.Money;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

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
        assertEquals(orderId, "114-2738105-8609821");
    }

    @Test void testParseTotalCost() {
        prepareBrowser();
        assertEquals(placedOrderDetailPage.parseTotalCost(),"10.94");
    }

    @Test void testUKParseTotalCost() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.UK, buyer, 1);
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "UKOrderDetails.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        PlacedOrderDetailPage orderDetailPage = new PlacedOrderDetailPage(buyerPanel);
        Money grandTotal = orderDetailPage.parseTotalCost();
        assertEquals(grandTotal.toString(),"$13.03");
    }

    @Test void testCAParseTotalCost() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.CA, buyer, 1);
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "CAOrderDetails.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        PlacedOrderDetailPage orderDetailPage = new PlacedOrderDetailPage(buyerPanel);
        Money grandTotal = orderDetailPage.parseTotalCost();
        assertEquals(grandTotal.toString(),"$12.73");
    }

    @Test void testParseLastCode() {
        prepareBrowser();
        assertEquals(placedOrderDetailPage.parseLastCode(),"3576");
    }

    @Test
    public void testParseShippingAddress() throws Exception {
        prepareBrowser();
        Address address = placedOrderDetailPage.parseShippingAddress();
        assertEquals(address.getName(),"zhuanyun/720/4907401");
        assertEquals(address.getAddress1(),"501 BROAD AVE STE 13");
        assertEquals(address.getAddress2(),"");
        assertEquals(address.getCity(),"RIDGEFIELD");
        assertEquals(address.getState(),"NJ");
        assertEquals(address.getZip(),"07657-2348");
        assertEquals(address.getCountry(),"United States");


    }

    @Test
    public void testParseItems() throws Exception {
        prepareBrowser();
        Map<String, String> items = placedOrderDetailPage.parseItems();
        String key = items.keySet().stream().collect(Collectors.toList()).get(0);
        assertEquals(key,"1598695274");
        assertEquals(items.get(key).toString(),"$6.95");
    }
}