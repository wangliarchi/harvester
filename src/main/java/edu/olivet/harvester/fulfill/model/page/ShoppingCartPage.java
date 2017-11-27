package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.order.PageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 9:35 AM
 */
public class ShoppingCartPage extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingCartPage.class);


    public ShoppingCartPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public String cartUrl() {
        return country.baseUrl() + "/" + AmazonPage.ShoppingCart.urlMark();
    }

    public void enter() {
        LOGGER.info("Loading shopping cart page {}", cartUrl());
        JXBrowserHelper.loadPage(browser, cartUrl());

    }


    public void clearShoppingCart() {
        enter();
        clear();

    }

    public void processToCheckout() {

        DOMElement checkoutLink = JXBrowserHelper.selectElementByCssSelector(browser, "#hlb-ptc-btn-native");
        if (checkoutLink != null) {
            String url = checkoutLink.getAttribute(PageUtils.HREF);
            if (StringUtils.containsNone(url, "//")) {
                url = country.baseUrl() + url;
            }
            JXBrowserHelper.loadPage(browser, url);
            return;
        }

        //redirect to shopping cart page
        if (!StringUtils.containsIgnoreCase(browser.getURL(), AmazonPage.ShoppingCart.urlMark())) {
            enter();
        }
        LOGGER.info("Current at {} - {}", browser.getTitle(), browser.getURL());
        DOMElement checkoutBtn = JXBrowserHelper.selectElementByName(browser, "proceedToCheckout");
        JXBrowserHelper.insertChecker(browser);
        checkoutBtn.click();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);
        LOGGER.info("Current at {} - {}", browser.getTitle(), browser.getURL());
    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void clear() {
        long start = System.currentTimeMillis();

        try {
            DOMElement cartForm = JXBrowserHelper.selectElementByCssSelector(browser, "#activeCartViewForm");
            if (StringUtils.contains(cartForm.getInnerHTML(), "sc-empty-cart")) {
                LOGGER.info("Shopping cart is empty.");
                return;
            }
        } catch (Exception e) {
            // -> Ignore
        }
        String selector = "div#sc-active-cart span.a-size-small.sc-action-delete input[type=submit]";

        List<DOMElement> deletes = JXBrowserHelper.selectElementsByCssSelector(browser, selector);
        if (CollectionUtils.isEmpty(deletes)) {
            LOGGER.info("Shopping cart is empty.");
            return;
        }

        LOGGER.info("{} items found in shopping cart.", deletes.size());

        int index = 1;
        while (true) {

            DOMElement deleteBtn = JXBrowserHelper.selectElementByCssSelector(browser, selector);
            if (deleteBtn == null) {
                LOGGER.info("All item removed from shopping cart.");
                break;
            }
            LOGGER.info("Removing item #{} from cart.", index);

            deleteBtn.click();

            WaitTime.Short.execute();
            index++;
        }


        LOGGER.debug("Cleared shopping cart finished in {}", Strings.formatElapsedTime(start));
    }


    public void execute(Order order) {

    }
}
