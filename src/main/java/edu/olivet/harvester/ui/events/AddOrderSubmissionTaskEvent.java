package edu.olivet.harvester.ui.events;

import com.alibaba.fastjson.JSON;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.*;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.OrderSubmissionBuyerTaskService;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.AddOrderSubmissionTaskDialog;
import edu.olivet.harvester.ui.dialog.ItemCheckResultDialog;
import edu.olivet.harvester.ui.panel.TasksAndProgressPanel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/12/17 12:14 PM
 */
public class AddOrderSubmissionTaskEvent extends Observable implements HarvesterUIEvent, OrderSubmissionTaskHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddOrderSubmissionTaskEvent.class);

    @Inject
    OrderSubmissionTaskService orderSubmissionTaskService;
    @Inject
    OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;

    @Override
    public void execute() {
        AddOrderSubmissionTaskDialog dialog = UITools.setDialogAttr(new AddOrderSubmissionTaskDialog(this));
        if (dialog.isOk()) {
            UITools.info(" Task created successfully");
        }
    }

    @Override
    public void saveTasks(List<OrderSubmissionTask> tasks) {
        tasks.forEach(it -> {
            it.setOrderRangeCol(it.getOrderRange().toString());
            it.setSkipValidationCol(it.getSkipValidation().toString());
            it.setStatus(OrderTaskStatus.Scheduled.name());
            orderSubmissionTaskService.saveTask(it);
        });

        UITools.info(tasks.size() + " task(s) been created. Please wait a moment for title check.");
        if (CollectionUtils.isNotEmpty(tasks)) {
            checkTitle(tasks);
        }

        TasksAndProgressPanel.getInstance().loadTasksToTable();


    }


    @Inject
    AppScript appScript;
    @Inject
    OrderValidator orderValidator;

    public void checkTitle(List<OrderSubmissionTask> tasks) {

        List<Order> orders = new ArrayList<>();
        Map<String, List<String>> invalidOrders = new HashMap<>();
        Map<String, List<Order>> skippedValidationOrderMap = new HashMap<>();

        tasks.forEach(task -> {
            RuntimeSettings settings = task.convertToRuntimeSettings();
            List<Order> sheetOrders = appScript.readOrders(settings);
            if (OrderValidator.needCheck(settings, null, OrderValidator.SkipValidation.ItemName)) {
                orders.addAll(sheetOrders);
            } else {
                skippedValidationOrderMap.put(task.getOrderRange().getSheetName(), sheetOrders);
            }
        });

        orders.removeIf(order -> StringUtils.isNotBlank(orderValidator.canSubmit(order)));

        if (CollectionUtils.isNotEmpty(orders)) {
            List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(orders);
            ItemCheckResultDialog dialog = UITools.setDialogAttr(new ItemCheckResultDialog(null, true, results));

            if (dialog.isValidReturn()) {
                List<ItemCompareResult> sync = dialog.getIsbn2Sync();
                sync.forEach(it -> {
                    if (!it.isManualCheckPass()) {
                        Order order = it.getOrder();
                        List<String> errors = invalidOrders.getOrDefault(order.sheetName, new ArrayList<>());
                        errors.add(order.sheetName + " " + order.row + " " + it.getPreCheckReport());
                        invalidOrders.put(order.sheetName, errors);
                        orders.remove(order);
                    }
                });
            }
        }

        Map<String, List<Order>> validOrderMap = orders.stream().collect(Collectors.groupingBy(Order::getSheetName));
        validOrderMap.putAll(skippedValidationOrderMap);


        for (OrderSubmissionTask task : tasks) {
            List<Order> sheetValidOrders = validOrderMap.getOrDefault(task.getOrderRange().getSheetName(), new ArrayList<>());
            List<String> sheetInvalidOrders = invalidOrders.getOrDefault(task.getOrderRange().getSheetName(), new ArrayList<>());
            task.setTotalOrders(sheetValidOrders.size());
            task.setOrders(JSON.toJSONString(sheetValidOrders));
            task.setInvalidOrders(StringUtils.join(sheetInvalidOrders, "\n"));
            orderSubmissionTaskService.saveTask(task);

            Map<Account, Map<Country, List<Order>>> map = new HashMap<>();

            for (Order order : sheetValidOrders) {
                Account buyerAccount = OrderBuyerUtils.getBuyer(order, task);
                Country fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(order);
                Map<Country, List<Order>> countryListMap = map.getOrDefault(buyerAccount, new HashMap<>());
                List<Order> orderList = countryListMap.getOrDefault(fulfillmentCountry, new ArrayList<>());
                orderList.add(order);
                countryListMap.put(fulfillmentCountry, orderList);
                map.put(buyerAccount, countryListMap);
            }

            map.forEach((buyer, countryListMap) -> {
                countryListMap.forEach((country, orderList) -> {
                    OrderSubmissionBuyerAccountTask orderSubmissionBuyerAccountTask = new OrderSubmissionBuyerAccountTask();
                    orderSubmissionBuyerAccountTask.setBuyerAccount(buyer.getEmail());
                    orderSubmissionBuyerAccountTask.setFulfillmentCountry(country.name());
                    orderSubmissionBuyerAccountTask.setTaskId(task.getId());
                    orderSubmissionBuyerAccountTask.setMarketplaceName(task.getMarketplaceName());
                    orderSubmissionBuyerAccountTask.setSpreadsheetId(task.getSpreadsheetId());
                    orderSubmissionBuyerAccountTask.setSpreadsheetName(task.getSpreadsheetName());
                    orderSubmissionBuyerAccountTask.setSheetName(task.getOrderRange().getSheetName());
                    orderSubmissionBuyerAccountTask.setOrders(JSON.toJSONString(orderList));
                    orderSubmissionBuyerAccountTask.setTotalOrders(orderList.size());
                    orderSubmissionBuyerTaskService.saveTask(orderSubmissionBuyerAccountTask);
                });
            });
        }

    }

}
