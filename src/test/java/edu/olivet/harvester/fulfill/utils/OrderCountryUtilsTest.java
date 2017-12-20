package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.OrderEnums;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/5/17 10:23 AM
 */
public class OrderCountryUtilsTest extends BaseTest {
    @Test
    public void testGetShipToCountry() throws Exception {
        order = prepareOrder();

        order.ship_country = "United States";
        order.remark = "";
        order.type = OrderEnums.OrderItemType.BOOK;

        //normal order, to US
        assertEquals(OrderCountryUtils.getShipToCountry(order), "US");

        order.remark = "US FWD";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "US");

        order.ship_country = "China";
        order.remark = "";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "CN");

        order.remark = "US FWD";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "US");
        order.remark = "US Shipment";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "CN");

        order.remark = "UK FWD";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "UK");

        order.remark = "UK Shipment";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "CN");

        order.type = OrderEnums.OrderItemType.PRODUCT;
        order.remark = "";
        assertEquals(OrderCountryUtils.getShipToCountry(order), "US");
    }

    @Test
    public void testGetMarketplaceCountry() throws Exception {
        order = prepareOrder();
        order.sales_chanel = "Amazon.com";
        assertEquals(OrderCountryUtils.getMarketplaceCountry(order), Country.US);

        order.sales_chanel = "Amazon.co.uk";
        assertEquals(OrderCountryUtils.getMarketplaceCountry(order), Country.UK);
    }

    @Test
    public void testGetFulfillmentCountry() throws Exception {
        order = prepareOrder();

        order.ship_country = "United States";
        order.remark = "";
        order.type = OrderEnums.OrderItemType.BOOK;
        order.sales_chanel = "Amazon.com";

        //normal order, to US
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.US);

        order.remark = "US FWD";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.US);

        order.ship_country = "China";
        order.remark = "";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.US);

        order.remark = "US FWD";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.US);

        order.remark = "US Shipment";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.US);

        order.remark = "UK FWD";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.UK);

        order.remark = "UK Shipment";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.UK);

        order.remark = "CA Shipment";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.CA);


        order.type = OrderEnums.OrderItemType.PRODUCT;
        order.remark = "";
        assertEquals(OrderCountryUtils.getFulfillmentCountry(order), Country.US);
    }

    @Test
    public void testGetOfferListingUrl() throws Exception {
        order = prepareOrder();

        order.ship_country = "United States";
        order.remark = "";
        order.type = OrderEnums.OrderItemType.BOOK;
        order.sales_chanel = "Amazon.com";
        order.isbn = "0310445876";
        order.condition = "New";
        order.seller = "AP";
        assertEquals(OrderCountryUtils.getOfferListingUrl(order), "https://www.amazon.com/gp/offer-listing/0310445876/ref=olp_prime_new?ie=UTF8&condition=new&shipPromoFilter=1");

        order.seller_id = "A1KUFZLJ107W44";
        order.seller = "bargainbookstores";
        order.character = "pt";
        assertEquals(OrderCountryUtils.getOfferListingUrl(order), "https://www.amazon.com/gp/offer-listing/0310445876/ref=olp_tab_new?ie=UTF8&condition=new&seller=A1KUFZLJ107W44");

    }

}