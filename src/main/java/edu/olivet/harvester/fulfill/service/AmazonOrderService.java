package edu.olivet.harvester.fulfill.service;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.OrderFetcher;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.export.service.ExportOrderService;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/28/2017 10:45 AM
 */
public class AmazonOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonOrderService.class);

    @Inject private OrderFetcher orderFetcher;
    @Inject private ExportOrderService exportOrderService;
    @Inject private DBManager dbManager;

    public edu.olivet.harvester.model.Order reloadOrder(edu.olivet.harvester.model.Order order) {
        //check local first
        AmazonOrder amazonOrder = loadFromLocal(order.order_id, order.sku);
        if (amazonOrder != null) {
            return amazonOrder.toOrder();
        }

        //load from amazon
        //amazonOrder = loadOrder(order);
        //if (amazonOrder != null) {
        //    return amazonOrder.toOrder();
        //}

        throw new BusinessException("Cant load order " + order.order_id + " " + order.sku);
    }

    public AmazonOrder loadOrder(edu.olivet.harvester.model.Order order) {
        Country country = OrderCountryUtils.getMarketplaceCountry(order);
        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();
        //load order info from amazon
        try {
            List<Order> orders = orderFetcher.read(Lists.newArrayList(order.order_id), credential);
            if (CollectionUtils.isNotEmpty(orders)) {
                List<AmazonOrder> amazonOrders = exportOrderService.convertToAmazonOrders(orders, country);
                for (AmazonOrder amazonOrder : amazonOrders) {
                    if (order.sku.equalsIgnoreCase(amazonOrder.getSku())) {
                        return amazonOrder;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("fail to load order {} from {} ", order.order_id, country, e);
        }

        throw new BusinessException("fail to load order " + order.order_id + " from  " + country);
    }

    private AmazonOrder loadFromLocal(String amazonOrderId, String sku) {
        List<AmazonOrder> orders = dbManager.query(AmazonOrder.class, Cnd.where("orderId", "=", amazonOrderId)
                .and("sku", "=", sku));
        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }

        return orders.get(0);
    }
}
