package edu.olivet.harvester.fulfill.service;

import com.alibaba.fastjson.JSON;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.model.setting.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.spreadsheet.model.OrderRange;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ItemCheckResultDialog;
import edu.olivet.harvester.ui.panel.TasksAndProgressPanel;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/16/2017 7:38 AM
 */
@Singleton
public class OrderSubmissionTaskService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmissionTaskService.class);
    @Inject private
    DBManager dbManager;

    public List<OrderSubmissionTask> todayTasks() {
        return dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .asc("dateCreated"));
    }

    public List<OrderSubmissionTask> todayScheduledTasks() {
        return dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .and("status", "=", OrderTaskStatus.Scheduled.name())
                        .and("totalOrders", ">", 0)
                        .asc("dateCreated"));
    }

    public List<OrderSubmissionTask> listAllTasks() {
        return dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", "NOT IS", null)
                        .desc("dateCreated"));
    }

    public void cleanUp() {
        List<OrderSubmissionTask> tasks = dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .and("status", "=", OrderTaskStatus.Processing.name())
                        .or("status", "=", OrderTaskStatus.Queued.name())
                        .asc("dateCreated"));
        tasks.forEach(this::stopTask);
    }


    public OrderSubmissionTask get(String id) {
        return dbManager.readById(id, OrderSubmissionTask.class);
    }

    public static OrderSubmissionTask convertFromRuntimeSettings(RuntimeSettings settings) {
        OrderSubmissionTask orderSubmissionTask = new OrderSubmissionTask();
        orderSubmissionTask.setSid(settings.getSid());
        orderSubmissionTask.setMarketplaceName(settings.getMarketplaceName());

        //noinspection ConstantConditions
        orderSubmissionTask.setLostLimit(settings.getLostLimit());
        //noinspection ConstantConditions
        orderSubmissionTask.setPriceLimit(settings.getPriceLimit());
        //noinspection ConstantConditions
        orderSubmissionTask.setEddLimit(settings.getEddLimit());
        orderSubmissionTask.setNoInvoiceText(settings.getNoInvoiceText());
        orderSubmissionTask.setFinderCode(settings.getFinderCode());
        orderSubmissionTask.setSkipValidation(settings.getSkipValidation());
        orderSubmissionTask.setSpreadsheetId(settings.getSpreadsheetId());
        orderSubmissionTask.setSpreadsheetName(settings.getSpreadsheetName());

        OrderRange orderRange = new OrderRange();
        orderRange.setSheetName(settings.getSheetName());
        AdvancedSubmitSetting advancedSubmitSetting = settings.getAdvancedSubmitSetting();
        switch (advancedSubmitSetting.getSubmitRange()) {
            case ALL:
                orderRange.setBeginRow(null);
                orderRange.setEndRow(null);
                break;
            case SINGLE:
                orderRange.setBeginRow(advancedSubmitSetting.getSingleRowNo());
                orderRange.setEndRow(null);
                break;
            default:
                orderRange.setBeginRow(advancedSubmitSetting.getStartRowNo());
                orderRange.setEndRow(advancedSubmitSetting.getEndRowNo());
        }

        orderSubmissionTask.setOrderRange(orderRange);
        orderSubmissionTask.setBuyerAccount(settings.getBuyerEmail());
        orderSubmissionTask.setPrimeBuyerAccount(settings.getPrimeBuyerEmail());
        return orderSubmissionTask;
    }

    public OrderSubmissionTask createFromRuntimeSettings(RuntimeSettings settings) {
        OrderSubmissionTask orderSubmissionTask = OrderSubmissionTaskService.convertFromRuntimeSettings(settings);
        orderSubmissionTask = saveTask(orderSubmissionTask);

        return orderSubmissionTask;
    }

    public void saveSuccess(OrderSubmissionTask task) {
        task = get(task.getId());
        task.setSuccess(task.getSuccess() + 1);
        saveTask(task);
    }

    public void saveSuccess(String id) {
        OrderSubmissionTask task = get(id);
        task.setSuccess(task.getSuccess() + 1);
        saveTask(task);
    }

    public void saveFailed(OrderSubmissionTask task) {
        task = get(task.getId());
        task.setFailed(task.getFailed() + 1);
        saveTask(task);
    }

    public void saveFailed(String id) {
        OrderSubmissionTask task = get(id);
        task.setFailed(task.getFailed() + 1);
        saveTask(task);
    }

    public OrderSubmissionTask saveTask(OrderSubmissionTask task, boolean reloadTable) {
        saveTask(task);
        if (reloadTable) {
            TasksAndProgressPanel.getInstance().loadTasksToTable();
        }
        return task;
    }

    public OrderSubmissionTask saveTask(OrderSubmissionTask task) {
        //if new, generate id
        if (StringUtils.isBlank(task.getId())) {
            task.setDateCreated(new Date());
            task.setId(DigestUtils.sha256Hex(task.toString()));
            task.setTaskStatus(OrderTaskStatus.Scheduled);
        }

        //check if completed
        if (task.getTotalOrders() > 0 && task.getSuccess() + task.getFailed() == task.getTotalOrders() &&
                task.taskStatus() != OrderTaskStatus.Completed &&
                task.taskStatus() != OrderTaskStatus.Retried) {
            task.setTaskStatus(OrderTaskStatus.Completed);
            task.setDateEnded(new Date());
        }

        task.setOrderRangeCol(task.getOrderRange().toString());
        task.setSkipValidationCol(task.getSkipValidation().toString());

        dbManager.insertOrUpdate(task, OrderSubmissionTask.class);

        return task;

    }

    @Inject private
    OrderSubmissionBuyerTaskService orderSubmissionBuyerTaskService;

    public void deleteTask(OrderSubmissionTask task) {
        task.setTaskStatus(OrderTaskStatus.Deleted);
        saveTask(task);
        //delete buyer tasks as well
        orderSubmissionBuyerTaskService.deleteByTaskId(task.getId());
    }

    public void hardDeleteTask(OrderSubmissionTask task) {
        dbManager.deleteById(task.getId(), OrderSubmissionTask.class);
        //delete buyer tasks as well
        orderSubmissionBuyerTaskService.deleteByTaskId(task.getId());
    }

    public void startTask(String id) {
        OrderSubmissionTask task = get(id);
        startTask(task);
    }

    public void startTask(OrderSubmissionTask task) {
        task.setTaskStatus(OrderTaskStatus.Processing);
        task.setDateStarted(new Date());
        saveTask(task);
    }

    public void stopTask(String taskId) {
        OrderSubmissionTask task = get(taskId);
        stopTask(task);
    }

    public void stopTask(OrderSubmissionTask task) {
        task.setTaskStatus(OrderTaskStatus.Stopped);
        task.setDateEnded(new Date());
        saveTask(task);

        orderSubmissionBuyerTaskService.stopByTaskId(task.getId());
    }

    public void completed(OrderSubmissionTask task) {
        task.setTaskStatus(OrderTaskStatus.Completed);
        task.setDateEnded(new Date());
        saveTask(task);
    }


    @Inject private
    AppScript appScript;
    @Inject private
    OrderValidator orderValidator;

    public void checkTitle(List<OrderSubmissionTask> tasks) {

        List<Order> orders = new ArrayList<>();
        Map<String, List<String>> invalidOrders = new HashMap<>();
        Map<OrderSubmissionTask, List<Order>> skippedValidationOrderMap = new HashMap<>();

        tasks.forEach(task -> {

            List<Order> sheetOrders = appScript.readOrders(task);
            sheetOrders.forEach(order -> order.setTask(task));

            if (OrderValidator.needCheck(task, OrderValidator.SkipValidation.ItemName)) {
                orders.addAll(sheetOrders);
            } else {
                skippedValidationOrderMap.put(task, sheetOrders);
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
                        List<String> errors = invalidOrders.getOrDefault(order.getTask().getId(), new ArrayList<>());
                        errors.add(order.sheetName + " " + order.row + " " + it.getPreCheckReport());
                        invalidOrders.put(order.getTask().getId(), errors);
                        orders.remove(order);
                    }
                });
            }
        }

        Map<OrderSubmissionTask, List<Order>> validOrderMap = orders.stream().collect(Collectors.groupingBy(Order::getTask));
        validOrderMap.putAll(skippedValidationOrderMap);


        for (OrderSubmissionTask task : tasks) {
            List<Order> sheetValidOrders = validOrderMap.getOrDefault(task, new ArrayList<>());
            List<String> sheetInvalidOrders = invalidOrders.getOrDefault(task.getId(), new ArrayList<>());


            task.setTotalOrders(sheetValidOrders.size());
            task.setOrders(JSON.toJSONString(sheetValidOrders.stream().distinct().collect(Collectors.toList())));
            task.setInvalidOrders(StringUtils.join(sheetInvalidOrders.stream().distinct().collect(Collectors.toList()), "\n"));
            saveTask(task);
        }

    }


}
