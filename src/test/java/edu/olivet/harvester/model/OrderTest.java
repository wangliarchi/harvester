package edu.olivet.harvester.model;

import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

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
    public void testEddDays() throws Exception {
    }

}