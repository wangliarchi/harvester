package edu.olivet.harvester.fulfill.model.page;

import com.google.common.collect.Lists;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/16/17 2:57 PM
 */
public class AmazonPrimeAdPage extends FulfillmentPage implements PageObject {

    public AmazonPrimeAdPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    private final List<String> linkSelectors = Lists.newArrayList(".prime-nothanks-button", "#mom-no-thanks",
            ".prime-popover-actions button.primary","#prime-no-thanks");

    @Override
    public void execute(Order order) {

        for (String selector : linkSelectors) {
            DOMElement noLink = JXBrowserHelper.selectElementByCssSelector(browser, selector);
            if (noLink != null && JXBrowserHelper.isVisible(noLink)) {
                JXBrowserHelper.click(noLink);
                WaitTime.Shortest.execute();
                return;
            }
        }


    }
}
