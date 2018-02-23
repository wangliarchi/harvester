package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HuntVariableServiceDEBookTest extends BaseTest {
    @Inject HuntVariableService huntVariableService;
    @Inject Now now;


    public static Order prepareDEBookOrder() {
        order = BaseTest.prepareOrder();
        order.purchase_date = "2018-02-12_15:44:41";
        order.sku = "Used700de160817usbook-c059634";
        order.sku_address = "https://www.amazon.de/dp/B01K3ISO1K";
        order.price = "29.45";
        order.shipping_fee = "3";
        order.quantity_purchased = "1";
        order.isbn = "0936756462";
        order.ship_country = "Germany";
        order.sales_chanel = "Amazon.de";
        order.estimated_delivery_date = "2018-02-28 2018-03-22";
        return order;
    }



    @Test
    public void sellerVariableDEBook50() {

        order = prepareDEBookOrder();
        Seller seller = new Seller();

        //（1）当销售价格在0-50欧元之间
        order.price = "44";
        order.shipping_fee = "3";
        //A 如果利润达到30-45之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(4, Country.DE));

        //1 订单产生国AP直寄：0欧元
        seller.setOfferListingCountry(Country.DE);
        seller.setShipFromCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0f);

        //2 订单产生国非AP Prime直寄：1欧元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.2f);

        //3 订单产生国一般seller直寄：4欧元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 4.8f);


        seller.setOfferListingCountry(Country.UK);
        seller.setShipFromCountry(Country.UK);
        //4 UKAP直寄：1.5欧元
        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 1.8f);

        //5 UK Prime直寄：4欧元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 4.8f);
        //6 UK一般seller转运：6欧元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 7.2f);

        seller.setOfferListingCountry(Country.US);
        seller.setShipFromCountry(Country.US);
        //7 US AP直寄：2欧元
        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 2.4f);

        //8 USPrime买回：5.5
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 6.6f);
        //9 US买回：7欧元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 8.4f);
    }


    @Test
    public void sellerVariableDEBook200() {

        order = prepareDEBookOrder();
        Seller seller = new Seller();

        //（3）当销售价格在100.1-200之间：
        //A 如果利润达到120-170之间，按照以下原则加入安全值进行对比，选出利润最高的选项：
        order.price = "164";
        order.shipping_fee = "3";
        //A 如果利润达到30-45之间，按照以下原则加入安全值进行对比，选出利润最高的选项（以下是利润减掉的值，下同）：
        seller.setPrice(new Money(4, Country.DE));

        //1 订单产生国AP直寄：0欧元
        seller.setOfferListingCountry(Country.DE);
        seller.setShipFromCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 0f);

        //2 订单产生国非AP Prime直寄：1欧元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 9.6f);

        //3 订单产生国一般seller直寄：4欧元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 120f);


        seller.setOfferListingCountry(Country.UK);
        seller.setShipFromCountry(Country.UK);
        //4 UKAP直寄：1.5欧元
        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 6f);

        //5 UK Prime直寄：4欧元
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 36f);
        //6 UK一般seller转运：6欧元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 120f);

        seller.setOfferListingCountry(Country.US);
        seller.setShipFromCountry(Country.US);
        //7 US AP直寄：2欧元
        seller.setType(SellerType.AP);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 7.2f);

        //8 USPrime买回：5.5
        seller.setType(SellerType.Prime);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 36f);
        //9 US买回：7欧元
        seller.setType(SellerType.Pt);
        huntVariableService.setSellerVariable(seller, order);
        assertEquals(seller.getSellerVariable(), 42f);
    }

    @Test
    public void setIntlShippingVariableTest() {
        now.set(Dates.parseDate("02/15/2018"));
        Order order = prepareDEBookOrder();
        Seller seller = new Seller();


        //1 订单产生国AP直寄：0 欧元
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 0f);

        //2 订单产生国非AP Prime直寄：0欧元
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.Prime);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 0f);

        //3 订单产生国一般seller直寄：3欧元  -- 0
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.Prime);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 0f);

        //1 英镑=1.2欧元=1.32美元

        //4 UKAP及Prime直寄：6.04*英镑兑欧元汇率
        seller.setShipFromCountry(Country.UK);
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.Prime);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 8.0f);

        seller.setShipFromCountry(Country.UK);
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 8.0f);

        //5 非AP Prime，UK一般seller直寄：4.02* 英镑兑欧元汇率
        seller.setShipFromCountry(Country.UK);
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.Pt);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 8f);

        //6 UK一般seller转运：6.82* 英镑兑欧元汇率

        //7 US AP直寄：13*美元兑欧元汇率
        seller.setShipFromCountry(Country.US);
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 13f);

        seller.setShipFromCountry(Country.US);
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Prime);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 13f);

        //8 USPrime买回：8*美元兑欧元汇率
        //9 US买回：12*美元兑欧元汇率（本国寄到warehouse待确定？？？+warehouse寄到顾客）
        seller.setShipFromCountry(Country.US);
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 13f);


        order.ship_country = "CZ";
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 7.2f);

        order.ship_country = "RU";
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 15f);

        order.ship_country = "IN";
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 15.6f);

        order.ship_country = "ZA";
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 15.6f);
    }

}