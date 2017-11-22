package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.addressvalidator.AddressValidator;
import edu.olivet.harvester.fulfill.utils.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.utils.OrderAddressUtils;
import edu.olivet.harvester.fulfill.utils.ProfitLostControl;
import edu.olivet.harvester.model.Money;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 1:53 PM
 */
public abstract class OrderReviewAbstractPage extends FulfillmentPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReviewAbstractPage.class);

    OrderReviewAbstractPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }


    public void checkTotalCost(Order order) {

        Money grandTotal = parseTotal();

        if (!ProfitLostControl.canPlaceOrder(order, grandTotal.toUSDAmount().floatValue())) {
            throw new BusinessException("Order cost exceed maximum limit.");
        }

        RuntimeSettings settings = RuntimeSettings.load();
        float remainingBudget = ApplicationContext.getBean(DailyBudgetHelper.class).getRemainingBudget(settings.getSpreadsheetId(), new Date());
        if (remainingBudget < grandTotal.toUSDAmount().floatValue()) {
            throw new BusinessException("You don't have enough fund to process this order. Need $" + grandTotal.toUSDAmount() + ", only have $" + String.format("%.2f", remainingBudget));
        }


    }


    public Money parseTotal() {
        String grandTotalText = JXBrowserHelper.text(browser, "#subtotals-marketplace-table .grand-total-price");
        Money grandTotal;
        try {
            grandTotal = Money.fromText(grandTotalText, buyerPanel.getCountry());
        } catch (Exception e) {
            LOGGER.error("Error reading grand total. ", e);
            throw new BusinessException("Can read grand total - " + e.getMessage());
        }

        return grandTotal;
    }

    public Address parseEnteredAddress() {
        try {
            String name = JXBrowserHelper.text(browser, ".displayAddressUL .displayAddressFullName");
            String addressLine1 = JXBrowserHelper.text(browser, ".displayAddressUL .displayAddressAddressLine1");
            String addressLine2 = JXBrowserHelper.text(browser, ".displayAddressUL .displayAddressAddressLine2");
            String cityStateZip = JXBrowserHelper.text(browser, ".displayAddressUL .displayAddressCityStateOrRegionPostalCode");
            String[] parts = StringUtils.split(cityStateZip, ",");
            String city = parts[0].trim();
            String[] regionZip = StringUtils.split(parts[1].trim(), " ");
            String zip = regionZip[regionZip.length - 1];
            String state = StringUtils.join(Arrays.copyOf(regionZip, regionZip.length - 1), " ");

            Address enteredAddress = new Address();
            enteredAddress.setName(name.replace(RuntimeSettings.load().getNoInvoiceText(), ""));
            enteredAddress.setAddress1(addressLine1);
            enteredAddress.setAddress2(addressLine2);
            enteredAddress.setCity(city);
            enteredAddress.setState(state);
            enteredAddress.setZip(zip);

            return enteredAddress;

        } catch (Exception e) {

            throw new BusinessException("Error parse shipping address for " + buyerPanel.getOrder().order_id);
        }


    }

    public void reviewShippingAddress(AddressValidator addressValidator) {
        Address enteredAddress = parseEnteredAddress();

        if (!addressValidator.verify(OrderAddressUtils.orderShippingAddress(buyerPanel.getOrder()), enteredAddress)) {
            throw new BusinessException(String.format("Address failed review. Entered %s, origin %s", enteredAddress, Address.loadFromOrder(buyerPanel.getOrder())));
        }

    }


    public void placeOrder(Order order) {
        //checkTotalCost(order);
        if (PSEventListener.stopped()) {
            throw new BusinessException("Process stoped as requested.");
        }

        DOMElement placeOrderBtn = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, "#submitOrderButtonId .a-button-input, .place-your-order-button");
        placeOrderBtn.click();

        JXBrowserHelper.waitUntilNewPageLoaded(browser);

        DOMElement forceDuplicate = JXBrowserHelper.selectElementByName(browser, "forcePlaceOrder");
        if (forceDuplicate != null) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            forceDuplicate.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

    }

}
