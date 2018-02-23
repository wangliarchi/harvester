package edu.olivet.harvester.hunt.utils;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/15/2018 2:26 PM
 */
public class SellerHuntUtilsEUBookTest extends BaseTest {
    @Inject SellerHuntUtils sellerHuntUtils;
    @Inject Now now;

    @Test
    public void countriesToHuntDEBookLocal() {
        //订单产生国AP直寄，订单产生国Prime直寄，订单产生国一般seller直寄，
        //UK AP直寄，UK一般seller直寄，UK Prime直寄
        //DE AP直寄，
        //US直寄，USPrime买回，US买回。

        now.set(Dates.parseDate("2018-02-15"));
        order = prepareOrder();
        order.purchase_date = "2018-02-12_15:44:41";
        order.sku = "Used700de160817usbook-c059634";
        order.sku_address = "https://www.amazon.fr/dp/B01K3ISO1K";
        order.price = "29.45";
        order.shipping_fee = "3";
        order.quantity_purchased = "1";
        order.isbn = "0936756462";
        order.ship_country = "France";
        order.sales_chanel = "Amazon.fr";
        order.estimated_delivery_date = "2018-02-28 2018-03-22";

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        sellerHuntUtils.addCountry(countries, Country.FR, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.DE, SellerFullType.APDirect);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtExport);

        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }
}
