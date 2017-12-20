package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.service.ProfitLostControl;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/5/17 11:50 AM
 */
public class ProfitLostControlTest extends BaseTest {

    @BeforeClass
    public void init() {
        order = prepareOrder();

    }

    @Test
    public void testEarning() throws Exception {
        order.price = "90";
        order.quantity_purchased = "1";
        order.shipping_fee = "10";
        order.sales_chanel = "Amazon.com";
        assertEquals(ProfitLostControl.earning(order), 83.2f);


        order.price = "90";
        order.quantity_purchased = "1";
        order.shipping_fee = "10";
        order.sales_chanel = "Amazon.co.uk";

        assertEquals(ProfitLostControl.earning(order), 112.4f);

    }

    @Test
    public void testProfit() throws Exception {
        order.price = "90";
        order.quantity_purchased = "1";
        order.shipping_fee = "10";
        order.sales_chanel = "Amazon.com";

        assertEquals(ProfitLostControl.profit(order, 30f), 53.2f);
    }

    @Test
    public void testCanPlaceOrder() throws Exception {
        order.price = "90";
        order.quantity_purchased = "1";
        order.shipping_fee = "10";
        order.sales_chanel = "Amazon.com";

        assertEquals(ProfitLostControl.canPlaceOrder(order, 30f), true);

    }

}