package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums;
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
        order.setSpreadsheetId("testid");
        order.setSheetName("11/20");
        order.setType(OrderEnums.OrderItemType.BOOK);

        assertEquals(OrderAddressUtils.orderShippingAddress(order), Address.loadFromOrder(order));

        order.remark = "US FWD";

        Address address = FwdAddressUtils.getUSFwdAddress();
        address.setName("zhuanyun/700/1397838");
        assertEquals(OrderAddressUtils.orderShippingAddress(order), address);

    }

    @Test
    public void testUsFwdBookRecipient() throws Exception {
        Order order = prepareOrder();
        order.setContext("700US");
        order.order_id = "002-1578027-1397838";
        order.setSpreadsheetId("testid");
        order.setSheetName("11/20");
        order.setType(OrderEnums.OrderItemType.BOOK);
        assertEquals(FwdAddressUtils.getFwdRecipient(order), "zhuanyun/700/1397838");
    }

}