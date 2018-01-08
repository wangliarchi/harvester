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
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.fulfill.service.shipping.ShipOptionUtils;
import edu.olivet.harvester.fulfill.service.shipping.ShippingHandlerFactory;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
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

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/21/17 11:30 AM
 */
public class ShippingMethodOnePageTest extends BaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingMethodOnePageTest.class);
    Browser browser;

    @Inject
    SheetAPI sheetAPI;
    @Inject
    OrderService orderService;
    List<Order> orders;
    Map<String, List<Order>> orderMap;
    BuyerPanel buyerPanel;
    File[] directories;

    public void prepareData() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        browser = buyerPanel.getBrowserView().getBrowser();
        final String SPREADSHEET_ID = "1U_qeXOm5qvCLzrfhX96sJSTPKUXzP57_owTL12XmE9U";

        Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(SPREADSHEET_ID);
        orders = orderService.fetchOrders(spreadsheet, Range.between(Dates.parseDate("11/05/2017"), Dates.parseDate("11/17/2017")));
        orderMap = orders.stream().collect(Collectors.groupingBy(Order::getOrder_id));

        directories = new File(TEST_DATA_ROOT + File.separator + "pages").listFiles(file -> file.isDirectory() && StringUtils.contains(file.getName(), "720US"));

    }

    @Test
    public void testExecute() throws Exception {
        prepareData();

        for (File dir : directories) {
            File[] files = dir.listFiles(file -> file.isFile() && StringUtils.contains(file.getName(), "OrderReviewPage_execute.html"));
            for (File file : files != null ? files : new File[0]) {

                String orderId = RegexUtils.getMatched(file.getName(), RegexUtils.Regex.AMAZON_ORDER_NUMBER);
                if (orderMap.containsKey(orderId)) {
                    try {
                        Order order = orderMap.get(orderId).get(0);
                        buyerPanel.setOrder(order);
                        browser.loadHTML(Tools.readFileToString(file));
                        WaitTime.Short.execute();

                        List<ShippingOption> options = ShipOptionUtils.listAllOptions(browser, Country.US);
                        List<ShippingOption> validOptions = ShippingHandlerFactory.getHandler(order).getValidateOptions(order, options);

                        System.out.println(validOptions);
                    } catch (Exception e) {
                        LOGGER.error("{} ", dir.getName() + "/" + file.getName(), e);
                        WaitTime.Shortest.execute();
                    }
                }
            }
        }

    }

    @Test
    public void testDEShippingOptions() {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        buyerPanel = new BuyerPanel(0, Country.CA, buyer, 1);
        browser = buyerPanel.getBrowserView().getBrowser();
        File file = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "DECheckoutReview.html");
        browser.loadHTML(Tools.readFileToString(file));
        WaitTime.Shortest.execute();

        order = prepareOrder();
        order.estimated_delivery_date="2017-12-19 2018-01-05";
        List<ShippingOption> options = ShipOptionUtils.listAllOptions(browser, Country.DE);

        List<ShippingOption> validOptions = ShippingHandlerFactory.getHandler(order).getValidateOptions(order, options);

        System.out.println(validOptions);
    }

}