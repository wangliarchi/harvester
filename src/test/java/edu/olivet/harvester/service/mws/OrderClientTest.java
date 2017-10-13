package edu.olivet.harvester.service.mws;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import org.apache.commons.lang3.time.DateUtils;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Guice
public class OrderClientTest {

    @Inject
    private OrderClient orderClient;

    @Test
    public void testGetOrders() throws Exception {
        //using configuration settings for testing

        List<String> amazonOrderIds = new ArrayList<>();
        amazonOrderIds.add("114-1350328-1400255");
        amazonOrderIds.add("113-4995659-8661810");

        List<Order> orders = orderClient.getOrders(Country.US, amazonOrderIds);

        orders.forEach(System.out::println);

    }

    @Test
    public void testGetAmazonOrderStatuses() {
        List<edu.olivet.harvester.model.Order> orders = new ArrayList<>(2);

        edu.olivet.harvester.model.Order order = BaseTest.prepareOrder();
        order.order_id = "112-1598301-1085841";
        orders.add(order);

        edu.olivet.harvester.model.Order order1 = BaseTest.prepareOrder();
        order1.order_id = "112-9900100-3640236";
        orders.add(order1);

        orderClient.getAmazonOrderStatuses(orders, Country.US);

        orders.forEach(it -> System.out.println(it.order_id + "\t" + it.getAmazonOrderStatus()));

    }

    @Test
    public void testListUnshippedOrders() {
        Date createdBefore = DateUtils.addDays(new Date(), 0);
        Date createdAfter = DateUtils.addDays(new Date(), -10);

        List<Order> orders = orderClient.listUnshippedOrders(Country.US, createdBefore,createdAfter);

        orders.forEach(it -> System.out.println(it.getAmazonOrderId() + "\t" + it.getOrderStatus() + "\t" + it.getPurchaseDate()));
    }

}