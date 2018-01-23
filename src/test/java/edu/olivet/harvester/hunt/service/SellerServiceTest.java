package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.hunt.model.Rating;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.hunt.model.SellerEnums.StockStatus;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class SellerServiceTest extends BaseTest {
    @Inject SellerService sellerService;
    Browser browser;
    @Inject
    public void initBrowser() {
        BrowserView browserView = JXBrowserHelper.init("test", -1);
        browser = browserView.getBrowser();
        now.set(Dates.parseDate("2018-01-20"));
    }
    @Test
    public void testSellersUsBookUsed() {

        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "us-book-used.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));

        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.US);
        assertEquals(sellers.size(), 10);

        Seller seller = sellers.get(0);
        assertEquals(seller.getName(), "glenthebookseller");
        assertEquals(seller.getUuid(), "A27EK23NIPUXRO");
        assertEquals(seller.getType(), SellerType.Pt);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "1.25");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "3.98");
        assertEquals(seller.getCondition(), Condition.UsedGood);
        //assertEquals(seller.getConditionDetail(), "Item in good condition. Textbooks may not include supplemental items i.e. CDs, access codes etc");
        assertEquals(seller.getShipFromCountry(), Country.US);
        assertEquals(seller.isExpeditedAvailable(), true);
        assertEquals(seller.isIntlShippingAvailable(), false);
        assertEquals(seller.isInStock(), true);
        assertEquals(seller.getStockStatus(), StockStatus.InStock);
        assertEquals(seller.isAddOn(), false);
        assertEquals(seller.getRating(), 95);
        assertEquals(seller.getRatingCount(), 422541);

        seller = sellers.get(1);
        assertEquals(seller.getName(), "GoodwillSC");
        assertEquals(seller.getUuid(), "A2BWFYJ1AE0YU");
        assertEquals(seller.getType(), SellerType.Pt);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "1.24");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "3.99");
        assertEquals(seller.getCondition(), Condition.UsedGood);
        //assertEquals(seller.getConditionDetail(), "Item in good condition. Textbooks may not include supplemental items i.e. CDs, access codes etc");
        assertEquals(seller.getShipFromCountry(), Country.US);
        assertEquals(seller.getShippingFromState(), "SC");
        assertEquals(seller.isExpeditedAvailable(), false);
        assertEquals(seller.isIntlShippingAvailable(), false);
        assertEquals(seller.isInStock(), true);
        assertEquals(seller.getStockStatus(), StockStatus.InStock);
        assertEquals(seller.isAddOn(), false);
        assertEquals(seller.getRating(), 96);
        assertEquals(seller.getRatingCount(), 50245);

        seller = sellers.get(7);
        assertEquals(seller.getName(), "tacoma_goodwill");
        assertEquals(seller.getType(), SellerType.Pt);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "5.74");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "0.00");
        assertEquals(seller.getCondition(), Condition.UsedGood);
        assertEquals(seller.getShipFromCountry(), Country.US);
        assertEquals(seller.isExpeditedAvailable(), true);
        assertEquals(seller.isIntlShippingAvailable(), false);
        assertEquals(seller.isInStock(), true);
        assertEquals(seller.getStockStatus(), StockStatus.InStock);
        assertEquals(seller.isAddOn(), false);
        assertEquals(seller.getRating(), 98);
        assertEquals(seller.getRatingCount(), 103945);

        seller = sellers.get(8);
        assertEquals(seller.getName(), "Hawthorne Academic");
        assertEquals(seller.getUuid(), "A3HS0K3UQJW2XZ");
        assertEquals(seller.getType(), SellerType.Prime);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "5.85");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "0.00");
        assertEquals(seller.getCondition(), Condition.UsedGood);
        assertEquals(seller.getShipFromCountry(), Country.US);
        assertEquals(seller.isExpeditedAvailable(), true);
        assertEquals(seller.isIntlShippingAvailable(), false);
        assertEquals(seller.isInStock(), true);
        assertEquals(seller.getStockStatus(), StockStatus.InStock);
        assertEquals(seller.isAddOn(), false);
        assertEquals(seller.getRating(), 98);
        assertEquals(seller.getRatingCount(), 141042);
    }


    @Test
    public void testSellersUsBookUsedAP() {

        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "us-book-used-ap.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));

        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.US);
        assertEquals(sellers.size(), 10);

        Seller seller = sellers.get(9);
        assertEquals(seller.getName(), "AP");
        assertEquals(seller.getUuid(), "");
        assertEquals(seller.getType(), SellerType.AP);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "9.97");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "0.00");
        assertEquals(seller.getCondition(), Condition.New);
        assertEquals(seller.getShipFromCountry(), Country.US);
        assertEquals(seller.isExpeditedAvailable(), true);
        assertEquals(seller.isIntlShippingAvailable(), true);
        assertEquals(seller.isInStock(), true);
        assertEquals(seller.getStockStatus(), StockStatus.InStock);
        assertEquals(seller.isAddOn(), false);
        assertEquals(seller.getRating(), Rating.AP_POSITIVE);
        assertEquals(seller.getRatingCount(), Rating.AP_COUNT);
    }

    @Test
    public void testSellersUsOutOfStock() {

        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "us-book-outofstock.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));
        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.US);

        Seller seller = sellers.get(2);
        assertEquals(seller.getType(), SellerType.AP);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "30.00");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "0.00");
        assertEquals(seller.getCondition(), Condition.New);
        assertEquals(seller.isExpeditedAvailable(), true);
        assertEquals(seller.isIntlShippingAvailable(), true);
        assertEquals(seller.isInStock(), false);
        assertEquals(seller.getStockStatus(), StockStatus.OutOfStock);
        assertEquals(seller.isAddOn(), false);
    }

    @Test
    public void testSellersUSBackOrder() {
        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "us-book-backorder.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));
        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.US);

        Seller seller = sellers.get(1);
        assertEquals(seller.getType(), SellerType.AP);
        assertEquals(seller.getPrice().toUSDAmount().toPlainString(), "22.05");
        assertEquals(seller.getShippingFee().toUSDAmount().toPlainString(), "0.00");
        assertEquals(seller.getCondition(), Condition.New);
        assertEquals(seller.isExpeditedAvailable(), true);
        assertEquals(seller.isIntlShippingAvailable(), true);
        assertEquals(seller.isInStock(), true);
        assertEquals(seller.getStockStatus(), StockStatus.WillBeInStockSoon);
        assertEquals(seller.isAddOn(), false);
    }

    @Test
    public void testSellerShippingFromCountry() {
        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "us-book-backorder.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));
        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.US);

        assertEquals(sellers.get(1).getShipFromCountry(),Country.US);
        assertEquals(sellers.get(6).getShipFromCountry(),Country.US);
        assertEquals(sellers.get(7).getShipFromCountry(),Country.DE);


    }


    @Test
    public void testSellerShippingFromCountryDE() {
        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "de-book-used.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));
        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.DE);

        assertEquals(sellers.get(0).getShipFromCountry(),Country.UK);
        assertEquals(sellers.get(1).getShipFromCountry(),Country.DE);
        assertEquals(sellers.get(2).getShipFromCountry(),Country.UK);
        assertEquals(sellers.get(7).getShipFromCountry(),Country.US);


    }

    @Inject Now now;
    @Test
    public void testSellerEDD() {
        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "us-book-backorder.html");
        browser.loadHTML(Tools.readFileToString(htmlFile));
        WaitTime.Short.execute();

        List<Seller> sellers = sellerService.parseSellers(browser, Country.US);
        assertEquals(DateFormat.FULL_DATE.format(sellers.get(0).getLatestDeliveryDate()),"2018-02-12");
        assertEquals(DateFormat.FULL_DATE.format(sellers.get(6).getLatestDeliveryDate()),"2018-03-08");
    }


    @Test
    public void testGetSellerRatingsUs() {
        File htmlFile = new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + "seller" + File.separator + "seller-profile-us.html");
        Document document = Jsoup.parse(Tools.readFileToString(htmlFile));

        Map<RatingType, Rating> ratings = sellerService.getSellerRatings(document);
        assertEquals(ratings.get(RatingType.Last30Days).getCount(),502);
        assertEquals(ratings.get(RatingType.Last30Days).getPositive(),96);
        assertEquals(ratings.get(RatingType.Last90Days).getCount(),1825);
        assertEquals(ratings.get(RatingType.Last90Days).getPositive(),97);
        assertEquals(ratings.get(RatingType.Last12Month).getCount(),8863);
        assertEquals(ratings.get(RatingType.Last12Month).getPositive(),97);
        assertEquals(ratings.get(RatingType.Lifetime).getCount(),161885);
        assertEquals(ratings.get(RatingType.Lifetime).getPositive(),98);
    }

    @Test
    public void testParseSellers() {
        String asin = "0323377033";
        Country country = Country.US;
        Condition condition = Condition.New;
        List<Seller> sellers = sellerService.parseSellers(country,asin,condition);
        assertEquals(sellers.size(),27);

        sellers = sellerService.parseSellers(country,asin,Condition.Used);
        assertEquals(sellers.size(),30);
    }
}