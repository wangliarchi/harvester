package edu.olivet.harvester.service.mws;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.OrderFetcher;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.Range;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public List<Order> listOrders(Country country, @Nullable Map<OrderFetcher.DateRangeType,Date> dateMap, String... statuses) {
        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();

        return orderFetcher.readOrders(dateMap, credential,statuses);
    }



}
