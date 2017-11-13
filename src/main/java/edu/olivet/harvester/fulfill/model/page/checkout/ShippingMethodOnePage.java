package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:44 PM
 */
public class ShippingMethodOnePage extends ShippingAddressAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingMethodOnePage.class);
    private static final String CONTINUE_BTN_SELECTOR = "#shippingOptionFormId input.a-button-text";

    public ShippingMethodOnePage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void execute(Order order) {
        //get all available options
        List<DOMElement> options = JXBrowserHelper.selectElementsByCssSelector(browser, ".shipping-speed.ship-option");
        List<ShippingOption> shippingOptions = new ArrayList<>();
        for (DOMElement option : options) {
            String eddText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-success.a-text-bold").getInnerText();
            String priceText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-secondary").getInnerText();
            ShippingOption shippingOption = new ShippingOption(eddText, priceText, buyerPanel.getCountry());
            shippingOptions.add(shippingOption);
        }
        //remove long edd?
    }

    public void updateQty(Order order) {
        if (order.quantity_purchased.equals(JXBrowserHelper.text(browser, ".quantity-dropdown .a-dropdown-prompt"))) {
            return;
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

        browser.executeJavaScript(String.format("$('select.a-native-dropdown.quantity-dropdown-select').val(%s).change()", order.quantity_purchased));
        //JXBrowserHelper.setValueForFormSelect(browser, "select.a-native-dropdown.quantity-dropdown-select", order.quantity_purchased);

        DOMElement select = JXBrowserHelper.selectElementByCssSelector(browser, "select.a-native-dropdown.quantity-dropdown-select.js-select");
        WaitTime.Shortest.execute();
        LOGGER.info(select.getInnerHTML());

        //check errors
        DOMElement errorContainer = JXBrowserHelper.selectElementByCssSelector(browser, ".a-row.update-quantity-error");
        if (StringUtils.containsIgnoreCase(errorContainer.getAttribute("style"), "block")) {
            List<DOMElement> errors = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-row.update-quantity-error .error-message");
            errors.removeIf(it -> StringUtils.containsIgnoreCase(it.getAttribute("style"), "none"));
            if (CollectionUtils.isNotEmpty(errors)) {
                LOGGER.error("Error updating qty - {}", errors.stream().map(it -> it.getInnerText()).collect(Collectors.toSet()));
            }
        }


        //get the qty now
        String qty = JXBrowserHelper.text(browser, ".quantity-dropdown .a-dropdown-prompt");
        if (!order.quantity_purchased.equals(qty)) {
            OrderHelper.addQuantChangeRemark(order, qty);
        }
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

    }



}