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
public class SellerHuntUtilsUKBookTest extends BaseTest {
    @Inject SellerHuntUtils sellerHuntUtils;
    @Inject Now now;

    /**
     * 订单产生国AP直寄，订单产生国Prime直寄，订单产生国一般seller直寄，
     * DE AP直寄，
     * US直寄，USPrime买回，US买回。
     */
    @Test
    public void countriesToHuntUKBookLocal() {
        now.set(Dates.parseDate("2018-02-15"));
        order = prepareOrder();
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

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();
        sellerHuntUtils.addCountry(countries, Country.DE, SellerFullType.APDirect);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtExport);

        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }

    /**
     * 5.1 当订单产生国为UK
     *
     * 5.1.1 若收件人地址国家是 DE
     * 1 收件人地址国家AP 直寄：0
     * 2 收件人地址国家 Prime 直寄：0
     * 3 收件人地址国家 一般seller直寄 ：3 (只能选择ship from UK,FR.DE,ES,IT的seller)
     * 4 订单产生国AP及Prime直寄：4.4 英镑
     * 5 订单产生国一般seller直寄：4.02英镑（ 只能选择带International 选项,ship from UK,FR.DE,ES,IT的seller）
     * 6. US AP直寄，选择Expedited Shipping，13*美金兑换英镑汇率
     */
    @Test
    public void countriesToHuntUKBookToDE() {
        now.set(Dates.parseDate("2018-02-15"));
        order = prepareOrder();
        order.purchase_date = "2018-02-12_15:44:41";
        order.sku = "Used700de160817usbook-c059634";
        order.sku_address = "https://www.amazon.co.uk/dp/B01K3ISO1K";
        order.price = "29.45";
        order.shipping_fee = "3";
        order.quantity_purchased = "1";
        order.isbn = "0936756462";
        order.ship_country = "Germany";
        order.sales_chanel = "Amazon.co.uk";
        order.estimated_delivery_date = "2018-02-28 2018-03-22";

        Map<Country, Set<SellerFullType>> countries = new HashMap<>();
        sellerHuntUtils.addCountry(countries, Country.DE, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.UK, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        sellerHuntUtils.addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtExport);
        sellerHuntUtils.addCountry(countries, Country.CA, SellerFullType.APDirect, SellerFullType.PrimeDirect);
        assertEquals(sellerHuntUtils.countriesToHunt(order), countries);
    }
}
