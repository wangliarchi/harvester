package edu.olivet.harvester.fulfill.utils.pagehelper;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/24/2017 11:40 AM
 */
public class QtyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(QtyUtils.class);

    @Repeat(expectedExceptions = BusinessException.class)
    public static void updateQty(BuyerPanel buyerPanel, Order order) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        if (StringUtils.isNotBlank(JXBrowserHelper.text(browser, ".quantity-display"))) {
            try {
                _updateQtyTextField(buyerPanel, order);
            } catch (Exception e) {
                LOGGER.error("Error update qty");
                throw new BusinessException(e);
            }
        } else {
            try {
                _updateQtySelect(buyerPanel, order);
            } catch (Exception e) {
                LOGGER.error("Error update qty");
                throw new BusinessException(e);
            }

        }

    }


    private static void _updateQtyTextField(BuyerPanel buyerPanel, Order order) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            if (order.quantity_purchased.equals(JXBrowserHelper.text(browser, ".quantity-display"))) {
                return;
            }

            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

            DOMElement changeLink = JXBrowserHelper.selectElementByCssSelector(browser, ".a-declarative.change-quantity-button");

            if (changeLink != null && JXBrowserHelper.isVisible(changeLink)) {
                changeLink.click();

                JXBrowserHelper.waitUntilVisible(browser, ".quantity-input");

                JXBrowserHelper.fillValueForFormField(browser, ".quantity-input", order.quantity_purchased);

                WaitTime.Normal.execute();

                browser.executeJavaScript("document.querySelector('.a-row.quantity-block .update-quantity-button').click()");
                //updateLink.click();


                JXBrowserHelper.waitUntilVisible(browser, ".a-declarative.change-quantity-button");

                //check errors
                List<DOMElement> errors = JXBrowserHelper.selectElementsByCssSelector(browser,
                        ".a-row.update-quantity-error .error-message");
                errors.removeIf(JXBrowserHelper::isHidden);
                if (CollectionUtils.isNotEmpty(errors)) {
                    LOGGER.error("Error updating qty - {}", errors.stream().map(DOMElement::getInnerText).collect(Collectors.toSet()));
                }

                //get the qty now
                order.quantity_fulfilled = JXBrowserHelper.text(browser, ".quantity-display");

                JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            }
        }
    }

    private static void _updateQtySelect(BuyerPanel buyerPanel, Order order) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            String currentQty = JXBrowserHelper.text(browser, ".quantity-dropdown .a-dropdown-prompt");
            if (order.quantity_purchased.equals(currentQty)) {
                return;
            }

            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

            DOMElement dropdownBtn = JXBrowserHelper.selectElementByCssSelector(browser, ".quantity-dropdown .a-button-text");
            if (dropdownBtn == null) {
                continue;
            }

            dropdownBtn.click();
            JXBrowserHelper.waitUntilVisible(browser, "div.a-popover-wrapper ul.a-list-link");

            List<DOMElement> anchors = JXBrowserHelper.selectElementsByCssSelector(browser,
                    "div.a-popover-wrapper ul.a-list-link > li > a");
            for (DOMElement we : anchors) {
                if (order.quantity_purchased.equals(we.getInnerText().trim())) {
                    we.click();
                    JXBrowserHelper.waitUntilNotFound(browser, "div.a-popover");
                    break;
                }
            }

            //get the qty now
            order.quantity_fulfilled = JXBrowserHelper.text(browser, ".quantity-dropdown .a-dropdown-prompt");
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

            //check errors
            DOMElement errorContainer = JXBrowserHelper.selectElementByCssSelector(browser, ".a-row.update-quantity-error");

            if (JXBrowserHelper.isVisible(errorContainer)) {
                List<DOMElement> errors = JXBrowserHelper.selectElementsByCssSelector(browser,
                        ".a-row.update-quantity-error .error-message");
                errors.removeIf(JXBrowserHelper::isHidden);
                if (CollectionUtils.isNotEmpty(errors)) {
                    LOGGER.error("Error updating qty - {}", errors.stream().map(DOMElement::getInnerText).collect(Collectors.toSet()));
                    return;
                }
            }
        }

    }
}

