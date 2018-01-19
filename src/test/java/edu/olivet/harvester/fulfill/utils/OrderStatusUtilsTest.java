package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.OrderEnums;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/5/17 11:41 AM
 */
public class OrderStatusUtilsTest extends BaseTest {
    @Test
    public void testDetermineStatus() throws Exception {
        order = prepareOrder();

        order.ship_country = "United Kingdom";
        order.sales_chanel = "amazon.co.uk";
        order.type = OrderEnums.OrderItemType.PRODUCT;
        order.remark = "UK Shipment";
        order.seller = "";
        order.seller_id = "";
        order.character = "AP";
        //UK 直寄，p
        assertEquals(OrderStatusUtils.determineStatus(order), "p");

        //US FWD
        order.remark = "";
        assertEquals(OrderStatusUtils.determineStatus(order), "pm");
    }

}