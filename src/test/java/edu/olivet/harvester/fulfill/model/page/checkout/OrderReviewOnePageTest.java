package edu.olivet.harvester.fulfill.model.page.checkout;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.service.addressvalidator.USPSAddressValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 1:07 PM
 */
public class OrderReviewOnePageTest extends BaseTest {

    OrderReviewOnePage orderReviewOnePage;
    Browser browser;

    @Inject
    USPSAddressValidator uspsAddressValidator;
    public void prepareBrowser() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        orderReviewOnePage = new OrderReviewOnePage(buyerPanel);
        browser = buyerPanel.getBrowserView().getBrowser();

    }
    @Test
    public void testReviewShippingAddress() throws Exception {
        prepareBrowser();
        Order order = prepareOrder();
        order.ship_country = "Unted States";
        order.ship_state = "NY";
        order.ship_city = "Wingdale";
        order.ship_address_1 = "836 Birkshire road";
        order.ship_address_2 = "";
        order.ship_zip = "10594";
        orderReviewOnePage.getBuyerPanel().setOrder(order);

        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "verifyAddressOnePageUS.html")));
        WaitTime.Short.execute();
        orderReviewOnePage.reviewShippingAddress(uspsAddressValidator);

    }

    @Test
    public void testPlaceOrder() throws Exception {
    }

}