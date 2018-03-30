package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.SellerNotFoundException;
import edu.olivet.harvester.fulfill.exception.Exceptions.SellerPriceRiseTooHighException;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.hunt.service.SellerService;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 10:08 AM
 */
public class OfferListingPage extends FulfillmentPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferListingPage.class);


    private SellerService sellerService;

    public OfferListingPage(BuyerPanel buyerPanel, SellerService sellerService) {
        super(buyerPanel);
        this.sellerService = sellerService;
    }

    public void enter(Order order) {
        String url = OrderCountryUtils.getOfferListingUrl(order);
        LOGGER.info("Offer listing page {}", url);
        JXBrowserHelper.loadPage(browser, url);
        try {
            JXBrowserHelper.waitUntilVisible(browser, "#olpProductImage");
        } catch (Exception e) {
            //
        }
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
    }

    public void addToCart(Order order) {
        long start = System.currentTimeMillis();

        //find seller
        Seller seller;
        try {
            seller = findSeller(order);
        } catch (Exception e) {
            if (order.selfOrder) {
                setPostalCode();
                seller = findSeller(order);
            } else {
                throw e;
            }
        }

        //check if seller price lifted over maximum limit
        if (OrderValidator.needCheck(order, OrderValidator.SkipValidation.SellerPrice)) {
            String result = OrderValidator.sellerPriceChangeNotExceedConfiguration(order, seller);
            if (StringUtils.isNotBlank(result)) {
                throw new SellerPriceRiseTooHighException(result);
            }

        }

        LOGGER.info("Found seller {} for order {}", seller.getName(), order.order_id);
        _addToCart(browser, seller.getIndex());
        LOGGER.info("Added to shopping cart successfully, now at {} -> {}, took {}",
                browser.getTitle(), browser.getURL(), Strings.formatElapsedTime(start));

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

    }


    public Seller findSeller(Order order) {
        //go to offerlisting page
        enter(order);
        LOGGER.info("Finding seller for order {}", order.order_id);
        Country currentCountry = OrderCountryUtils.getFulfillmentCountry(order);
        while (true) {
            List<Seller> sellers = sellerService.parseSellers(browser, currentCountry);
            if (CollectionUtils.isEmpty(sellers)) {
                break;
            }
            //List<Seller> results = new ArrayList<>();
            for (Seller seller : sellers) {
                if (rightSeller(seller, order)) {
                    return seller;
                }
            }
            //to next page
            DOMElement nextPageLink = JXBrowserHelper.selectElementByCssSelector(browser, "#olpOfferListColumn .a-pagination li.a-last a");
            if (nextPageLink != null) {
                JXBrowserHelper.insertChecker(browser);
                nextPageLink.click();
                JXBrowserHelper.waitUntilNewPageLoaded(browser);
            } else {
                break;
            }
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

        throw new SellerNotFoundException(String.format(Remark.SELLER_DISAPPEAR.text2Write(), order.seller, order.character));
    }


    @SuppressWarnings("RedundantIfStatement")
    public boolean rightSeller(Seller seller, Order order) {

        if (order.selfOrder && seller.getShippingFee().getAmount().floatValue() > 0.1) {
            return false;
        }

        //compare condition first;
        if (!order.selfOrder && !ConditionUtils.goodToGo(order.condition(), seller.getCondition())) {
            return false;
        }

        //compare by uuid
        if (StringUtils.isNotBlank(order.seller_id) && order.seller_id.equalsIgnoreCase(seller.getUuid()) &&
                seller.getType() == SellerType.getByCharacter(order.character)) {
            return true;
        }
        //compare by name
        if (StringUtils.isNotBlank(order.seller) && order.seller.equalsIgnoreCase(seller.getName()) &&
                seller.getType() == SellerType.getByCharacter(order.character)) {
            return true;
        }

        if ((seller.getType() == SellerType.AP && order.sellerIsAP())) {
            return true;
        }

        if (seller.getType() == SellerType.APWareHouse && order.sellerIsAPWarehouse()) {
            return true;
        }

        return false;
    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void _addToCart(Browser browser, int sellerIndex) {
        List<DOMElement> rows = JXBrowserHelper.selectElementsByCssSelector(browser, "div.a-row.olpOffer");
        DOMElement sellerRow = rows.get(sellerIndex);

        DOMElement addToCartBtn = JXBrowserHelper.selectElementByCssSelector(sellerRow, ".olpBuyColumn .a-button-input");
        if (addToCartBtn == null) {
            throw new BusinessException("Fail to add item to cart.");
        }
        JXBrowserHelper.insertChecker(browser);
        addToCartBtn.click();
        checkPopovers();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);
    }


    private void checkPopovers() {
        //Protection Plan popup
        try {
            WaitTime.Shorter.execute();
            DOMElement popoverElement = JXBrowserHelper.selectVisibleElement(browser, ".a-popover");
            if (popoverElement != null) {
                WaitTime.Shorter.execute();
                DOMElement closeBtn = JXBrowserHelper.selectElementByCssSelector(popoverElement, ".a-button-close.a-declarative");
                JXBrowserHelper.click(closeBtn);
                WaitTime.Short.execute();
            }
        } catch (Exception e) {
            //
        }
    }

    public static void main(String[] args) {
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);
        OfferListingPage offerListingPage = new OfferListingPage(buyerPanel, null);

        JFrame frame = new JFrame("Order Submission Demo");
        frame.getContentPane().add(buyerPanel);
        frame.setVisible(true);
        frame.setSize(new Dimension(1260, 736));

        Browser browser = buyerPanel.getBrowserView().getBrowser();
        String offerListingURL = "https://www.amazon.com//gp/offer-listing/0545521378/ref=olp_prime_new?ie=UTF8&condition=new&shipPromoFilter=1";
        JXBrowserHelper.loadPage(browser, offerListingURL);
        offerListingPage._addToCart(browser, 0);


    }

    public void execute(Order order) {

    }


    public String getDefaultPostalCode() {
        switch (country) {
            case US:
                return "10001";
            case FR:
                return "75008";
            case ES:
                return "08001";
            case IT:
                return "00194";
            case DE:
                return "10115";
            case UK:
                return "WC2N 5DU";
            case CA:
                return "M1R 0E9";
            default:
                return "";
        }
    }

    public void setPostalCode() {
        try {
            String postalCode = getDefaultPostalCode();
            if (StringUtils.isBlank(postalCode)) {
                return;
            }
            //75008 FR zip code
            JXBrowserHelper.loadPage(browser, country.baseUrl());

            String currentZip = JXBrowserHelper.textFromElement(browser, "#glow-ingress-line2");
            if (Strings.containsAnyIgnoreCase(currentZip, postalCode)) {
                return;
            }
            //
            DOMElement trigger = JXBrowserHelper.selectVisibleElement(browser, "#nav-global-location-slot .a-popover-trigger");
            trigger.click();

            JXBrowserHelper.waitUntilVisible(browser, "#GLUXZipUpdate");

            DOMElement input01 = JXBrowserHelper.selectVisibleElement(browser, "#GLUXZipUpdateInput_0");
            String[] parts = StringUtils.split(postalCode, " ");
            if (parts.length == 2 && input01 != null) {

                JXBrowserHelper.fillValueForFormField(browser, "#GLUXZipUpdateInput_0", parts[0]);
                JXBrowserHelper.fillValueForFormField(browser, "#GLUXZipUpdateInput_1", parts[1]);
            } else {
                JXBrowserHelper.waitUntilVisible(browser, ".a-popover-wrapper input[type='text']");
                JXBrowserHelper.fillValueForFormField(browser, ".a-popover-wrapper input[type='text']", postalCode);
            }


            WaitTime.Short.execute();

            JXBrowserHelper.selectVisibleElement(browser, ".a-button .a-button-inner.a-declarative").click();
            WaitTime.Shortest.execute();
            //JXBrowserHelper.waitUntilVisible(browser, ".a-button-inner.a-declarative");
            //DOMElement closeBtn = JXBrowserHelper.selectVisibleElement(browser, ".a-popover-footer .a-button-inner.a-declarative");
            //if (closeBtn != null) {
            //    JXBrowserHelper.click(closeBtn);
            //    WaitTime.Short.execute();
            //}
        } catch (Exception e) {
            //
        }
    }
}
