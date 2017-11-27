package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.Seller;
import edu.olivet.harvester.fulfill.model.SellerEnums;
import edu.olivet.harvester.fulfill.service.SellerService;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.OrderValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
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
        LOGGER.info("Offer listing page {}",url);
        JXBrowserHelper.loadPage(browser, url);
        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");
    }

    public void addToCart(Order order) {
        long start = System.currentTimeMillis();
        //go to offerlisting page
        enter(order);

        //find seller
        Seller seller = findSeller(order);

        if (OrderValidator.needCheck(order, OrderValidator.SkipValidation.SellerPrice)) {
            String result = OrderValidator.sellerPriceChangeNotExceedConfiguration(order, seller);
            if (StringUtils.isNotBlank(result)) {
                throw new BusinessException(result);
            }

        }

        LOGGER.info("Found seller {} for order {}", seller.getName(), order.order_id);
        _addToCart(browser, seller.getIndex());
        LOGGER.info("Added to shopping cart successfully, now at {} -> {}, took {}", browser.getTitle(), browser.getURL(), Strings.formatElapsedTime(start));

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "1");

    }

    public Seller findSeller(Order order) {

        LOGGER.info("Finding seller for order {}", order.order_id);

        Country currentCountry = OrderCountryUtils.getFulfillementCountry(order);
        List<Seller> sellers = sellerService.parseSellers(browser, currentCountry);

        //List<Seller> results = new ArrayList<>();
        for (Seller seller : sellers) {
            String sellerName = seller.getName();
            boolean sellerEq = (StringUtils.isNotBlank(seller.getUuid()) && StringUtils.isNotBlank(order.seller_id) && seller.getUuid().equalsIgnoreCase(order.seller_id) && seller.getType().abbrev().equalsIgnoreCase(order.character)) ||
                    (StringUtils.isNotBlank(sellerName) && StringUtils.isNotBlank(order.seller) && sellerName.equalsIgnoreCase(order.seller) && seller.getType().abbrev().equalsIgnoreCase(order.character)) ||
                    (seller.getType() == SellerEnums.SellerType.AP && order.sellerIsAP()) ||
                    (seller.getType() == SellerEnums.SellerType.APWareHouse && order.sellerIsAPWarehouse());

            //ConditionUtils
            if (sellerEq && ConditionUtils.goodToGo(order.condition, seller.getCondition())) {
                return seller;
            }
        }

        throw new BusinessException(String.format(Remark.SELLER_DISAPPEAR.text2Write(), order.seller, order.character));
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public void _addToCart(Browser browser, int sellerIndex) {
        List<DOMElement> rows = JXBrowserHelper.selectElementsByCssSelector(browser, "div.a-row.olpOffer");
        DOMElement sellerRow = rows.get(sellerIndex);

        DOMElement addToCartBtn = JXBrowserHelper.selectElementByCssSelector(sellerRow, ".olpBuyColumn .a-button-input");
        JXBrowserHelper.insertChecker(browser);
        addToCartBtn.click();
        JXBrowserHelper.waitUntilNewPageLoaded(browser);
        //wait until new page loaded
        //JXBrowserHelper.wait(browser, By.cssSelector("#hlb-ptc-btn-native"));
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


}
