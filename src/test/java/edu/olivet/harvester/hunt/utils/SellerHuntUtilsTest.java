package edu.olivet.harvester.hunt.utils;

import com.google.common.collect.Lists;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SellerHuntUtilsTest extends BaseTest {
    @Test
    public void countriesToHunt() {
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

        assertEquals(SellerHuntUtils.countriesToHunt(order), Lists.newArrayList(Country.US, Country.FR, Country.UK, Country.CA));

    }

    @Test
    public void determineRemarkAppendix() {
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

}