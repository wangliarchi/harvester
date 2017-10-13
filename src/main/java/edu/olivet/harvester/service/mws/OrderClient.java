package edu.olivet.harvester.service.mws;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.OrderFetcher;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class OrderClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderClient.class);


    @Inject
    private OrderFetcher orderFetcher;


    /**
     * Returns orders based on the AmazonOrderId values that you specify.
     * https://docs.developer.amazonservices.com/en_UK/orders-2013-09-01/Orders_GetOrder.html
     * MWS GetOrder operation returns an order for each AmazonOrderId that you specify, up to a maximum of 50.
     * The GetOrder operation includes order information for each order returned,
     * including PurchaseDate, OrderStatus, FulfillmentChannel, and LastUpdateDate.
     */
    public List<Order> getOrders(Country country, List<String> amazonOrderIds) {

        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();

        return orderFetcher.read(amazonOrderIds, credential);

    }

    public List<Order> listOrders(Country country, @Nullable Map<OrderFetcher.DateRangeType, Date> dateMap, String... statuses) {
        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();

        return orderFetcher.readOrders(dateMap, credential, statuses);
    }


    public void getAmazonOrderStatuses(List<edu.olivet.harvester.model.Order> orders, Country country) {

        List<String> amazonOrderIds = orders.stream().map(it -> it.order_id).collect(Collectors.toList());

        List<com.amazonservices.mws.orders._2013_09_01.model.Order> amazonOrders;
        try {
            amazonOrders = getOrders(country, amazonOrderIds);
        } catch (Exception e) {
            LOGGER.error("Error load order info via MWS for country {}, {}", country.name(), e);
            return;
        }


        Map<String, com.amazonservices.mws.orders._2013_09_01.model.Order> orderMap = new HashMap<>();
        for (com.amazonservices.mws.orders._2013_09_01.model.Order order : amazonOrders) {
            orderMap.put(order.getAmazonOrderId(), order);
        }

        List<edu.olivet.harvester.model.Order> shippedCanceledOrders = new ArrayList<>();
        orders.forEach(order -> {
            if (orderMap.containsKey(order.order_id)) {
                com.amazonservices.mws.orders._2013_09_01.model.Order amzOrder = orderMap.get(order.order_id);
                order.setAmazonOrderStatus(amzOrder.getOrderStatus());
                order.setSales_chanel(amzOrder.getSalesChannel());
            }
        });
    }

    public List<Order> listUnshippedOrders(Country country) {
        Map<OrderFetcher.DateRangeType, Date> dateMap = new HashMap<>();

        Date createdBefore = DateUtils.addDays(new Date(), -1);
        Date createdAfter = DateUtils.addDays(new Date(), -10);

        return listUnshippedOrders(country,createdBefore,createdAfter);

    }

    public List<Order> listUnshippedOrders(Country country, Date createdBefore, Date createdAfter) {
        Map<OrderFetcher.DateRangeType, Date> dateMap = new HashMap<>();
        dateMap.put(OrderFetcher.DateRangeType.CreatedBefore, createdBefore);
        dateMap.put(OrderFetcher.DateRangeType.CreatedAfter, createdAfter);

        List<Order> orders =  listOrders(country,dateMap, "Unshipped", "PartiallyShipped");
        orders.removeIf(order -> "Shipped".equals(order.getOrderStatus()) || "Canceled".equals(order.getOrderStatus()));

        return  orders;

    }


}
