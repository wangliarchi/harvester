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
import edu.olivet.harvester.fulfill.service.flowcontrol.OrderFlowEngine;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ItemCheckResultDialog;
import edu.olivet.harvester.ui.panel.RuntimeSettingsPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Order station prototype entry
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Singleton
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);

    @Inject
    AppScript appScript;
    @Inject
    SheetService sheetService;
    @Inject
    OrderValidator orderValidator;

    @Inject
    MarkStatusService markStatusService;

    @Inject
    OrderFlowEngine orderFlowEngine;

    @Inject
    MessageListener messageListener;

    @Inject
    DailyBudgetHelper dailyBudgetHelper;

    @Inject
    OrderService orderService;

    @Inject
    OrderSubmissionTaskService orderSubmissionTaskService;

    private static final Map<String, Boolean> DUPLICATION_CHECK_CACHE = new HashMap<>();

    private static final List<Country> SUPPORTED_MARKETPLACES = Lists.newArrayList(Country.US, Country.CA, Country.UK, Country.DE, Country.FR, Country.ES, Country.IT, Country.AU);

    public void execute(RuntimeSettings runtimeSettings) {
        PSEventListener.start();
        OrderSubmissionTask task = orderSubmissionTaskService.createFromRuntimeSettings(runtimeSettings);
        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance().progressBar, SimpleOrderSubmissionRuntimePanel.getInstance().progressTextLabel);
        execute(task, true);
        //reset after done
        PSEventListener.end();
    }

    public void execute(OrderSubmissionTask task) {
        execute(task, false);
    }

    public void execute(OrderSubmissionTask task, boolean singleTask) {

        long start = System.currentTimeMillis();

        dailyBudgetHelper.setRuntimePanelObserver(RuntimeSettingsPanel.getInstance());

        List<Order> validOrders = prepareOrderSubmission(task);
        if (CollectionUtils.isEmpty(validOrders)) {
            orderSubmissionTaskService.completed(task);
            return;
        }

        task.setTotalOrders(validOrders.size());
        task.setOrders(JSON.toJSONString(validOrders));
        orderSubmissionTaskService.startTask(task);

        OrderDispatcher.getInstance().dispatch(validOrders, task, singleTask);


    }


    public List<Order> validateOrders(List<Order> orders) {
        List<Order> validOrders = new ArrayList<>();
        for (Order order : orders) {
            String error;
            if (!SUPPORTED_MARKETPLACES.contains(OrderCountryUtils.getFulfillmentCountry(order))) {
                error = String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.", SUPPORTED_MARKETPLACES);
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
                String msg = String.format("%s duplicated orders found in %s, %s", duplicatedOrders.size(), spreadsheet.getProperties().getTitle(),
                    StringUtils.join(duplicatedOrders.stream().map(it -> it.order_id + " @ " + it.sheetName).collect(Collectors.toSet()).toArray(new String[duplicatedOrders.size()]), ", "));
                throw new BusinessException(msg + "\n\n Please fix before submitting orders.");
            }
        }
    }

    private List<Order> prepareOrderSubmission(OrderSubmissionTask task) {
        RuntimeSettings settings = task.convertToRuntimeSettings();

        long start = System.currentTimeMillis();

        if (!SUPPORTED_MARKETPLACES.contains(Country.valueOf(settings.getMarketplaceName()))) {
            messageListener.addMsg(String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.", SUPPORTED_MARKETPLACES), InformationLevel.Negative);
            task.setStatus(OrderTaskStatus.Error.name());
            task.setDateStarted(new Date());
            orderSubmissionTaskService.saveTask(task);
            return null;
        }

        //check daily budget
        try {
            dailyBudgetHelper.checkBudget(settings.getSpreadsheetId());
        } catch (Exception e) {
            LOGGER.error("Error when fetch daily budget", e);
            throw e;
        }

        //checkDuplicates(settings.getSpreadsheetId());
        //mark status first
        markStatusService.execute(settings, false);
        List<Order> orders = task.getOrderList();
        if (CollectionUtils.isEmpty(orders)) {
            orders = appScript.readOrders(settings);
            orders = validateOrders(orders);
            orders = titleCheck(orders);
        } else if (System.currentTimeMillis() - task.getDateCreated().getTime() > 10 * 60 * 1000) {
            orders = sheetService.reloadOrders(orders);
        }

        String resultSummary = String.format("Finished loading orders to submit for %s, %d orders found, took %s", settings.toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        messageListener.addLongMsg(resultSummary, orders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        if (CollectionUtils.isEmpty(orders)) {
            return orders;
        }

        //remove if not valid
        List<Order> validOrders = validateOrders(orders);

        if (CollectionUtils.isEmpty(validOrders)) {
            return validOrders;
        }

        resultSummary = String.format("%d order(s) to be submitted.", validOrders.size());
        task.setTotalOrders(validOrders.size());
        LOGGER.info(resultSummary);
        messageListener.addMsg(resultSummary, validOrders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        return validOrders;
    }

    public List<Order> titleCheck(List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return orders;
        }

        if (OrderValidator.needCheck(null, OrderValidator.SkipValidation.ItemName)) {
            List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(orders);
            ItemCheckResultDialog dialog = UITools.setDialogAttr(new ItemCheckResultDialog(null, true, results));

            if (dialog.isValidReturn()) {
                List<ItemCompareResult> sync = dialog.getIsbn2Sync();
                sync.forEach(it -> {
                    if (!it.isManualCheckPass()) {
                        messageListener.addMsg(it.getOrder(), "Failed item name check. " + it.getPreCheckReport(), InformationLevel.Negative);
                        orders.remove(it.getOrder());
                    }
                });
            }
        }

        return orders;
    }

    public String parseErrorMsg(String fullMsg) {
        if (Strings.containsAnyIgnoreCase(fullMsg, JXBrowserHelper.CHANNEL_CLOSED_MESSAGE)) {
            return "JXBrowser Crashed";
        }

        Pattern pattern = Pattern.compile(Pattern.quote("xception:"));
        String[] parts = pattern.split(fullMsg);
        return parts[parts.length - 1].trim();
    }

}
