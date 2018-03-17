package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.common.model.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/8/18 4:13 PM
 */
@Singleton
public class OrderDispatcher {
    //private static final Logger LOGGER = LoggerFactory.getLogger(OrderDispatcher.class);
    private static Map<String, BuyerPanelOrderWorker> jobs = new HashMap<>();
    private static Map<String, CountDownLatch> latches = new HashMap<>();

    @Inject private OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;

    private ExecutorService threadPool;

    @Inject
    public void init() {
        threadPool = Executors.newFixedThreadPool(SystemSettings.reload().getMaxOrderProcessingThread());
    }

    private List<BuyerPanelOrderWorker> listJobs() {
        return new ArrayList<>(jobs.values());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public synchronized boolean hasJobRunning() {
        for (BuyerPanelOrderWorker job : listJobs()) {
            if (!job.isDone()) {
                return true;
            }
        }
        return false;
    }


    public void dispatch(List<Order> orders, OrderSubmissionTask task) {
        //按照账号/国家 分配订单
        Map<Account, Map<Country, List<Order>>> map = groupOrdersByBuyer(orders, task);

        map.forEach((buyer, countryListMap) -> countryListMap.forEach((country, orderList) -> {
            OrderSubmissionBuyerAccountTask buyerTask = orderSubmissionBuyerTaskService.create(country, buyer, task, orderList);
            String key = buyer.getEmail() + Constants.HYPHEN + country.name();
            CountDownLatch latch = getLatch(key);

            //if job not existed, or is done, create new worker, otherwise add task to existed worker
            if (!jobs.containsKey(key) || jobs.get(key).isDone()) {
                BuyerPanelOrderWorker job = new BuyerPanelOrderWorker(country, buyer);
                job.addTask(buyerTask, latch);
                threadPool.submit(job);
                jobs.put(key, job);
            } else {
                BuyerPanelOrderWorker job = jobs.get(key);
                job.addTask(buyerTask, latch);
            }

        }));

        if (!PSEventListener.isRunning()) {
            PSEventListener.start();
        }
    }


    private Map<Account, Map<Country, List<Order>>> groupOrdersByBuyer(List<Order> orders, OrderSubmissionTask task) {
        Map<Account, Map<Country, List<Order>>> map = new HashMap<>();

        for (Order order : orders) {
            Account buyerAccount = OrderBuyerUtils.getBuyer(order);
            _addOrderToGroup(order, buyerAccount, map);
        }
        return map;
    }

    private static void _addOrderToGroup(Order order, Account buyerAccount, Map<Account, Map<Country, List<Order>>> map) {
        Country fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(order);
        Map<Country, List<Order>> countryListMap = map.getOrDefault(buyerAccount, new HashMap<>());
        List<Order> orderList = countryListMap.getOrDefault(fulfillmentCountry, new ArrayList<>());
        orderList.add(order);
        countryListMap.put(fulfillmentCountry, orderList);
        map.put(buyerAccount, countryListMap);
    }

    private CountDownLatch getLatch(String key) {
        CountDownLatch latch = latches.computeIfAbsent(key, k -> new CountDownLatch(1));

        if (latch.getCount() == 0) {
            latch = new CountDownLatch(1);
        }

        return latch;
    }
}
