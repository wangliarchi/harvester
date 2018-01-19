package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:35 PM
 */
public class OrderPlacedSuccessPage extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPlacedSuccessPage.class);

    public OrderPlacedSuccessPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    public void execute(Order order) {

        JXBrowserHelper.waitUntilVisible(browser, ".a-alert-content a.a-link-emphasis");
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
        DOMElement viewLink = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, ".a-alert-content a.a-link-emphasis");
        //parse orderId from url
        String orderId = RegexUtils.getMatched(browser.getURL(), RegexUtils.Regex.AMAZON_ORDER_NUMBER);
        if (StringUtils.isBlank(orderId)) {
            orderId = RegexUtils.getMatched(viewLink.getAttribute("href"), RegexUtils.Regex.AMAZON_ORDER_NUMBER);
        }


        if (StringUtils.isNotBlank(orderId)) {
            order.order_number = orderId;
            order.account = buyer.getEmail();

            String url = String.format("%s/gp/css/summary/edit.html/ref=typ_rev_edit?ie=UTF8&orderID=%s",
                    buyerPanel.getCountry().baseUrl(), orderId);
            LOGGER.debug("Order placed successfully, with order id {}. Now heading to {}", orderId, url);
            JXBrowserHelper.loadPage(browser, url);
        } else {
            JXBrowserHelper.insertChecker(browser);
            viewLink.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }


    }

    public static void main(String[] args) {
        String url = "https://www.amazon.com/gp/buy/thankyou/handlers/display.html?ie=UTF8&asins=B005LTNLDS&isRefresh=1&orderId=113-5188884-1320230&purchaseId=106-9921921-2849009&viewId=ThankYouCart";
        String orderId = RegexUtils.getMatched(url, RegexUtils.Regex.AMAZON_ORDER_NUMBER);
        String vUrl = String.format("%s/gp/css/summary/edit.html/ref=typ_rev_edit?ie=UTF8&orderID=%s", Country.US.baseUrl(), orderId);
        System.out.println(vUrl);
    }
}
