package edu.olivet.harvester.fulfill.model.page.checkout;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.service.AddressValidatorService;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 1:07 PM
 */
public class OrderReviewOnePageTest extends BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReviewOnePageTest.class);
    private OrderReviewOnePage orderReviewOnePage;
    private Browser browser;

    @Inject private
    AddressValidatorService addressValidatorService;
    @Inject private
    SheetAPI sheetAPI;
    @Inject private
    OrderService orderService;
    private List<Order> orders;
    private Map<String, List<Order>> orderMap;
    private BuyerPanel buyerPanel;
    private File[] directories;

    private void prepareData() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        orderReviewOnePage = new OrderReviewOnePage(buyerPanel);
        browser = buyerPanel.getBrowserView().getBrowser();

        final String SPREADSHEET_ID = "1U_qeXOm5qvCLzrfhX96sJSTPKUXzP57_owTL12XmE9U";

        Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(SPREADSHEET_ID);
        orders = orderService.fetchOrders(spreadsheet, Range.between(Dates.parseDate("11/05/2017"), Dates.parseDate("11/17/2017")));
        orderMap = orders.stream().collect(Collectors.groupingBy(Order::getOrder_id));
        //
        directories = new File(TEST_DATA_ROOT + File.separator + "pages").listFiles(file -> file.isDirectory() && StringUtils.contains(file.getName(), "720US"));

    }

    @Test
    public void testReviewShippingAddress() {
        prepareData();

        for (File dir : directories) {
            File[] files = dir.listFiles(file -> file.isFile() && StringUtils.contains(file.getName(), "OrderReview.html"));
            assert files != null;
            for (File file : files) {

                String orderId = RegexUtils.getMatched(file.getName(), RegexUtils.Regex.AMAZON_ORDER_NUMBER);
                if (orderMap.containsKey(orderId)) {
                    Order order = orderMap.get(orderId).get(0);
                    buyerPanel.setOrder(order);
                    browser.loadHTML(Tools.readFileToString(file));
                    WaitTime.Short.execute();
                    try {
                        Address address = orderReviewOnePage.parseEnteredAddress();
                        if (!addressValidatorService.verify(Address.loadFromOrder(order), address)) {
                            LOGGER.error("Address failed verification. " + dir.getName() + "/" + file.getName() + " Entered " + address + ", original " + Address.loadFromOrder(order));
                        }
                    } catch (Exception e) {
                        //
                        LOGGER.error("Address failed verification. " + dir.getName() + "/" + file.getName(), e);
                    }
                }
            }

        }


    }


    @Test
    public void testUKReviewTotalCost() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.UK, buyer, 1);
        browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "UKCheckoutReview.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        orderReviewOnePage = new OrderReviewOnePage(buyerPanel);
        Money grandTotal = orderReviewOnePage.parseTotal();
        assertEquals(grandTotal.toString(), "$13.03");
    }

    @Test
    public void testUKReviewTotalCostFail() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.UK, buyer, 1);
        browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "Amazon.co.ukCheckout.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        orderReviewOnePage = new OrderReviewOnePage(buyerPanel);
        Money grandTotal = orderReviewOnePage.parseTotal();
        assertEquals(grandTotal.toString(), "$13.03");
    }

    @Test
    public void testCAReviewTotalCost() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.CA, buyer, 1);
        browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "CACheckoutReview.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        orderReviewOnePage = new OrderReviewOnePage(buyerPanel);
        Money grandTotal = orderReviewOnePage.parseTotal();
        assertEquals(grandTotal.toString(), "$12.73");
    }

    @Test
    public void testDEReviewTotalCost() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.DE, buyer, 1);
        browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "DECheckoutReview.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        orderReviewOnePage = new OrderReviewOnePage(buyerPanel);
        Money grandTotal = orderReviewOnePage.parseTotal();
        assertEquals(grandTotal.toString(), "$12.24");
    }

    @Test
    public void testReviewTotalCost() {
        prepareData();


        for (File dir : directories) {
            File[] files = dir.listFiles(file -> file.isFile() && StringUtils.contains(file.getName(), "OrderReviewPage_execute.html"));
            assert files != null;
            for (File file : files) {
                String orderId = RegexUtils.getMatched(file.getName(), RegexUtils.Regex.AMAZON_ORDER_NUMBER);
                if (orderMap.containsKey(orderId)) {
                    Order order = orderMap.get(orderId).get(0);
                    browser.loadHTML(Tools.readFileToString(file));
                    WaitTime.Short.execute();
                    Money grandTotal = orderReviewOnePage.parseTotal();
                    if (!order.cost.equalsIgnoreCase(grandTotal.toUSDAmount().toPlainString())) {
                        System.out.println(String.format("File %s, Actual %s, Parsed %s", dir.getName() + "/" + file.getName(), order.cost, grandTotal.toUSDAmount().toPlainString()));
                    }

                }
            }

        }


    }

    @Test
    public void testPlaceOrder() throws Exception {
    }

}