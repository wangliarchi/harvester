package edu.olivet.harvester.feeds.helper;


import com.google.inject.Inject;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

@Guice(modules = {MockDBModule.class, MockDateModule.class})
public class ShipmentOrderFilterTest extends BaseTest {

    @Inject
    private ShipmentOrderFilter shipmentOrderFilter;

    @Test
    public void testRemoveDuplicatedOrders() {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.reloadSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, "08/31");

        List<edu.olivet.harvester.common.model.Order> orders = appScript.getOrdersFromWorksheet(worksheet);
        StringBuilder resultSummary = new StringBuilder();
        StringBuilder resultDetail = new StringBuilder();
        List<edu.olivet.harvester.common.model.Order> filterd = shipmentOrderFilter.removeDuplicatedOrders(orders, resultSummary, resultDetail);

        assertEquals(filterd.size(), 10);

        assertEquals(filterd.stream().map(it -> it.order_id).collect(Collectors.toSet()), new HashSet<>(Arrays.asList(
                "112-9914696-8464248",
                "114-6213194-8837063",
                "114-6532260-0041852",
                "111-3140015-6109021",
                "112-1487598-0950651",
                "114-0961383-9264229",
                "112-9421439-0058664",
                "113-8382913-6392256",
                "112-1328432-8909803",
                "114-6250525-1507456")));

        System.out.println(resultSummary);
        System.out.println(resultDetail);

    }

    @Test
    public void testRemoveWC() {
        edu.olivet.harvester.common.model.Order order = BaseTest.prepareOrder();
        order.status = "wc";

        List<edu.olivet.harvester.common.model.Order> orders = new ArrayList<>(2);
        orders.add(order);

        StringBuilder resultSummary = new StringBuilder();
        StringBuilder resultDetail = new StringBuilder();
        List<edu.olivet.harvester.common.model.Order> filtered = shipmentOrderFilter.removeWCGrayLabelOrders(orders, resultSummary, resultDetail);

        assertEquals(filtered.size(), 0);


        resultSummary = new StringBuilder();
        resultDetail = new StringBuilder();
        order.status = "WC";
        orders.add(order);

        filtered = shipmentOrderFilter.removeWCGrayLabelOrders(orders, resultSummary, resultDetail);

        assertEquals(filtered.size(), 0);

        System.out.println(resultSummary);
        System.out.println(resultDetail);
    }

    @Test
    public void testRemoveNotUnshippedOrders() {
        List<edu.olivet.harvester.common.model.Order> orders = new ArrayList<>(3);

        edu.olivet.harvester.common.model.Order order = BaseTest.prepareOrder();
        order.order_id = "113-3520286-4229806";
        order.setAmazonOrderStatus("Canceled");
        orders.add(order);

        edu.olivet.harvester.common.model.Order orderShipped = BaseTest.prepareOrder();
        orderShipped.order_id = "113-3520286-4229802";
        orderShipped.setAmazonOrderStatus("Shipped");
        orders.add(orderShipped);


        edu.olivet.harvester.common.model.Order orderUnshipped = BaseTest.prepareOrder();
        orderUnshipped.order_id = "113-3520286-4229803";
        orderUnshipped.setAmazonOrderStatus("Unshipped");
        orders.add(orderUnshipped);

        StringBuilder resultSummary = new StringBuilder();
        StringBuilder resultDetail = new StringBuilder();
        List<edu.olivet.harvester.common.model.Order> filtered = shipmentOrderFilter.removeNotUnshippedOrders(orders, resultSummary, resultDetail);

        assertEquals(filtered.size(), 1);

        System.out.println(resultSummary);
        System.out.println(resultDetail);
    }
}