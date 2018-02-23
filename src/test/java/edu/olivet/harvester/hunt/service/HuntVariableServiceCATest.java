package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.Rating;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HuntVariableServiceCATest extends BaseTest {
    @Inject HuntVariableService huntVariableService;
    @Inject Now now;

    @Test
    public void getHuntStandardCABook() {
        //可选旧书类/CD，本年好评率90%，本年rating数60，本月好评率90%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.CA, OrderItemType.BOOK, Condition.UsedGood);
        assertEquals(standard.getYearlyRating().getPositive(), 90);
        assertEquals(standard.getYearlyRating().getCount(), 60);
        assertEquals(standard.getMonthlyRating().getPositive(), 90);
        assertEquals(standard.getMonthlyRating().getCount(), 1);


        //可选新书类/CD，本年好评率93%，本年rating数60，本月好评率93%，本月好评数1。
        standard = huntVariableService.getHuntStandard(Country.CA, OrderItemType.BOOK, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 93);
        assertEquals(standard.getYearlyRating().getCount(), 60);
        assertEquals(standard.getMonthlyRating().getPositive(), 93);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }

    @Test
    public void getHuntStandardCAProduct() {
        //产品，本年好评率85%，本年rating数30，本月好评率80%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.CA, OrderItemType.PRODUCT, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 85);
        assertEquals(standard.getYearlyRating().getCount(), 30);
        assertEquals(standard.getMonthlyRating().getPositive(), 80);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }

    /**
     * US和UK Seller rating 安全值一样，如下：
     * AP  安全值 设为0美元
     *
     * Prime seller：
     * 80% <=Rating 率 < 85% ：安全值 设为3美元
     * 85% <=Rating 率 < 90% ：安全值 设为2美元
     * 90% <=Rating 率 < 95% ：安全值 设为1美元
     * 95% <=Rating 率：安全值 设为0美元
     *
     * 普通seller
     * 80% <=Rating 率 < 85% ：安全值 设为4美元
     * 85% <=Rating 率 < 90% ：安全值 设为3美元
     * 90% <=Rating 率 < 95% ：安全值 设为2美元
     * 95% <=Rating 率：安全值 设为1美元
     */
    @Test
    public void sellerVariableUSProduct() {
        order = prepareOrder();

        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";
        order.sku = "newpro18140915a160118";
        order.price = "47";
        order.shipping_fee = "3";
        order.purchase_date = "2018-02-03_06:01:10";
        order.estimated_delivery_date = "2018-02-26 2018-03-19";
        order.original_condition = "New-New";

        now.set(Dates.parseDate("02/05/2018"));

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setShipFromCountry(Country.US);
        seller.setPrice(new Money(10, Country.US));
        seller.setShippingFee(new Money(0, Country.US));
        seller.setCondition(Condition.New);

        seller.getRatings().put(RatingType.Last30Days, new Rating(97, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 1.0f);

        seller.getRatings().put(RatingType.Last30Days, new Rating(95, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 1.0f);

        seller.getRatings().put(RatingType.Last30Days, new Rating(93, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 1.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 2.0f);

        seller.getRatings().put(RatingType.Last30Days, new Rating(90, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 1.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 2.0f);


        seller.getRatings().put(RatingType.Last30Days, new Rating(88, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 2.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 3.0f);


        seller.getRatings().put(RatingType.Last30Days, new Rating(85, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 2.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 3.0f);

        seller.getRatings().put(RatingType.Last30Days, new Rating(83, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 3.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 4.0f);
    }

    /**
     * US和UK Seller rating 安全值一样，如下：
     * AP  安全值 设为0美元
     *
     * Prime seller：
     * 80% <=Rating 率 < 85% ：安全值 设为6美元
     * 85% <=Rating 率 < 90% ：安全值 设为4美元
     * 90% <=Rating 率 < 95% ：安全值 设为2美元
     * 95% <=Rating 率：安全值 设为0美元
     *
     * 普通seller
     * 80% <=Rating 率 < 85% ：安全值 设为8美元
     * 85% <=Rating 率 < 90% ：安全值 设为6美元
     * 90% <=Rating 率 < 95% ：安全值 设为4美元
     * 95% <=Rating 率：安全值 设为2美元
     */
    @Test
    public void sellerVariableUSProduct80() {
        order = prepareOrder();

        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";
        order.sku = "newpro18140915a160118";
        order.price = "87";
        order.shipping_fee = "3";
        order.purchase_date = "2018-02-03_06:01:10";
        order.estimated_delivery_date = "2018-02-26 2018-03-19";
        order.original_condition = "New-New";

        now.set(Dates.parseDate("02/05/2018"));

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setShipFromCountry(Country.US);
        seller.setPrice(new Money(10, Country.US));
        seller.setShippingFee(new Money(0, Country.US));
        seller.setCondition(Condition.New);

        seller.getRatings().put(RatingType.Last30Days, new Rating(97, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 2.0f);



        seller.getRatings().put(RatingType.Last30Days, new Rating(93, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 2.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 4.0f);



        seller.getRatings().put(RatingType.Last30Days, new Rating(88, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 4.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 6.0f);



        seller.getRatings().put(RatingType.Last30Days, new Rating(83, 100, RatingType.Last30Days));
        seller.setType(SellerType.AP);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 0.0f);
        seller.setType(SellerType.Prime);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 6.0f);
        seller.setType(SellerType.Pt);
        huntVariableService.setHuntingVariable(seller, order);
        assertEquals(seller.getRatingVariable() + seller.getSellerVariable(), 8.0f);
    }
}