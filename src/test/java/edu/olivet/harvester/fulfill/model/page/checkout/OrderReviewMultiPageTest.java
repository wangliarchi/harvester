package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.ShippingEnums;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 4:11 PM
 */
public class OrderReviewMultiPageTest extends BaseTest {
    private OrderReviewMultiPage orderReviewMultiPage;
    private Browser browser;

    private void prepareBrowser() {

        Account buyer = new Account("abc@test.com/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        orderReviewMultiPage = new OrderReviewMultiPage(buyerPanel);

        browser = buyerPanel.getBrowserView().getBrowser();

    }

    @Test
    public void testChangeShippingAddress() throws Exception {
    }

    @Test
    public void testChangePaymentMethod() throws Exception {
    }

    @Test
    public void testChangeShippingMethod() throws Exception {
        prepareBrowser();
        Order order = prepareOrder();
        order.estimated_delivery_date = "2017-11-13 2017-11-21";
        orderReviewMultiPage.getBuyerPanel().setOrder(order);
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "9.1-PlaceYourOrder-Amazon.comCheckout.html")));
        WaitTime.Short.execute();

        orderReviewMultiPage.changeShippingMethod();


        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "1-A.html")));
        WaitTime.Short.execute();

        orderReviewMultiPage.changeShippingMethod();

    }

    @Test
    public void testCheckTotalCost() {
        prepareBrowser();
        Order order = prepareOrder();
        order.estimated_delivery_date = "2017-11-13 2017-11-21";
        orderReviewMultiPage.getBuyerPanel().setOrder(order);
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "9.1-PlaceYourOrder-Amazon.comCheckout.html")));
        WaitTime.Short.execute();

        orderReviewMultiPage.checkTotalCost(order);
    }

    @Test
    public void testCheckShippingCost() {
        prepareBrowser();
        Order order = prepareOrder();
        order.estimated_delivery_date = "2017-11-13 2017-11-21";
        order.ship_country = "United States";
        order.ship_state = "FL";
        order.shippingSpeed = ShippingEnums.ShippingSpeed.Standard;
        orderReviewMultiPage.getBuyerPanel().setOrder(order);
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "9.1-PlaceYourOrder-Amazon.comCheckout.html")));
        WaitTime.Short.execute();

        orderReviewMultiPage.checkShippingCost(order);
    }

    @Test
    public void testUpdateQty() throws Exception {
    }

    @Test
    public void testPlaceOrder() throws Exception {
    }

}