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

public class HuntVariableServiceUKBookTest extends BaseTest {
    @Inject HuntVariableService huntVariableService;
    @Inject Now now;


    public static Order prepareUKBookOrder() {
        order = BaseTest.prepareOrder();
        order.purchase_date = "2018-02-12_15:44:41";
        order.sku = "Used700de160817usbook-c059634";
        order.sku_address = "https://www.amazon.co.uk/dp/B01K3ISO1K";
        order.price = "29.45";
        order.shipping_fee = "3";
        order.quantity_purchased = "1";
        order.isbn = "0936756462";
        order.ship_country = "United Kingdom";
        order.sales_chanel = "Amazon.co.uk";
        order.estimated_delivery_date = "2018-02-28 2018-03-22";
        return order;
    }

    /**
     * 1 订单产生国AP直寄：p（当购买价小于10时，p=2.8， 当购买价格大于等于10时，p=0）英镑
     * 2 订单产生国非AP Prime直寄：p（当购买价小于10时，p=2.8， 当购买价格大于等于10时，p=0）英镑
     * 3 订单产生国一般seller直寄：2.8英镑
     * 4 UK转运：5.6英镑
     * 5 DE AP直寄：3* 欧元兑英镑汇率
     * 6 US AP直寄：13*美元兑英镑汇率
     * 7 USPrime买回：8*美元兑英镑汇率
     * 8 US买回：12*美元兑英镑汇率
     */
    @Test
    public void setIntlShippingVariableTest() {
        now.set(Dates.parseDate("02/15/2018"));
        Order order = prepareUKBookOrder();
        Seller seller = new Seller();

        seller.setShipFromCountry(Country.UK);
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.AP);
        seller.setPrice(new Money(5, Country.UK));
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 3.93f);

        seller.setPrice(new Money(30, Country.UK));
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 0f);


        //5 UK一般seller直寄：4.02* 英镑兑欧元汇率
        seller.setType(SellerType.Pt);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 0f);

        //7 DE AP直寄：3欧元
        seller.setShipFromCountry(Country.DE);
        seller.setOfferListingCountry(Country.DE);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 3.6f);

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
    }


    /**
     * 4 订单产生国AP及Prime直寄：4.4 英镑
     5 订单产生国一般seller直寄：4.02英镑（ 只能选择带International 选项,ship from UK,FR.DE,ES,IT的seller）
     6. US AP直寄，选择Expedited Shipping，13*美金兑换英镑汇率

     */
    @Test
    public void setIntlShippingVariableTestIntl() {
        now.set(Dates.parseDate("02/15/2018"));
        Order order = prepareUKBookOrder();
        Seller seller = new Seller();
        order.ship_country = "Germany";

        //订单产生国AP及Prime直寄：4.4 英镑
        seller.setShipFromCountry(Country.UK);
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 8f);

        order.ship_country = "CZ";
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 7.92f);

        order.ship_country = "RU";
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 11.2f);

        order.ship_country = "IN";
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 11.2f);

        order.ship_country = "ZA";
        seller.setType(SellerType.AP);
        huntVariableService.setIntlShippingVariable(seller, order);
        assertEquals(seller.getShippingVariable(), 11.2f);
    }
}