package edu.olivet.harvester.hunt.utils;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

public class SellerHuntUtilsTest extends BaseTest {
    @Inject SellerHuntUtils sellerHuntUtils;
    @Inject Now now;


    @Test
    public void countriesToHuntUSBookIntl() {
        now.set(Dates.parseDate("2018-01-25"));
        order = prepareOrder();
        order.purchase_date = "2018-01-24_07:39:41";
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.price = "14.85";
        order.shipping_fee = "24.95";
        order.quantity_purchased = "1";
        order.isbn = "60112816";
        order.ship_country = "France";
        order.sales_chanel = "Amazon.com";
        order.estimated_delivery_date = "2018-02-14 2018-03-08";

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtExport);
        sellerHuntUtils.addCountry(countries, Country.FR, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect );
        sellerHuntUtils.addCountry(countries, Country.CA, SellerFullType.APDirect, SellerFullType.PrimeDirect);

        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }

    @Test
    public void countriesToHuntUSBookLocal() {

        now.set(Dates.parseDate("2018-01-25"));
        order = prepareOrder();
        order.purchase_date = "2018-01-24_07:39:41";
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.price = "14.85";
        order.shipping_fee = "3.95";
        order.quantity_purchased = "1";
        order.isbn = "60112816";
        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";
        order.estimated_delivery_date = "2018-02-01 2018-02-03";

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);

        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }

    @Test
    public void countriesToHuntUSBookLocalLongEdd() {

        now.set(Dates.parseDate("2018-01-25"));
        order = prepareOrder();
        order.purchase_date = "2018-01-24_07:39:41";
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.price = "14.85";
        order.shipping_fee = "3.95";
        order.quantity_purchased = "1";
        order.isbn = "60112816";
        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";
        order.estimated_delivery_date = "2018-02-14 2018-03-08";

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }


    @Test
    public void countriesToHuntCABookLocal() {
        now.set(Dates.parseDate("2018-01-25"));
        order = prepareOrder();
        order.purchase_date = "2018-02-03T21:07:00+00:00";
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.price = "58.46";
        order.shipping_fee = "6.49";
        order.quantity_purchased = "1";
        order.isbn = "0801427231";
        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";
        order.estimated_delivery_date = "2018-02-26 2018-03-20";

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        sellerHuntUtils.addCountry(countries, Country.CA, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtExport);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);

        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }


    @Test
    public void determineRemarkAppendixUSBookLocal() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "");
    }

    @Test
    public void determineRemarkAppendixUSBookExport() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United Kingdom";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "US FWD");
    }

    @Test
    public void determineRemarkAppendixUSBookIntlDirectShip() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United Kingdom";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.AP);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "");
    }

    @Test
    public void determineRemarkAppendixUSProductLocal() {
        order = prepareOrder();
        order.sku = "XinUSPro2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "");
    }

    @Test
    public void determineRemarkAppendixUSProductExport() {
        order = prepareOrder();
        order.sku = "XinUSPro2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United Kingdom";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "US FWD");
    }

    @Test
    public void determineRemarkAppendixUSBookIntlUKFWD() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.Prime);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK Shipment");
    }

    @Test
    public void determineRemarkAppendixUSBookIntlUKShipment() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United States";
        order.sales_chanel = "Amazon.com";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.AP);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK Shipment");
    }


    @Test
    public void determineRemarkAppendixCABookLocal() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.CA);
        seller.setType(SellerType.Pt);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "");
    }

    @Test
    public void determineRemarkAppendixCABookExport() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "US FWD");
    }

    @Test
    public void determineRemarkAppendixCABookIntlDirectShip() {
        order = prepareOrder();
        order.sku = "XinUSBk2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.AP);

        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "US Shipment");


        seller.setOfferListingCountry(Country.UK);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK Shipment");
    }


    @Test
    public void determineRemarkAppendixCAProductExport() {
        order = prepareOrder();
        order.sku = "XinUSPro2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "Canada";
        order.sales_chanel = "Amazon.ca";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "");

        seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.AP);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "US Shipment");

        seller = new Seller();
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.Pt);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK FWD");

        seller = new Seller();
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.AP);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK Shipment");
    }

    @Test
    public void determineRemarkAppendixEUProduct() {
        order = prepareOrder();
        order.sku = "XinUSPro2016-0819-C8EA1F4A32A";
        order.sku_address = "https://www.amazon.com/dp/B011MEXATU";
        order.ship_country = "United Kingdom";
        order.sales_chanel = "Amazon.co.uk";

        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.Pt);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "");

        seller = new Seller();
        seller.setOfferListingCountry(Country.US);
        seller.setType(SellerType.AP);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "US Shipment");

        seller = new Seller();
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.Pt);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK Shipment");

        seller = new Seller();
        seller.setOfferListingCountry(Country.UK);
        seller.setType(SellerType.AP);
        assertEquals(SellerHuntUtils.determineRemarkAppendix(seller, order), "UK Shipment");
    }

}