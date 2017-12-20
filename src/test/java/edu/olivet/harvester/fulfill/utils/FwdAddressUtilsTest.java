package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/4/17 6:28 PM
 */
public class FwdAddressUtilsTest extends BaseTest {
    @Test
    public void testGetFwdRecipient() throws Exception {
        String spreadsheetId = "1Xmjr6XC2nGFoqeK_Zf_u9BCP2cnMTo0cZGugFNH07OE";
        String sheetName = "11/28";
        List<Order> orders = appScript.readOrders(spreadsheetId, sheetName);
        Order order = orders.get(6);
        order.setType(OrderEnums.OrderItemType.PRODUCT);
        order.setContext("704CA");

        assertEquals(FwdAddressUtils.getFwdRecipient(order), "Tab/11/28/704CA/006");


        order = orders.get(10);
        order.setType(OrderEnums.OrderItemType.PRODUCT);
        order.setContext("704CA");
        assertEquals(FwdAddressUtils.getFwdRecipient(order), "Tab/11/28/704CA/007");
    }

    @Test
    public void testGetLastFWDIndex() throws Exception {
        String spreadsheetId = "1Xmjr6XC2nGFoqeK_Zf_u9BCP2cnMTo0cZGugFNH07OE";
        String sheetName = "11/28";
        List<Order> orders = appScript.readOrders(spreadsheetId, sheetName);
        int lastIndex = FwdAddressUtils.getLastFWDIndex(spreadsheetId, sheetName, orders);
        assertEquals(lastIndex, 5);

    }


    @Test
    public void testUsFwdBookRecipient() throws Exception {
    }

    @Test
    public void testUsFwdProductRecipient() throws Exception {
    }

}