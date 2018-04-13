package edu.olivet.harvester.common.model;

import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/24/17 6:10 PM
 */
public class OrderTest extends BaseTest {
    @Test
    public void testMaxEddDays() throws Exception {
        Order order = prepareOrder();
        order.expected_ship_date = "2017-10-24 2017-10-26";
        order.estimated_delivery_date = "2017-10-04 2017-11-07";
        assertEquals(order.maxEddDays(), 12);
    }

    @Test
    public void testOrderNumberValid() throws Exception {
        Order order = prepareOrder();

        order.order_number = "112-9346936-6598646";
        assertTrue(order.orderNumberValid());

        order.order_number = "112-9346936-6598646 112-9346936-6598647";
        assertTrue(order.orderNumberValid());

        order.order_number = "112-9346936-659864a";
        assertFalse(order.orderNumberValid());

    }

    @Test
    public void testPurchaseDate() {
        //2018-04-05T01:04:03+00:00 -> 2018-04-04T21:04:03.000-0400
        //2018-04-04_18:04:03 -> 2018-04-04T21:04:03.000-0400
        Order order = prepareOrder();

        order.sales_chanel = "Amazon.com";
        order.purchase_date = "2018-04-05T01:04:03+00:00";
        Date date1 = order.getPurchaseDate();
        order.purchase_date = "2018-04-04_18:04:03";
        Date date2 = order.getPurchaseDate();
        assertEquals(date1, date2);
    }


}