package edu.olivet.harvester.service.mws;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Guice
public class OrderClientTest {

    @Inject private OrderClient orderClient;

    @Test
    public void testGetOrders() throws Exception {
        //using configuration settings for testing

        List<String> amazonOrderIds = new ArrayList<>();
        amazonOrderIds.add("114-1350328-1400255");
        amazonOrderIds.add("113-4995659-8661810");

        List<Order> orders = orderClient.getOrders(Country.US,amazonOrderIds);

        orders.forEach(it -> System.out.println(it));

    }

}