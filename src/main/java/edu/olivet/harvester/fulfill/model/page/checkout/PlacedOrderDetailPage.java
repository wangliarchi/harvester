package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.page.FulfillmentPage;
import edu.olivet.harvester.model.Money;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.ui.BuyerPanel;
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
            order.cost = parseTotalCost();
            order.last_code = parseLastCode();
            order.account = buyer.getEmail();


            if (StringUtils.isBlank(order.quantity_fulfilled)) {
                order.quantity_fulfilled = order.quantity_purchased;
            }

            if (!order.quantity_purchased.equals(order.quantity_fulfilled)) {
                OrderHelper.addQuantChangeRemark(order, order.quantity_fulfilled);
            }

            Address address = parseShippingAddress();
            order.setFulfilledAddress(address);
            Map<String, String> items = parseItems();
            order.setFulfilledASIN(StringUtils.join(items.keySet(), ", "));

        } catch (Exception e) {
            LOGGER.error("Error parse data on order detail page", e);
            //reload page
            JXBrowserHelper.loadPage(browser, String.format("%s/gp/css/summary/edit.html/ref=typ_rev_edit?ie=UTF8&orderID=%s", buyerPanel.getCountry().baseUrl(), order.order_number));
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
        String[] parts = StringUtils.split(cityStateZip, ",");
        String city = parts[0].trim();
        String[] regionZip = StringUtils.split(parts[1].trim(), " ");
        String zip = regionZip[regionZip.length - 1];
        String state = StringUtils.join(Arrays.copyOf(regionZip, regionZip.length - 1), " ");
        address.setCity(city);
        address.setState(state);
        address.setZip(zip);
        address.setCountry(JXBrowserHelper.text(browser, ".displayAddressCountryName"));
        return address;
    }

    public Map<String, String> parseItems() {
        Map<String, String> items = new HashMap<>();
        List<DOMElement> shipments = JXBrowserHelper.selectElementsByCssSelector(browser, ".a-box.shipment");
        for (DOMElement shipment : shipments) {
            DOMElement link = JXBrowserHelper.selectElementByCssSelector(shipment, ".item-view-left-col-inner .a-link-normal");
            String asin = RegexUtils.getMatched(link.getAttribute("href"), RegexUtils.Regex.ASIN);
            String money = JXBrowserHelper.text(shipment, ".a-size-small.a-color-price");
            items.put(asin, money);
        }
        return items;
    }

    public String parseTotalCost() {
        String total = JXBrowserHelper.text(browser, "#od-subtotals .a-text-right.a-span-last .a-color-base.a-text-bold");
        try {
            Money money = Money.fromText(total, country);
            return money.toUSDAmount().toPlainString();
        } catch (Exception e) {
            //ignore
        }
        return "";
    }

    public String parseLastCode() {
        String text = JXBrowserHelper.text(browser, "#orderDetails .a-box.a-first");
        return RegexUtils.getMatched(text, "\\*\\*\\*\\* [0-9]{4}").replaceAll("\\*\\*\\*\\* ", "");
    }


}
