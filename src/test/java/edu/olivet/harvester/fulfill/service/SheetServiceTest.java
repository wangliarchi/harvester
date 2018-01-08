package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/21/17 2:21 PM
 */

public class SheetServiceTest extends BaseTest {

    @Inject SheetService sheetService;
    @Inject AppScript appScript;

    @Test
    public void testLocateOrder() throws Exception {
        Order order = prepareOrder();
        order.spreadsheetId = "19OalGYOM9pR-51SdY4dXcR_hJyAPDXdJaWGFavthX_s";
        order.sheetName = "11/09";
        order.status = "a";
        order.order_id = "113-6791203-7119418";
        order.recipient_name = "Stepheny Petitte";
        order.isbn = "1850788529";
        order.seller = "FTBOOKS";
        order.seller_id = "A1B5RR99M22Q6W";
        order.sku = "JiuMC710-20170825-usbook-c267783";
        order.quantity_purchased = "2";
        order.condition = "Used - Good";
        order.character = "pt";

        assertEquals(sheetService.locateOrder(order), 11);


    }

    @Test
    public void testLocateOrders() throws Exception {
        List<Order> orders = appScript.readOrders("1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI", "test");
        for (Order order : orders) {
            assertEquals(order.row, sheetService.reloadOrder(order).row);
        }
    }
    //

}