package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/22/17 3:03 PM
 */
public class OrderAddressUtilsTest extends BaseTest {
    @Test
    public void testOrderShippingAddress() throws Exception {
    }

    @Test
    public void testUsFwdBookRecipient() throws Exception {
        Order order = prepareOrder();
        OrderAddressUtils.usFwdBookRecipient(order);
    }

}