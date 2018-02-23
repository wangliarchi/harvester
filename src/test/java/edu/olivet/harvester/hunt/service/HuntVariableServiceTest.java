package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class HuntVariableServiceTest extends BaseTest {
    @Inject HuntVariableService huntVariableService;

    @Test
    public void getHuntStandardUSBook() {
        //可选旧书类/CD，本年好评率90%，本年rating数60，本月好评率90%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.US, OrderItemType.BOOK, Condition.UsedGood);
        assertEquals(standard.getYearlyRating().getPositive(), 90);
        assertEquals(standard.getYearlyRating().getCount(), 60);
        assertEquals(standard.getMonthlyRating().getPositive(), 90);
        assertEquals(standard.getMonthlyRating().getCount(), 1);


        //可选新书类/CD，本年好评率93%，本年rating数60，本月好评率93%，本月好评数1。
        standard = huntVariableService.getHuntStandard(Country.US, OrderItemType.BOOK, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 93);
        assertEquals(standard.getYearlyRating().getCount(), 60);
        assertEquals(standard.getMonthlyRating().getPositive(), 93);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }

    @Test
    public void getHuntStandardUSProduct() {
        //产品，本年好评率85%，本年rating数30，本月好评率80%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.US, OrderItemType.PRODUCT, Condition.UsedGood);
        assertEquals(standard.getYearlyRating().getPositive(), 85);
        assertEquals(standard.getYearlyRating().getCount(), 30);
        assertEquals(standard.getMonthlyRating().getPositive(), 80);
        assertEquals(standard.getMonthlyRating().getCount(), 1);


        standard = huntVariableService.getHuntStandard(Country.US, OrderItemType.PRODUCT, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 85);
        assertEquals(standard.getYearlyRating().getCount(), 30);
        assertEquals(standard.getMonthlyRating().getPositive(), 80);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }


    @Test
    public void getHuntStandardProduct() {
        //产品，本年好评率85%，本年rating数30，本月好评率80%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.US, OrderItemType.PRODUCT, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 85);
        assertEquals(standard.getYearlyRating().getCount(), 30);
        assertEquals(standard.getMonthlyRating().getPositive(), 80);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }


    @Test
    public void getHuntStandardUKBook() {
        //可选旧书类/CD，本年好评率90%，本年rating数60，本月好评率90%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.UK, OrderItemType.BOOK, Condition.UsedGood);
        assertEquals(standard.getYearlyRating().getPositive(), 90);
        assertEquals(standard.getYearlyRating().getCount(), 60);
        assertEquals(standard.getMonthlyRating().getPositive(), 90);
        assertEquals(standard.getMonthlyRating().getCount(), 1);


        //可选新书类/CD，本年好评率93%，本年rating数60，本月好评率93%，本月好评数1。
        standard = huntVariableService.getHuntStandard(Country.UK, OrderItemType.BOOK, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 93);
        assertEquals(standard.getYearlyRating().getCount(), 60);
        assertEquals(standard.getMonthlyRating().getPositive(), 93);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }

    @Test
    public void getHuntStandardUKProduct() {
        //产品，本年好评率85%，本年rating数30，本月好评率80%，本月好评数1。
        HuntStandard standard = huntVariableService.getHuntStandard(Country.UK, OrderItemType.PRODUCT, Condition.New);
        assertEquals(standard.getYearlyRating().getPositive(), 85);
        assertEquals(standard.getYearlyRating().getCount(), 30);
        assertEquals(standard.getMonthlyRating().getPositive(), 80);
        assertEquals(standard.getMonthlyRating().getCount(), 1);
    }

    @Test
    public void sellerVariableUSBook50() {
        //https://docs.google.com/document/d/1TjZoNLB1vm61YWZTN2xpkF6ObqTyLqidGbzs2BObx1I/edit
        order = prepareOrder();

        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";
        order.sku = "new18140915a160118";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);


        //（1）当销售价格在0-50美元之间
        order.price = "47";
        order.shipping_fee = "3";

        //A 如果利润达到30.01-45之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        //1 US AP直寄：0 美元
        seller.setPrice(new Money(3, Country.US));
        seller.setShippingFee(new Money(0, Country.US));
        seller.setShipFromCountry(Country.US);

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable() - seller.getTaxVariable(), 0.0f);
        //2 US Prime直寄：1 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.0f);
        //3 US 一般seller直寄：4 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 4.0f);

        //B 如果利润达到20.01-30之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(15, Country.US));


        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：1 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.0f);
        //3 US 一般seller直寄：2.5 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 2.5f);

        //C 如果利润达到10.01-20之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(25, Country.US));


        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：0.5 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.5f);
        //3 US 一般seller直寄：1.5 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.5f);

        //D 如果利润达到5.01-10之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(35, Country.US));


        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：0 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //3 US 一般seller直寄：1 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.0f);


        //D 如果利润达到-5-5之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(45, Country.US));

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：0 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //3 US 一般seller直寄：1 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.0f);
    }

    @Test
    public void sellerVariableUSBook100() {
        //https://docs.google.com/document/d/1TjZoNLB1vm61YWZTN2xpkF6ObqTyLqidGbzs2BObx1I/edit
        order = prepareOrder();

        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";
        order.sku = "new18140915a160118";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);

        order.price = "97";
        order.shipping_fee = "3";

        //A  如果利润达到65.01-85之间：
        //1 US AP直寄：0 美元
        seller.setPrice(new Money(3, Country.US));
        seller.setShippingFee(new Money(0, Country.US));
        seller.setShipFromCountry(Country.US);
        assertEquals(seller.profit(order), 80.2f);

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：3 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 3.0f);
        //3 US 一般seller直寄：5 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 5.0f);

        //B  如果利润达到45.01-65之间：
        seller.setPrice(new Money(20, Country.US));

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);

        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 2.5f);

        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 4f);


        //C 如果利润达到25.01-45之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(40, Country.US));
        //assertEquals(seller.profit(order), 39.199997f);

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：0.5 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.5f);
        //3 US 一般seller直寄：1.5 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 3f);

        //D 如果利润达到15.01-25之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(65, Country.US));
        //assertEquals(seller.profit(order), 15.199997f);

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：0 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.0f);
        //3 US 一般seller直寄：1 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 2.0f);


        //D 如果利润达到0-15之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(80, Country.US));

        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0.0f);
        //2 US Prime直寄：0 美元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.0f);
        //3 US 一般seller直寄：1 美元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 2.0f);
    }
}