package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class ShippingMethodMultiPage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingMethodMultiPage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#shippingOptionFormId input.a-button-text";

    public ShippingMethodMultiPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        //handled on order review page

        DOMElement continueBtn = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, CONTINUE_BTN_SELECTOR);

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

        if (continueBtn == null) {
            JXBrowserHelper.saveOrderScreenshot(order,buyerPanel,"1");
            throw new BusinessException(String.format("Continue button on select shipping option page not found. Current at %s- %s", browser.getTitle(), browser.getURL()));
        }

        JXBrowserHelper.insertChecker(browser);
        continueBtn.click();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);
    }
}