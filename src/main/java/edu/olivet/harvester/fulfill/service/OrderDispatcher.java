package edu.olivet.harvester.fulfill.service;

import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/8/18 4:13 PM
 */
@Singleton
public class OrderDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDispatcher.class);
    private static Map<String, BuyerPanelOrderWorker> jobs = new HashMap<>();
    private static OrderDispatcher instance = null;
    private OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;

    private OrderDispatcher() {
        orderSubmissionBuyerTaskService = ApplicationContext.getBean(OrderSubmissionBuyerTaskService.class);
    }

    public static OrderDispatcher getInstance() {
        if (instance == null) {
            instance = new OrderDispatcher();
        }

        return instance;
    }

    public List<BuyerPanelOrderWorker> listJobs() {
        return jobs.values().stream().collect(Collectors.toList());
    }

    public boolean hasJobRunning() {
        for (BuyerPanelOrderWorker job : listJobs()) {
            if (job.isRunning()) {
                return true;
            }
        }

        return false;
    }

    public void dispatch(List<Order> orders, OrderSubmissionTask task, boolean singleTask) {
        ProgressUpdater.updateTotal(orders.size());

        Map<Account, Map<Country, List<Order>>> map = OrderDispatcher.groupOrdersByBuyer(orders, task);

        map.forEach((buyer, countryListMap) -> {
            countryListMap.forEach((country, orderList) -> {
                OrderSubmissionBuyerAccountTask buyerTask = orderSubmissionBuyerTaskService.create(country, buyer, task, orderList);

                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
                String key = buyerPanel.getKey();

                BuyerPanelOrderWorker job = jobs.computeIfAbsent(key, k -> {
                    BuyerPanelOrderWorker worker = new BuyerPanelOrderWorker(buyerPanel);
                    return worker;
                });

                if (job.isDone()) {
                    job = new BuyerPanelOrderWorker(buyerPanel);
                    jobs.put(key, job);
                }

                job.addTask(buyerTask);
                if (job.getState() == SwingWorker.StateValue.PENDING) {
                    job.execute();
                }
            });
        });
    }


    public static Map<Account, Map<Country, List<Order>>> groupOrdersByBuyer(List<Order> orders, RuntimeSettings runtimeSettings) {
        Map<Account, Map<Country, List<Order>>> map = new HashMap<>();

        for (Order order : orders) {
            Account buyerAccount = OrderBuyerUtils.getBuyer(order, runtimeSettings);
            _addOrderToGroup(order, buyerAccount, map);
        }
        return map;
    }

    public static Map<Account, Map<Country, List<Order>>> groupOrdersByBuyer(List<Order> orders, OrderSubmissionTask task) {
        Map<Account, Map<Country, List<Order>>> map = new HashMap<>();

        for (Order order : orders) {
            Account buyerAccount = OrderBuyerUtils.getBuyer(order, task);
            order.setTask(task);
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
}
