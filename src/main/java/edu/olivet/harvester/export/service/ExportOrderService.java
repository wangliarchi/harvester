package edu.olivet.harvester.export.service;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.amazonservices.mws.orders._2013_09_01.model.OrderItem;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.OrderFetcher;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.export.utils.SelfOrderChecker;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.service.BlacklistBuyer;
import edu.olivet.harvester.model.Remark;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.service.mws.OrderClient;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/19/17 7:49 PM
 */
public class ExportOrderService extends OrderClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportOrderService.class);
    private final String[] STATUS_FILTERS = {"Unshipped", "PartiallyShipped", "Shipped"};
    private final int DAYS_BACK = -7;

    @Inject
    OrderFetcher orderFetcher;
    @Inject
    SheetAPI sheetAPI;
    @Inject
    DBManager dbManager;
    @Inject
    OrderService orderService;

    @Inject
    BlacklistBuyer blacklistBuyer;
    @Inject
    Now now;


    public List<edu.olivet.harvester.model.Order> listUnexportedOrders(Date lastExportedDate, Date toDate, Country country) {

        //list orders from amazon for the specified date range. order status include shipped, unshipped, and partiallyShipped
        List<Order> orders = listOrdersFromAmazon(lastExportedDate, toDate, country);

        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }

        //remove exported orders
        orders = removeExportedOrders(orders, lastExportedDate, country);
        if (CollectionUtils.isEmpty(orders)) {
            return null;
        }

        //save to db, read order item info, and finally convert to AmazonOrder object
        List<AmazonOrder> amazonOrders = saveAmazonOrders(orders, country);

        return toOrders(amazonOrders);


    }

    /**
     * <pre>
     *     convert AmazonOrder to Order object
     *     handle blacklist buyer and self order check
     * </pre>
     */
    public List<edu.olivet.harvester.model.Order> toOrders(List<AmazonOrder> amazonOrders) {
        List<edu.olivet.harvester.model.Order> orderList = new ArrayList<>();
        for (AmazonOrder amazonOrder : amazonOrders) {
            edu.olivet.harvester.model.Order order;
            try {
                order = amazonOrder.toOrder();
            } catch (Exception e) {
                LOGGER.error("error convert AmazonOrder {} to order object", amazonOrder, e);
                continue;
            }

            //mark blacklist buyer
            if (blacklistBuyer.isBlacklist(amazonOrder.getName(), amazonOrder.getEmail(), Address.loadFromOrder(order))) {
                blacklistBuyer.appendRemark(order);
            }

            //mark self order
            if (SelfOrderChecker.isSelfOrder(amazonOrder)) {
                SelfOrderChecker.markAsSelfOrder(order);
            }

            orderList.add(order);
        }

        return orderList;
    }


    /**
     * <pre>
     *     List orders from amazon
     *     Using last updated after & before
     *     Return unshipped, partial shipped and shipped orders
     * </pre>
     */
    public List<Order> listOrdersFromAmazon(Date lastExportedDate, Date toDate, Country country) {
        Map<OrderFetcher.DateRangeType, Date> dateMap = new HashMap<>();
        dateMap.put(OrderFetcher.DateRangeType.LastUpdatedAfter, lastExportedDate);
        dateMap.put(OrderFetcher.DateRangeType.LastUpdatedBefore, toDate);
        LOGGER.info("Fetching orders last updated between {} and {}", dateMap.get(OrderFetcher.DateRangeType.LastUpdatedAfter),
                dateMap.get(OrderFetcher.DateRangeType.LastUpdatedBefore));
        List<Order> orders;
        try {
            orders = orderFetcher.readOrders(dateMap, Settings.load().getConfigByCountry(country).getMwsCredential(), STATUS_FILTERS);
        } catch (Exception e) {
            LOGGER.error("Error fetching orders from amazon {} to {}", dateMap.get(OrderFetcher.DateRangeType.LastUpdatedAfter), dateMap.get(OrderFetcher.DateRangeType.LastUpdatedBefore), e);
            throw new BusinessException(String.format("Error fetching orders from amazon %s to %s", dateMap.get(OrderFetcher.DateRangeType.LastUpdatedAfter), dateMap.get(OrderFetcher.DateRangeType.LastUpdatedBefore)));
        }

        return orders;


    }

    /**
     * <pre>
     *     remove already exported orders.
     *     read last DAYS_BACK order from order update sheets
     * </pre>
     */
    public List<Order> removeExportedOrders(List<Order> orders, Date fromDate, Country country) {
        //load orders from last 7 days to check duplicates
        List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();
        Map<String, edu.olivet.harvester.model.Order> allOrders = new HashMap<>();


        Date minDate;
        if (fromDate.after(DateUtils.addDays(now.get(), DAYS_BACK))) {
            minDate = DateUtils.addDays(now.get(), DAYS_BACK);
        } else {
            minDate = DateUtils.addDays(fromDate, -1);
        }

        spreadsheetIds.forEach(it -> {
            orderService.fetchOrders(sheetAPI.getSpreadsheet(it), minDate).forEach(order -> {
                allOrders.put(order.order_id, order);
            });
        });

        orders.removeIf(order -> allOrders.containsKey(order.getAmazonOrderId()));
        return orders;
    }

    /**
     * <pre>
     *     List order items from amazon
     *     save to local db for later reference
     * </pre>
     */
    public List<AmazonOrder> saveAmazonOrders(List<Order> orders, Country country) {
        List<AmazonOrder> amazonOrders = new ArrayList<>();
        for (Order order : orders) {
            AmazonOrder amazonOrder = new AmazonOrder();
            amazonOrder.setOrderId(order.getAmazonOrderId());
            amazonOrder.setOrderStatus(order.getOrderStatus());
            amazonOrder.setPurchaseDate(order.getPurchaseDate().toGregorianCalendar().getTime());
            amazonOrder.setXml(order.toXML());

            List<OrderItem> items = orderFetcher.readItems(order.getAmazonOrderId(), Settings.load().getConfigByCountry(country).getMwsCredential());
            for (OrderItem item : items) {
                amazonOrder.setOrderItemId(item.getOrderItemId());
                amazonOrder.setAsin(item.getASIN());
                amazonOrder.setSku(item.getSellerSKU());
                try {
                    String isbn = TrueFakeAsinMappingService.getISBN(item.getSellerSKU(), item.getASIN());
                    amazonOrder.setIsbn(isbn);
                } catch (BusinessException e) {
                    LOGGER.error(e.getMessage());
                    amazonOrder.setIsbn(StringUtils.EMPTY);
                }
                amazonOrder.setItemXml(item.toXML());
                amazonOrder.setExportStatus(AmazonOrder.NOT_EXPORTED);
                amazonOrder.setLastUpdate(new Date());

                dbManager.insertOrUpdate(amazonOrder, AmazonOrder.class);

                amazonOrders.add(amazonOrder);
            }
        }

        return amazonOrders;
    }
}
