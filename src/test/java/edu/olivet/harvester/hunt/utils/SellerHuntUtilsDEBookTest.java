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
public class SellerHuntUtilsDEBookTest extends BaseTest {
    @Inject SellerHuntUtils sellerHuntUtils;
    @Inject Now now;

    @Test
    public void countriesToHuntDEBookLocal() {
        //订单产生国AP直寄，订单产生国Prime直寄，订单产生国一般seller直寄，
        // UK AP直寄，UK一般seller直寄  - 所以UK seller都默认 可以国际直寄
        // US AP直寄，USPrime 买回，US买回。US BOOK Prime 都默认可以国际直寄
        now.set(Dates.parseDate("2018-02-15"));
        order = prepareOrder();
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

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        sellerHuntUtils.addCountry(countries, Country.DE, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtExport);

        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }
}
