package edu.olivet.harvester.fulfill;


import com.alibaba.fastjson.JSON;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ItemCheckResultDialog;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.MessageListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Order station prototype entry
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Singleton
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);

    @Inject private
    AppScript appScript;
    @Inject private
    SheetService sheetService;
    @Inject private
    OrderValidator orderValidator;

    @Inject private
    MarkStatusService markStatusService;


    @Inject private
    MessageListener messageListener;

    @Inject private
    DailyBudgetHelper dailyBudgetHelper;

    @Inject private
    OrderService orderService;

    @Inject private
    OrderSubmissionTaskService orderSubmissionTaskService;

    @Inject private OrderDispatcher orderDispatcher;
    private static final Map<String, Boolean> DUPLICATION_CHECK_CACHE = new HashMap<>();

    private static final List<Country> SUPPORTED_MARKETPLACES =
            Lists.newArrayList(Country.US, Country.CA, Country.UK, Country.DE, Country.FR, Country.ES, Country.IT, Country.AU);

    public void execute(RuntimeSettings runtimeSettings) {

        OrderSubmissionTask task = orderSubmissionTaskService.createFromRuntimeSettings(runtimeSettings);

        execute(task, true);

        //todo
        while (true) {
            if (orderDispatcher.hasJobRunning()) {
                PSEventListener.start();
                break;
            }
        }
        while (true) {
            if (task.getTaskStatus() == OrderTaskStatus.Completed) {
                PSEventListener.end();
                break;
            }
        }
    }


    public void execute(OrderSubmissionTask task) {
        execute(task, false);
    }

    public void execute(OrderSubmissionTask task, boolean singleTask) {
        List<Order> validOrders = prepareOrderSubmission(task);
        if (CollectionUtils.isEmpty(validOrders)) {
            task.setTaskStatus(OrderTaskStatus.Completed);
            orderSubmissionTaskService.saveTask(task, true);
            if (singleTask) {
                PSEventListener.end();
            }
            return;
        }

        task.setTotalOrders(validOrders.size());
        task.setOrders(JSON.toJSONString(validOrders));
        task.setTaskStatus(OrderTaskStatus.Queued);
        orderSubmissionTaskService.saveTask(task, true);

        if (singleTask) {
            ProgressUpdater.setProgressBarComponent(
                    SimpleOrderSubmissionRuntimePanel.getInstance().progressBar,
                    SimpleOrderSubmissionRuntimePanel.getInstance().progressTextLabel);
            ProgressUpdater.updateTotal(task.getTotalOrders());
            dailyBudgetHelper.addRuntimePanelObserver(task.getSpreadsheetId(), SimpleOrderSubmissionRuntimePanel.getInstance());
        }

        orderDispatcher.dispatch(validOrders, task);
    }


    public List<Order> validateOrders(List<Order> orders) {
        List<Order> validOrders = new ArrayList<>();
        for (Order order : orders) {
            String error;
            if (!SUPPORTED_MARKETPLACES.contains(OrderCountryUtils.getFulfillmentCountry(order))) {
                error = String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.",
                        SUPPORTED_MARKETPLACES);
            } else {
                error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);
            }

            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error, InformationLevel.Negative);
            } else {
                validOrders.add(order);
            }
        }

        return validOrders;
    }

    public void _noOrders() {
        LOGGER.info("No valid orders to submit.");
        UITools.error("No valid orders to be submitted. See failed record log for more detail.");
        messageListener.addMsg("No valid orders to be submitted.");
    }

    public void checkDuplicates(String spreadsheetId) {
        //check duplication
        if (!DUPLICATION_CHECK_CACHE.containsKey(spreadsheetId)) {
            Spreadsheet spreadsheet = sheetService.getSpreadsheet(spreadsheetId);
            List<Order> duplicatedOrders = orderService.findDuplicates(spreadsheet);
            DUPLICATION_CHECK_CACHE.put(spreadsheetId, true);
            if (CollectionUtils.isNotEmpty(duplicatedOrders)) {
                String duplicates = StringUtils.join(duplicatedOrders.stream().map(it -> it.order_id + " @ " + it.sheetName)
                        .collect(Collectors.toSet()).toArray(new String[duplicatedOrders.size()]), ", ");
                String msg = String.format("%s duplicated orders found in %s, %s",
                        duplicatedOrders.size(), spreadsheet.getProperties().getTitle(), duplicates);
                throw new BusinessException(msg + "\n\n Please fix before submitting orders.");
            }
        }
    }

    private List<Order> prepareOrderSubmission(OrderSubmissionTask task) {


        long start = System.currentTimeMillis();

        if (!SUPPORTED_MARKETPLACES.contains(Country.valueOf(task.getMarketplaceName()))) {
            messageListener.addMsg(String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.",
                    SUPPORTED_MARKETPLACES), InformationLevel.Negative);
            task.setTaskStatus(OrderTaskStatus.Error);
            task.setDateStarted(new Date());
            orderSubmissionTaskService.saveTask(task);
            return null;
        }

        //check daily budget
        try {
            dailyBudgetHelper.checkBudget(task.getSpreadsheetId());
        } catch (Exception e) {
            LOGGER.error("Error when fetch daily budget", e);
            throw e;
        }

        //checkDuplicates(settings.getSpreadsheetId());
        //mark status first
        markStatusService.execute(task.convertToRuntimeSettings(), false);
        List<Order> orders = task.getOrderList();
        if (CollectionUtils.isEmpty(orders)) {
            orders = appScript.readOrders(task);
            orders = titleCheck(orders);
        } else if (System.currentTimeMillis() - task.getDateCreated().getTime() > 10 * 60 * 1000) {
            orders = sheetService.reloadOrders(orders);
        }

        String resultSummary = String.format("Finished loading orders to submit for %s, %d orders found, took %s",
                task.convertToRuntimeSettings().toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        messageListener.addLongMsg(resultSummary, orders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        if (CollectionUtils.isEmpty(orders)) {
            return orders;
        }

        orders.forEach(order -> order.setTask(task));
        //remove if not valid
        List<Order> validOrders = validateOrders(orders);

        if (CollectionUtils.isEmpty(validOrders)) {
            return validOrders;
        }

        resultSummary = String.format("%d order(s) to be submitted.", validOrders.size());
        LOGGER.info(resultSummary);
        messageListener.addMsg(resultSummary, validOrders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        return validOrders;
    }

    public List<Order> titleCheck(List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return orders;
        }

        if (OrderValidator.needCheck(orders.get(0), OrderValidator.SkipValidation.ItemName)) {
            List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(orders);
            ItemCheckResultDialog dialog = UITools.setDialogAttr(new ItemCheckResultDialog(null, true, results));

            if (dialog.isValidReturn()) {
                List<ItemCompareResult> sync = dialog.getIsbn2Sync();
                sync.forEach(it -> {
                    if (!it.isManualCheckPass()) {
                        messageListener.addMsg(it.getOrder(), "Failed item name check. " + it.getPreCheckReport(),
                                InformationLevel.Negative);
                        orders.remove(it.getOrder());
                    }
                });
            }
        }

        return orders;
    }


}
