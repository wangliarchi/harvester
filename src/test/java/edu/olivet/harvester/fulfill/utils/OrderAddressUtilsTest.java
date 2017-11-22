package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.model.Order;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/22/17 3:03 PM
 */
public class OrderAddressUtilsTest extends BaseTest {

    @Test
    public void testOrderShippingAddress() throws Exception {
        Order order = prepareOrder();
        order.setContext("700US");
        order.order_id = "002-1578027-1397838";
        order.remark = "";

        assertEquals(OrderAddressUtils.orderShippingAddress(order), Address.loadFromOrder(order));

        order.remark = "US FWD";

        Address address = Address.USFwdAddress();
        address.setName("zhuanyun/700/1397838");
        assertEquals(OrderAddressUtils.orderShippingAddress(order),address);

    }

    @Test
    public void testUsFwdBookRecipient() throws Exception {
        Order order = prepareOrder();
        order.setContext("700US");
        order.order_id = "002-1578027-1397838";
        assertEquals(OrderAddressUtils.usFwdBookRecipient(order), "zhuanyun/700/1397838");
    }

}