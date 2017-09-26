package edu.olivet.harvester.feeds;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

@Guice
public class ConfirmShipmentsTest {

    @Inject private ConfirmShipments confirmShipments;
    @Inject private AppScript appScript;

    @Test
    public void testGetOrdersFromWorksheet() throws Exception {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);

        assertEquals(orders.size(),7);

        assertEquals(orders.get(0).sku,"JiuXMZLCustextbkAug22-2017-C726348");
        assertEquals(orders.get(6).status,"fi09月/25日已移表");


    }

    @Test(expectedExceptions = NoWorksheetFoundException.class)
    public void testGetOrdersFromWorksheetInValidSheetName() throws Exception {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, "09/222");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);
    }

    @Test(expectedExceptions = NoOrdersFoundInWorksheetException.class)
    public void testGetOrdersFromWorksheetNoData() throws Exception {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, "template");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);
    }

    @Test
    public void testRemoveDulicatedOrders() {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, "08/31");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);

        Map<String, Order> filterd = confirmShipments.removeDulicatedOrders(orders);

        assertEquals(filterd.size(),10);

        assertEquals(filterd.keySet(),new HashSet<>(Arrays.asList("112-9914696-8464248",
                "114-6213194-8837063",
                "114-6532260-0041852",
                "111-3140015-6109021",
                "112-1487598-0950651",
                "114-0961383-9264229",
                "112-9421439-0058664",
                "113-8382913-6392256",
                "112-1328432-8909803",
                "114-6250525-1507456")));

    }

    @Test
    public void testRemoveWC() {
        Order order = BaseTest.prepareOrder();
        order.status = "wc";

        Map<String, Order> orders = new HashMap<String, Order>();
        orders.put(order.order_id,order);

        Map<String, Order> filtered = confirmShipments.removeWCGrayLabelOrders(orders);

        assertEquals(filtered.size(),0);


        order.status = "WC";
        orders.put(order.order_id,order);

        filtered = confirmShipments.removeWCGrayLabelOrders(orders);

        assertEquals(filtered.size(),0);


    }

    @Test
    public void testRemoveNotUnshippedOrders() {
        Order order = BaseTest.prepareOrder();

        //canceled
        order.order_id = "113-3520286-4229806";
        Map<String, Order> orders = new HashMap<String, Order>();
        orders.put(order.order_id,order);

        Map<String, Order> filtered = confirmShipments.removeNotUnshippedOrders(orders, Country.US);

        assertEquals(filtered.size(),0);

    }


    @Test
    public void testGenerateFeedFile() throws Exception {
    }

}