package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.model.Money;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:38 PM
 */
public class PlacedOrderDetailPage extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlacedOrderDetailPage.class);

    public PlacedOrderDetailPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    @Repeat
    public void execute(Order order) {
        try {
            JXBrowserHelper.wait(browser, By.cssSelector("#orderDetails"));
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            order.order_number = parseOrderId();
            order.cost = parseTotalCost();
            order.last_code = parseLastCode();
            order.account = buyer.getEmail();
        } catch (Exception e) {
            LOGGER.error("Error parse data on order detail page", e);
            //reload page
            JXBrowserHelper.loadPage(browser, String.format("%s/gp/css/summary/edit.html/ref=typ_rev_edit?ie=UTF8&orderID=%s", buyerPanel.getCountry().baseUrl(), order.order_number));
            throw new BusinessException(e);
        }
    }

    public String parseOrderId() {
        String text = JXBrowserHelper.text(browser, "#orderDetails");
        return RegexUtils.getMatched(text, RegexUtils.Regex.AMAZON_ORDER_NUMBER);
    }

    public String parseTotalCost() {
        String total = JXBrowserHelper.text(browser, "#od-subtotals .a-text-right.a-span-last .a-color-base.a-text-bold");
        try {
            Money money = Money.fromText(total, country);
            return money.toUSDAmount().toPlainString();
        } catch (Exception e) {
            //ignore
        }
        return "";
    }

    public String parseLastCode() {
        String text = JXBrowserHelper.text(browser, "#orderDetails .a-box.a-first");
        return RegexUtils.getMatched(text, "\\*\\*\\*\\* [0-9]{4}").replaceAll("\\*\\*\\*\\* ", "");
    }


}
