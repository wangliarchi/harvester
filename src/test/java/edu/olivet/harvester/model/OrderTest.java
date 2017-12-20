package edu.olivet.harvester.model;

import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

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
        assertEquals(order.maxEddDays(),12);
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



}