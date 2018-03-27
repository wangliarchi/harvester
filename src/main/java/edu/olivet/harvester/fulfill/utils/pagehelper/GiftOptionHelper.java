package edu.olivet.harvester.fulfill.utils.pagehelper;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMInputElement;
import com.teamdev.jxbrowser.chromium.dom.DOMTextAreaElement;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.utils.JXBrowserHelper;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/2/2017 1:50 PM
 */
@SuppressWarnings("CheckStyle")
public class GiftOptionHelper {
    public static final String continueBtnSelector = ".save-gift-button-box .a-button-primary .a-button-text," +
            ".save-gift-button-box .a-button-primary .a-button-input," +
            ".popover-gift-bottom .a-button-primary .a-button-text," +
            ".popover-gift-bottom .a-button-primary .a-button-input";
    private static final String CHECKBOX_SELECTOR =
            "#giftForm .includeReceiptCheckbox input,.include-gift-receipt-checkbox input,.include-gift-receipt input,input.hide-prices-checkbox";

    public static void giftOption(Browser browser, Order order) {
        //US FWD or UK FWD order does not need gift options.
        if (order.isUKForward() || order.purchaseBack()) {
            return;
        }
        //a-button a-button-small gift-popover-link
        DOMElement giftOptionBtn = JXBrowserHelper.selectElementByCssSelector(browser, ".shipping-group .gift-popover-link");

        if (giftOptionBtn != null) {
            giftOptionBtn.click();
            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilVisible(browser, continueBtnSelector);


            if (JXBrowserHelper.selectElementByCssSelector(browser, "#giftForm") != null) {
                List<DOMElement> checkboxes = JXBrowserHelper.selectElementsByCssSelectors(browser, CHECKBOX_SELECTOR);
                for (DOMElement checkbox : checkboxes) {
                    ((DOMInputElement) checkbox).setChecked(true);
                }

                List<DOMElement> textareas = JXBrowserHelper.selectElementsByCssSelectors(browser, "#giftForm .item-gift-message-span textarea");
                for (DOMElement textarea : textareas) {
                    ((DOMTextAreaElement) textarea).setValue("");
                }

            } else {
                DOMInputElement giftReceipt = (DOMInputElement) JXBrowserHelper.selectElementByCssSelector(browser, CHECKBOX_SELECTOR);
                if (giftReceipt != null) {
                    giftReceipt.setChecked(true);
                }

                DOMTextAreaElement giftMessageTextArea = (DOMTextAreaElement) JXBrowserHelper.selectElementByName(browser, "gift-message-text");
                assert giftMessageTextArea != null;
                giftMessageTextArea.setValue("");
                WaitTime.Shortest.execute();
            }

            try {
                DOMElement continueBtn = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, continueBtnSelector);
                if (continueBtn != null) {
                    continueBtn.click();
                    WaitTime.Shortest.execute();
                    JXBrowserHelper.waitUntilNotFound(continueBtn);
                }
            } catch (Exception e) {
                //
            }
        }


        DOMElement giftForm = JXBrowserHelper.selectVisibleElement(browser, "#giftForm");
        if (giftForm != null) {
            DOMElement continueBtn = JXBrowserHelper.selectVisibleElement(browser, continueBtnSelector);
            assert continueBtn != null;
            continueBtn.click();
            WaitTime.Shortest.execute();
            JXBrowserHelper.waitUntilNotFound(continueBtn);
        }
    }

}
