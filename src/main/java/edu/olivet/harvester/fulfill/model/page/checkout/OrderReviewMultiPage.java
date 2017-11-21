package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.page.AmazonPage;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.ShipOptionUtils;
import edu.olivet.harvester.model.CreditCard;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 3:54 PM
 */
public class OrderReviewMultiPage extends OrderReviewAbstractPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReviewMultiPage.class);

    public OrderReviewMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        //placeholder
    }

    public void changeShippingAddress() {
        String url = country.baseUrl() + AmazonPage.EnterShippingAddress.urlMark();
        JXBrowserHelper.loadPage(browser, url);
        JXBrowserHelper.wait(browser, By.cssSelector("#enterAddressFullName"));
    }

    public void changePaymentMethod() {
        String url = country.baseUrl() + AmazonPage.SelectPayment.urlMark();
        JXBrowserHelper.loadPage(browser, url);
        JXBrowserHelper.wait(browser, By.cssSelector("#new-payment-methods"));
    }

    public void changeShippingMethod() {
        //get all available options
        JXBrowserHelper.saveOrderScreenshot(buyerPanel.getOrder(), buyerPanel, "1");

        ShipOptionUtils.selectShipOption(buyerPanel);

        JXBrowserHelper.saveOrderScreenshot(buyerPanel.getOrder(), buyerPanel, "2");

    }

    public void updateQty(Order order) {

        if (order.quantity_purchased.equals(JXBrowserHelper.text(browser, ".quantity-display"))) {
            return;
        }

        DOMElement changeLink = JXBrowserHelper.selectElementByCssSelector(browser, ".a-declarative.change-quantity-button");

        if (changeLink != null && JXBrowserHelper.isVisible(browser, ".a-declarative.change-quantity-button")) {
            changeLink.click();

            JXBrowserHelper.waitUntilVisible(browser, ".quantity-input");

            JXBrowserHelper.fillValueForFormField(browser, ".quantity-input", order.quantity_purchased);

            WaitTime.Shortest.execute();

            browser.executeJavaScript("document.querySelector('.a-row.quantity-block .update-quantity-button').click()");
            //updateLink.click();


            JXBrowserHelper.waitUntilVisible(browser, ".a-declarative.change-quantity-button");

            //check errors
            List<DOMElement> errors = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-row.update-quantity-error .error-message");
            errors.removeIf(it -> JXBrowserHelper.isHidden(it));
            if (CollectionUtils.isNotEmpty(errors)) {
                LOGGER.error("Error updating qty - {}", errors.stream().map(DOMElement::getInnerText).collect(Collectors.toSet()));
            }

            //get the qty now
            String qty = JXBrowserHelper.text(browser, ".quantity-display");
            order.quantity_fulfilled = qty;
            if (!order.quantity_purchased.equals(qty)) {
                OrderHelper.addQuantChangeRemark(order, qty);
            }


        }

    }


    public boolean reviewPaymentMethod() {
        String lastDigits = JXBrowserHelper.text(browser, "#payment-information");
        lastDigits = lastDigits.replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
        CreditCard creditCard = OrderBuyerUtils.getCreditCard(buyerPanel.getOrder());
        return creditCard.getCardNo().endsWith(lastDigits);
    }


}
