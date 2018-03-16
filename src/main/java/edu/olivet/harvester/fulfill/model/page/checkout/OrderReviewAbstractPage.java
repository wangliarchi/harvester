package edu.olivet.harvester.fulfill.model.page.checkout;

import com.google.common.collect.Lists;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.model.CreditCard;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.exception.Exceptions.OutOfBudgetException;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.fulfill.service.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProfitLostControl;
import edu.olivet.harvester.fulfill.service.addressvalidator.AddressValidator;
import edu.olivet.harvester.fulfill.service.shipping.FeeLimitChecker;
import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.fulfill.utils.OrderAddressUtils;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 1:53 PM
 */
public abstract class OrderReviewAbstractPage extends FulfillmentPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderReviewAbstractPage.class);
    private static final List<String> SHIPPING_KEYWORDS =
            Lists.newArrayList("Shipping", "packing", "Verpackung", "Livraison", "Envío", "spedizione");
    private static final List<String> GIFTCARD_KEYWORDS =
            Lists.newArrayList("Gift Card", "Tarjeta de regalo", "Carta regalo", "Carte cadeau", "Geschenkkarte");

    OrderReviewAbstractPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    public void checkTotalCost(Order order) {
        Money grandTotal = parseTotal();
        Money giftCardCost = parseGiftCardCost();
        float totalUSD = grandTotal.toUSDAmount().floatValue() + giftCardCost.toUSDAmount().floatValue();
        if (order.selfOrder) {
            if (totalUSD > SystemSettings.load().getSelfOrderCostLimit()) {
                throw new OrderSubmissionException("Order cost " + grandTotal.usdText() + " exceed maximum limit");
            }
        } else {

            if (!ProfitLostControl.canPlaceOrder(order, totalUSD)) {
                throw new OrderSubmissionException("Order cost " + grandTotal.usdText() + " exceed maximum limit");
            }

            float remainingBudget = ApplicationContext.getBean(DailyBudgetHelper.class)
                    .getRemainingBudget(order.getSpreadsheetId(), new Date(), grandTotal.toUSDAmount().floatValue());

            if (remainingBudget < grandTotal.toUSDAmount().floatValue()) {
                throw new OutOfBudgetException("You don't have enough fund to process this order. Need $" +
                        grandTotal.toUSDAmount() + ", only have $" + String.format("%.2f", remainingBudget));
            }
        }

        order.orderTotalCost = new Money(grandTotal.toUSDAmount().floatValue() + giftCardCost.toUSDAmount().floatValue(), Country.US);
        order.cost = order.orderTotalCost.toUSDAmount().toPlainString();
    }


    public void checkShippingCost(Order order) {
        Money shippingCost = parseShippingFee();

        if (!FeeLimitChecker.getInstance().notExceed(order, shippingCost.getAmount().floatValue())) {
            throw new OrderSubmissionException("Order shipping cost " + shippingCost.usdText() + " exceed maximum limit. ");
        }
        order.shippingCost = shippingCost;
    }


    public Money parseShippingFee() {
        List<DOMElement> trs = JXBrowserHelper.selectElementsByCssSelector(browser, "#subtotals-marketplace-table tr");
        Money shippingCost = null;

        for (DOMElement tr : trs) {
            if (Strings.containsAnyIgnoreCase(JXBrowserHelper.textFromElement(tr), SHIPPING_KEYWORDS.toArray(new String[SHIPPING_KEYWORDS.size()]))) {
                try {
                    String shippingCostString = JXBrowserHelper.text(tr, ".a-text-right");
                    if (StringUtils.isNotBlank(shippingCostString)) {
                        shippingCost = Money.fromText(shippingCostString, buyerPanel.getCountry());
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error reading shipping cost. ", e);
                    throw new BusinessException("Cant read shipping cost - " + e.getMessage());
                }
            }
        }

        if (shippingCost == null) {
            throw new BusinessException("Cant read shipping cost.");
        }
        return shippingCost;
    }

    public Money parseGiftCardCost() {
        List<DOMElement> trs = JXBrowserHelper.selectElementsByCssSelector(browser, "#subtotals-marketplace-table tr");
        Money giftCardCost = new Money(0, buyerPanel.getCountry());

        for (DOMElement tr : trs) {
            if (Strings.containsAnyIgnoreCase(JXBrowserHelper.textFromElement(tr), GIFTCARD_KEYWORDS.toArray(new String[GIFTCARD_KEYWORDS.size()]))) {
                try {
                    String shippingCostString = JXBrowserHelper.text(tr, ".a-text-right");
                    if (StringUtils.isNotBlank(shippingCostString)) {
                        giftCardCost = Money.fromText(shippingCostString, buyerPanel.getCountry());
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error reading gift card cost. ", e);
                    //throw new BusinessException("Cant read shipping cost - " + e.getMessage());
                }
            }
        }

        return giftCardCost;
    }

    public boolean reviewPaymentMethod() {
        if (StringUtils.isNotBlank(buyerPanel.getOrder().promotionCode)) {
            Money total = parseTotal();
            LOGGER.info("{}", total);
            if (total.getAmount().floatValue() == 0) {
                return true;
            }
            if (total.getAmount().floatValue() > 1) {
                return false;
            }
        }
        JXBrowserHelper.waitUntilVisible(browser, "#payment-information");
        String lastDigits = JXBrowserHelper.textFromElement(browser, "#payment-information");
        lastDigits = lastDigits.replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
        CreditCard creditCard = CreditCardUtils.getCreditCard(buyerPanel.getBuyer());

        LOGGER.info("Last digits {} - {}", lastDigits, creditCard.lastDigits());
        return StringUtils.containsIgnoreCase(lastDigits, creditCard.lastDigits());
    }

    public Money parseTotal() {
        DOMElement transactionalTablePriceElement = JXBrowserHelper.selectElementByCssSelector(browser,
                "#subtotals-transactional-table .order-summary-tfx-grand-total-stressed .a-color-price.a-text-right," +
                        "#subtotals-transactional-table .grand-total-price");

        if (transactionalTablePriceElement != null) {
            String grandTotalText = transactionalTablePriceElement.getInnerText().trim();
            if (StringUtils.isNotBlank(grandTotalText)) {
                try {
                    float amount = Money.getAmountFromText(grandTotalText, Country.US);
                    if (amount > 0) {
                        return new Money(amount, Country.US);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error reading grand total. ", e);
                }
            }
        }

        String grandTotalText = JXBrowserHelper.text(browser, "#subtotals-marketplace-table .grand-total-price");

        try {
            return Money.fromText(grandTotalText, buyerPanel.getCountry());
        } catch (Exception e) {
            LOGGER.error("Error reading grand total. ", e);
            throw new BusinessException("Can not read grand total - " + e.getMessage());
        }

    }

    public Address parseEnteredAddress() {
        try {
            //
            DOMElement shippingAddressDiv = JXBrowserHelper.selectVisibleElement(browser, "#desktop-shipping-address-div,#shipaddress");
            String name = JXBrowserHelper.text(shippingAddressDiv, ".displayAddressUL .displayAddressFullName");
            String addressLine1 = JXBrowserHelper.text(shippingAddressDiv, ".displayAddressUL .displayAddressAddressLine1");
            String addressLine2 = JXBrowserHelper.text(shippingAddressDiv, ".displayAddressUL .displayAddressAddressLine2");
            String cityStateZip = JXBrowserHelper.text(shippingAddressDiv, ".displayAddressUL .displayAddressCityStateOrRegionPostalCode");
            String country = JXBrowserHelper.text(shippingAddressDiv, ".displayAddressUL .displayAddressCountryName");
            String[] parts = StringUtils.split(cityStateZip, ",");
            String city = parts[0].trim();
            String[] regionZip = StringUtils.split(parts[1].trim(), " ");
            String zip = regionZip[regionZip.length - 1];
            String state = StringUtils.join(Arrays.copyOf(regionZip, regionZip.length - 1), " ");

            Address enteredAddress = new Address();
            enteredAddress.setName(name.replace(buyerPanel.getOrder().getRuntimeSettings().getNoInvoiceText(), ""));
            enteredAddress.setAddress1(addressLine1);
            enteredAddress.setAddress2(addressLine2);
            enteredAddress.setCity(city);
            enteredAddress.setState(state);
            enteredAddress.setZip(zip);
            enteredAddress.setNoInvoiceText(buyerPanel.getOrder().getRuntimeSettings().getNoInvoiceText());

            if (StringUtils.isNotBlank(country)) {
                enteredAddress.setCountry(country);
            }


            return enteredAddress;

        } catch (Exception e) {

            throw new BusinessException("Error parse shipping address for " + buyerPanel.getOrder().order_id);
        }
    }

    public void reviewShippingAddress(AddressValidator addressValidator) {
        Address enteredAddress = parseEnteredAddress();

        if (!addressValidator.verify(OrderAddressUtils.orderShippingAddress(buyerPanel.getOrder()), enteredAddress)) {
            throw new OrderSubmissionException(String.format(
                    "Address failed review. Entered %s, origin %s",
                    enteredAddress, OrderAddressUtils.orderShippingAddress(buyerPanel.getOrder())));
        }
    }


    public void placeOrder(Order order) {
        //checkTotalCost(order);
        if (PSEventListener.stopped()) {
            throw new OrderSubmissionException("Process stopped as requested.");
        }

        DOMElement placeOrderBtn = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser,
                "#submitOrderButtonId .a-button-input, .place-your-order-button");

        JXBrowserHelper.insertChecker(browser);
        placeOrderBtn.click();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);
        WaitTime.Shorter.execute();
        DOMElement forceDuplicate = JXBrowserHelper.selectElementByName(browser, "forcePlaceOrder");
        if (forceDuplicate != null) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            JXBrowserHelper.insertChecker(browser);
            forceDuplicate.click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
            WaitTime.Shorter.execute();
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
    }

}
