package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.model.page.AmazonPage;
import edu.olivet.harvester.fulfill.service.shipping.ShipOptionUtils;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.pagehelper.GiftOptionHelper;
import edu.olivet.harvester.fulfill.utils.pagehelper.QtyUtils;
import edu.olivet.harvester.model.CreditCard;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        QtyUtils.updateQty(buyerPanel, order);

        GiftOptionHelper.giftOption(buyerPanel.getBrowserView().getBrowser(), order);
    }


    public boolean reviewPaymentMethod() {
        String lastDigits = JXBrowserHelper.text(browser, "#payment-information");
        lastDigits = lastDigits.replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
        CreditCard creditCard = OrderBuyerUtils.getCreditCard(buyerPanel.getOrder());
        return creditCard.getCardNo().endsWith(lastDigits);
    }


}
