package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/5/17 10:22 AM
 */
public class OrderBuyerUtilsTest extends BaseTest {
    @Test
    public void testGetBuyer() throws Exception {
        Settings settings = Settings.load();
        order = prepareOrder();

        order.ship_country = "United States";
        order.remark = "";
        order.type = OrderEnums.OrderItemType.BOOK;
        order.sales_chanel = "Amazon.com";
        order.isbn = "0310445876";
        order.condition = "New";
        order.seller = "AP";

        RuntimeSettings runtimeSettings = RuntimeSettings.load();

        //normal order, to US
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());

        order.seller_id = "A1KUFZLJ107W44";
        order.seller = "bargainbookstores";
        order.character = "pt";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getBuyer());

        order.isbn = "0310445876";
        order.condition = "New";
        order.seller = "AP";
        order.ship_country = "China";
        order.remark = "";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());


        order.remark = "US FWD";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());

        order.remark = "US Shipment";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());

        order.remark = "UK FWD";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());

        order.remark = "UK Shipment";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());

        order.remark = "CA Shipment";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getPrimeBuyer());


        order.type = OrderEnums.OrderItemType.PRODUCT;
        order.remark = "";
        assertEquals(OrderBuyerUtils.getBuyer(order, runtimeSettings), settings.getConfigByCountry(Country.US).getProdPrimeBuyer());
    }

}