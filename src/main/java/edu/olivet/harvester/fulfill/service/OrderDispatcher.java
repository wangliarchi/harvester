package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import org.elasticsearch.common.util.concurrent.CountDown;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/8/18 4:13 PM
 */
@Singleton
public class OrderDispatcher {
    //private static final Logger LOGGER = LoggerFactory.getLogger(OrderDispatcher.class);
    private static Map<String, BuyerPanelOrderWorker> jobs = new HashMap<>();
    private static Map<String, CountDownLatch> latches = new HashMap<>();

    @Inject private OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;
    @Inject private DailyBudgetHelper dailyBudgetHelper;


    private List<BuyerPanelOrderWorker> listJobs() {
        return new ArrayList<>(jobs.values());
    }

    public boolean hasJobRunning() {
        for (BuyerPanelOrderWorker job : listJobs()) {
            if (job.isRunning()) {
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
            BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
            String key = buyerPanel.getKey();
            dailyBudgetHelper.addRuntimePanelObserver(task.getSpreadsheetId(), buyerPanel);
            CountDownLatch latch = getLatch(key);
            BuyerPanelOrderWorker job = jobs.computeIfAbsent(key, k -> new BuyerPanelOrderWorker(buyerPanel));

            job.addTask(buyerTask, latch);

            if (job.getState() == SwingWorker.StateValue.PENDING) {
                job.execute();
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

    private BuyerPanelOrderWorker getJob(String key, BuyerPanel buyerPanel, CountDownLatch latch) {
        BuyerPanelOrderWorker job = jobs.computeIfAbsent(key, k -> new BuyerPanelOrderWorker(buyerPanel));
        if (job.isDone()) {
            job = new BuyerPanelOrderWorker(buyerPanel);
        }

        return job;
    }
}
