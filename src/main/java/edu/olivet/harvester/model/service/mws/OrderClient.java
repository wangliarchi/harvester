package edu.olivet.harvester.model.service.mws;

import com.amazonservices.mws.client.MwsException;
import com.amazonservices.mws.orders._2013_09_01.MarketplaceWebServiceOrdersClient;
import com.amazonservices.mws.orders._2013_09_01.MarketplaceWebServiceOrdersConfig;
import com.amazonservices.mws.orders._2013_09_01.model.GetOrderRequest;
import com.amazonservices.mws.orders._2013_09_01.model.GetOrderResponse;
import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.amazonservices.mws.orders._2013_09_01.model.ResponseHeaderMetadata;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.OrderFetcher;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.utils.DatetimeHelper;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class OrderClient  {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderClient.class);


    @Inject
    private OrderFetcher orderFetcher;



    /**
     * Returns orders based on the AmazonOrderId values that you specify.
     * https://docs.developer.amazonservices.com/en_UK/orders-2013-09-01/Orders_GetOrder.html
     * MWS GetOrder operation returns an order for each AmazonOrderId that you specify, up to a maximum of 50.
     * The GetOrder operation includes order information for each order returned,
     * including PurchaseDate, OrderStatus, FulfillmentChannel, and LastUpdateDate.
     * @param country
     * @param amazonOrderIds
     */
    public  List<Order> getOrders(Country country, List<String> amazonOrderIds) {

        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();

        List<Order> result = orderFetcher.read(amazonOrderIds,credential);


        return result;

    }

}
