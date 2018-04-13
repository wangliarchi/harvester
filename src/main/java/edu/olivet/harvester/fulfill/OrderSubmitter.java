package edu.olivet.harvester.fulfill;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.utils.MessageListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Order station prototype entry
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Singleton
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);

    @Inject private OrderValidator orderValidator;
    @Inject private MarkStatusService markStatusService;
    @Inject private MessageListener messageListener;
    @Inject private DailyBudgetHelper dailyBudgetHelper;
    @Inject private OrderSubmissionTaskService orderSubmissionTaskService;
    @Inject private OrderDispatcher orderDispatcher;

    private static final List<Country> SUPPORTED_MARKETPLACES =
            Lists.newArrayList(Country.US, Country.CA, Country.UK, Country.DE, Country.FR, Country.ES, Country.IT, Country.AU,
                    Country.JP, Country.MX);

    /**
     * add task to order submission work queue
     *
     * @param task Order Submission Task
     */
    public void execute(OrderSubmissionTask task) {
        LOGGER.info("adding order submission task " + task.toString());
        task.setTaskStatus(OrderTaskStatus.Queued);
        orderSubmissionTaskService.saveTask(task);

        List<Order> validOrders = prepareOrderSubmission(task);
        if (CollectionUtils.isEmpty(validOrders)) {
            task.setTaskStatus(OrderTaskStatus.Completed);
            orderSubmissionTaskService.saveTask(task, true);
            return;
        }

        ProgressUpdater.updateTotal(validOrders.size());
        task.setTotalOrders(validOrders.size());
        task.setOrders(JSON.toJSONString(validOrders));
        task.setTaskStatus(OrderTaskStatus.Queued);
        orderSubmissionTaskService.saveTask(task, true);

        orderDispatcher.dispatch(validOrders, task);
    }

    private List<Order> prepareOrderSubmission(OrderSubmissionTask task) {
        long start = System.currentTimeMillis();

        //check if the marketplace is supported
        if (!marketplaceSupported(task)) {
            return null;
        }

        //check daily budget
        dailyBudgetHelper.checkBudget(task.getSpreadsheetId());

        //mark status first
        markStatusService.execute(task.convertToRuntimeSettings(), false);

        //load orders
        List<Order> orders = orderSubmissionTaskService.loadOrdersForTask(task);

        LOGGER.info("Finished loading orders for {}, {} orders found, took {}",
                task.convertToRuntimeSettings(), orders.size(), Strings.formatElapsedTime(start));

        if (CollectionUtils.isEmpty(orders)) {
            return orders;
        }

        //set task info for later use
        orders.forEach(order -> order.setTask(task));

        //remove if not valid
        List<Order> validOrders = validateOrders(orders);

        if (CollectionUtils.isEmpty(validOrders)) {
            return validOrders;
        }

        LOGGER.info("{} order(s) to be submitted.", validOrders.size());

        return validOrders;
    }

    public List<Order> validateOrders(List<Order> orders) {
        List<Order> validOrders = new ArrayList<>();
        for (Order order : orders) {
            String error = orderValidator.canSubmitWithStatusCheck(order);

            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error, InformationLevel.Negative);
                LOGGER.error("{} - {}", order, error);
                continue;
            }

            validOrders.add(order);
        }

        return validOrders;
    }

    public boolean marketplaceSupported(OrderSubmissionTask task) {
        if (!SUPPORTED_MARKETPLACES.contains(Country.valueOf(task.getMarketplaceName()))) {
            messageListener.addMsg(String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.",
                    SUPPORTED_MARKETPLACES), InformationLevel.Negative);
            task.setTaskStatus(OrderTaskStatus.Error);
            task.setDateStarted(new Date());
            orderSubmissionTaskService.saveTask(task);
            return false;
        }

        return true;
    }
}
