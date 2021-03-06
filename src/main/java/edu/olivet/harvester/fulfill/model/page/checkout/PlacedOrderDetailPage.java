package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 4:38 PM
 */
public class PlacedOrderDetailPage extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlacedOrderDetailPage.class);

    public PlacedOrderDetailPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    @Repeat
    public void execute(Order order) {
        try {
            JXBrowserHelper.wait(browser, By.cssSelector("#orderDetails"));
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            order.order_number = parseOrderId();
            order.orderTotalCost = parseTotalCost();
            order.last_code = parseLastCode();
            order.account = buyer.getEmail();


            if (StringUtils.isBlank(order.quantity_fulfilled)) {
                order.quantity_fulfilled = order.quantity_purchased;
            }

            if (!order.quantity_purchased.equals(order.quantity_fulfilled)) {
                OrderHelper.addQuantityChangeRemark(order, order.quantity_fulfilled);
            }

            Address address = parseShippingAddress();
            order.setFulfilledAddress(address);
            Map<String, Integer> items = parseItems();
            order.setFulfilledASIN(StringUtils.join(items.keySet(), ", "));

        } catch (Exception e) {
            LOGGER.error("Error parse data on order detail page", e);
            //reload page
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
            JXBrowserHelper.loadPage(browser,
                    String.format("%s/gp/css/summary/edit.html/ref=typ_rev_edit?ie=UTF8&orderID=%s",
                            buyerPanel.getCountry().baseUrl(), order.order_number));
            throw new BusinessException(e);
        }


    }


    public String parseOrderId() {
        String text = JXBrowserHelper.text(browser, "#orderDetails");
        return RegexUtils.getMatched(text, RegexUtils.Regex.AMAZON_ORDER_NUMBER);
    }

    public Address parseShippingAddress() {
        Address address = new Address();
        address.setName(JXBrowserHelper.text(browser, ".displayAddressFullName"));
        address.setAddress1(JXBrowserHelper.text(browser, ".displayAddressAddressLine1"));
        address.setAddress2(JXBrowserHelper.text(browser, ".displayAddressAddressLine2"));


        String cityStateZip = JXBrowserHelper.text(browser, ".displayAddressCityStateOrRegionPostalCode");
        try {
            String[] parts = StringUtils.split(cityStateZip, ",");
            String city = parts[0].trim();
            String[] regionZip = StringUtils.split(parts[1].trim(), " ");
            String zip = regionZip[regionZip.length - 1];
            String state = StringUtils.join(Arrays.copyOf(regionZip, regionZip.length - 1), " ");
            address.setCity(city);
            address.setState(state);
            address.setZip(zip);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        address.setCountry(JXBrowserHelper.text(browser, ".displayAddressCountryName"));
        address.setNoInvoiceText(buyerPanel.getOrder().getRuntimeSettings().getNoInvoiceText());
        return address;
    }

    public Map<String, Integer> parseItems() {
        Map<String, Integer> items = new HashMap<>();
        List<DOMElement> shipments = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box.shipment .item-view-left-col-inner");
        for (DOMElement shipment : shipments) {
            DOMElement link = JXBrowserHelper.selectElementByCssSelector(shipment, ".a-link-normal");
            String asin = RegexUtils.getMatched(link.getAttribute("href"), RegexUtils.Regex.ASIN);
            //String money = JXBrowserHelper.text(shipment, ".a-size-small.a-color-price");
            int qty = 1;
            try {
                String qtyString = JXBrowserHelper.text(shipment, ".item-view-qty");
                if (StringUtils.isNotBlank(qtyString)) {
                    qty = Integer.parseInt(qtyString);
                }
            } catch (Exception e) {
                //
            }
            if (items.containsKey(asin)) {
                items.put(asin, qty + items.get(asin));
            } else {
                items.put(asin, qty);
            }

        }
        return items;
    }


    public Money parseTotalCost() {
        List<DOMElement> totalTrs = JXBrowserHelper.selectElementsByCssSelector(browser, "#od-subtotals .a-text-right.a-span-last");
        for (DOMElement totalTr : totalTrs) {
            if (Strings.containsAnyIgnoreCase(totalTr.getInnerText(), "USD")) {
                String total = JXBrowserHelper.text(totalTr, ".a-color-base.a-text-bold");
                if (StringUtils.isNotBlank(total)) {
                    float amount = Money.getAmountFromText(total, Country.US);
                    if (amount > 0) {
                        return new Money(amount, Country.US);
                    }
                }
            }
        }

        String total = JXBrowserHelper.text(browser, "#od-subtotals .a-text-right.a-span-last .a-color-base.a-text-bold");
        try {
            return Money.fromText(total, country);
        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    public String parseLastCode() {
        String text = JXBrowserHelper.text(browser, "#orderDetails .a-box.a-first");
        return RegexUtils.getMatched(text, "\\*\\*\\*\\* [0-9]{4}").replaceAll("\\*\\*\\*\\* ", "");
    }


}
