package edu.olivet.harvester.feeds.helper;


import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.service.mws.OrderClient;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.testng.Assert.assertEquals;

@Guice(modules = {MockDBModule.class})
public class ShipmentOrderFilterTest extends BaseTest {

    @Inject private ShipmentOrderFilter shipmentOrderFilter;

    @BeforeClass
    public void init() {

        OrderClient orderClient = new OrderClient() {
            @Override
            public List<Order> getOrders(Country country, List<String> amazonOrderIds) {
                List<com.amazonservices.mws.orders._2013_09_01.model.Order> result = new ArrayList<>();
                for (String amazonOrderId : amazonOrderIds) {
                    File localOrderXMLFile = new File(TEST_DATA_ROOT + File.separator + "order-" + amazonOrderId + ".xml");
                    String xmlFragment = Tools.readFileToString(localOrderXMLFile);
                    result.add(MWSUtils.buildMwsObject(xmlFragment, com.amazonservices.mws.orders._2013_09_01.model.Order.class));
                }
                return result;
            }

        };

        shipmentOrderFilter.setMwsOrderClient(orderClient);
    }

    @Test
    public void testRemoveDuplicatedOrders() {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.reloadSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, "08/31");

        List<edu.olivet.harvester.model.Order> orders = appScript.getOrdersFromWorksheet(worksheet);
        StringBuilder resultSummary = new StringBuilder();
        Map<String, edu.olivet.harvester.model.Order> filterd = shipmentOrderFilter.removeDuplicatedOrders(orders,resultSummary);

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

        System.out.println(resultSummary);

    }

    @Test
    public void testRemoveWC() {
        edu.olivet.harvester.model.Order order = BaseTest.prepareOrder();
        order.status = "wc";

        Map<String, edu.olivet.harvester.model.Order> orders = new HashMap<>();
        orders.put(order.order_id,order);

        StringBuilder resultSummary = new StringBuilder();
        Map<String, edu.olivet.harvester.model.Order> filtered = shipmentOrderFilter.removeWCGrayLabelOrders(orders, resultSummary);

        assertEquals(filtered.size(),0);


        order.status = "WC";
        orders.put(order.order_id,order);

        filtered = shipmentOrderFilter.removeWCGrayLabelOrders(orders, resultSummary);

        assertEquals(filtered.size(),0);

        System.out.println(resultSummary);

    }

    @Test
    public void testRemoveNotUnshippedOrders() {
        edu.olivet.harvester.model.Order order = BaseTest.prepareOrder();

        //canceled
        order.order_id = "113-3520286-4229806";
        Map<String, edu.olivet.harvester.model.Order> orders = new HashMap<>();
        orders.put(order.order_id,order);


        StringBuilder resultSummary = new StringBuilder();

        Map<String, edu.olivet.harvester.model.Order> filtered = shipmentOrderFilter.removeNotUnshippedOrders(orders, Country.US, resultSummary);

        assertEquals(filtered.size(),0);

        System.out.println(resultSummary);

    }
}