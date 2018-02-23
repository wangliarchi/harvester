package edu.olivet.harvester.feeds.helper;

import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.feeds.helper.InventoryUpdateTypeHelper.UpdateType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class InventoryUpdateTypeHelperTest extends BaseTest {
    @Test
    public void getUpdateType() {
        Order order = prepareOrder();
        order.reference = "20";
        assertEquals(InventoryUpdateTypeHelper.getUpdateType(order), UpdateType.AddQuantity);
        order.reference = "0";
        assertEquals(InventoryUpdateTypeHelper.getUpdateType(order), UpdateType.ClearQuantity);

        order.color = "#666666";
        assertEquals(InventoryUpdateTypeHelper.getUpdateType(order), UpdateType.DeleteASIN);

        order.remark = "black list";
        assertEquals(InventoryUpdateTypeHelper.getUpdateType(order), UpdateType.DeleteASINSYNC);
    }


}