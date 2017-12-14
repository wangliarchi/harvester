package edu.olivet.harvester.fulfill;


import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.exception.Exceptions.OutOfBudgetException;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.fulfill.service.flowcontrol.OrderFlowEngine;
import edu.olivet.harvester.fulfill.utils.OrderBuyerUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.logger.StatisticLogger;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ItemCheckResultDialog;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
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

    private static final Map<String, Boolean> DUPLICATION_CHECK_CACHE = new HashMap<>();

    private static final List<Country> SUPPORTED_MARKETPLACES = Lists.newArrayList(Country.US, Country.CA, Country.UK, Country.DE, Country.FR, Country.ES, Country.IT);

    public void execute(RuntimeSettings settings) {
        if (PSEventListener.isRunning()) {
            throw new BusinessException("Other taks is running!");
        }
        messageListener.empty();

        if (!SUPPORTED_MARKETPLACES.contains(Country.valueOf(settings.getMarketplaceName()))) {
            messageListener.addMsg(String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.", SUPPORTED_MARKETPLACES), InformationLevel.Negative);
        }

        //check daily budget
        try {
            String spreadsheetId = settings.getSpreadsheetId();
            dailyBudgetHelper.getRemainingBudget(spreadsheetId, new Date());
        } catch (Exception e) {
            LOGGER.error("Error when fetch daily budget", e);
            UITools.error(e.getMessage());
            return;
        }

        //check duplication
        checkDuplicates(settings.getSpreadsheetId());


        //mark status first
        long start = System.currentTimeMillis();
        markStatusService.execute(settings, false);
        List<Order> orders = appScript.readOrders(settings);
        String resultSummary = String.format("Finished loading orders to submit for %s, %d orders found, took %s", settings.toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        messageListener.addLongMsg(resultSummary, orders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        if (CollectionUtils.isEmpty(orders)) {
            UITools.info(UIText.message("message.info.nostatus"), UIText.title("title.result"));
            return;
        }

        //remove if not valid
        List<Order> validOrders = validateOrders(orders);

        if (CollectionUtils.isEmpty(validOrders)) {
            _noOrders();
            return;
        }


        if (OrderValidator.needCheck(null, OrderValidator.SkipValidation.ItemName)) {
            List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(validOrders);
            ItemCheckResultDialog dialog = UITools.setDialogAttr(new ItemCheckResultDialog(null, true, results));

            if (dialog.isValidReturn()) {
                List<ItemCompareResult> sync = dialog.getIsbn2Sync();
                sync.forEach(it -> {
                    if (!it.isManualCheckPass()) {
                        messageListener.addMsg(it.getOrder(), "Failed item name check. " + it.getPreCheckReport(), InformationLevel.Negative);
                        validOrders.remove(it.getOrder());
                    }
                });
            }

            if (CollectionUtils.isEmpty(validOrders)) {
                _noOrders();
                return;
            }
        }


        //inform event listener.
        PSEventListener.start();

        resultSummary = String.format("%d order(s) to be submitted.", validOrders.size());
        LOGGER.info(resultSummary);
        messageListener.addMsg(resultSummary, validOrders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        ProgressUpdater.init(validOrders);
        for (Order order : validOrders) {
            //if stop btn clicked, break the process
            if (PSEventListener.stopped()) {
                break;
            }

            try {
                Account buyer = OrderBuyerUtils.getBuyer(order);
                Country country = OrderCountryUtils.getFulfillmentCountry(order);
                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
                TabbedBuyerPanel.getInstance().highlight(buyerPanel);
                //order = sheetService.reloadOrder(order);
                submit(order, buyerPanel);
            } catch (Exception e) {
                LOGGER.error("Error submit order {}", order.order_id, e);
                messageListener.addMsg(order, e.getMessage(), InformationLevel.Negative);

                if (e instanceof OutOfBudgetException) {
                    UITools.error("No more money to spend :(");
                    break;
                }
            }
        }


        StatisticLogger.log(String.format("%s\t%s", ProgressUpdater.toTable(), Strings.formatElapsedTime(start)));

        //reset after done
        PSEventListener.end();

    }

    @Inject
    DBManager dbManager;

    public void execute(OrderSubmissionTask task) {
        if (PSEventListener.isRunning()) {
            throw new BusinessException("Other taks is running!");
        }
        long start = System.currentTimeMillis();

        List<Order> validOrders = prepareOrderSubmission(task);
        if (CollectionUtils.isEmpty(validOrders)) {
            return;
        }


        task.setStatus(OrderTaskStatus.Processing.name());
        task.setDateStarted(new Date());
        task.save(dbManager);

        //inform event listener.
        PSEventListener.start();
        ProgressUpdater.init(validOrders);

        for (Order order : validOrders) {
            //if stop btn clicked, break the process
            if (PSEventListener.stopped()) {
                task.setStatus(OrderTaskStatus.Stopped.name());
                task.setDateEnded(new Date());
                task.save(dbManager);
                throw new BusinessException("Task stopped as requested");
            }

            try {
                Account buyer = OrderBuyerUtils.getBuyer(order);
                Country country = OrderCountryUtils.getFulfillmentCountry(order);
                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
                TabbedBuyerPanel.getInstance().highlight(buyerPanel);
                //order = sheetService.reloadOrder(order);
                submit(order, buyerPanel);

                if (StringUtils.isNotBlank(order.order_number)) {
                    task.setSuccess(task.getSuccess() + 1);
                } else {
                    task.setFailed(task.getFailed() + 1);
                }

            } catch (Exception e) {
                LOGGER.error("Error submit order {}", order.order_id, e);
                messageListener.addMsg(order, e.getMessage(), InformationLevel.Negative);

                task.setFailed(task.getFailed() + 1);
                if (e instanceof OutOfBudgetException) {
                    throw new BusinessException("No more money to spend :(");
                }
            } finally {
                task.setDateEnded(new Date());
                task.save(dbManager);
            }
        }

        task.setStatus(OrderTaskStatus.Completed.name());
        task.setDateEnded(new Date());
        task.save(dbManager);


        StatisticLogger.log(String.format("%s\t%s", ProgressUpdater.toTable(), Strings.formatElapsedTime(start)));

        //reset after done
        PSEventListener.end();

    }


    private List<Order> prepareOrderSubmission(OrderSubmissionTask task) {
        RuntimeSettings settings = task.convertToRuntimeSettings();

        long start = System.currentTimeMillis();


        if (!SUPPORTED_MARKETPLACES.contains(Country.valueOf(settings.getMarketplaceName()))) {
            messageListener.addMsg(String.format("Harvester can only support %s marketplaces at this moment. Sorry for inconvenience.", SUPPORTED_MARKETPLACES), InformationLevel.Negative);
            task.setStatus(OrderTaskStatus.Error.name());
            task.setDateStarted(new Date());
            task.save(dbManager);
            return null;
        }

        //check daily budget
        try {
            String spreadsheetId = settings.getSpreadsheetId();
            dailyBudgetHelper.getRemainingBudget(spreadsheetId, new Date());
        } catch (Exception e) {
            LOGGER.error("Error when fetch daily budget", e);
            throw e;
        }

        //checkDuplicates(settings.getSpreadsheetId());
        //mark status first
        markStatusService.execute(settings, false);
        List<Order> orders = task.getOrderList();
        if (System.currentTimeMillis() - task.getDateCreated().getTime() > 60 * 1000) {
            orders = sheetService.reloadOrders(orders);
        }

        String resultSummary = String.format("Finished loading orders to submit for %s, %d orders found, took %s", settings.toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        messageListener.addLongMsg(resultSummary, orders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        if (CollectionUtils.isEmpty(orders)) {
            UITools.info(UIText.message("message.info.nostatus"), UIText.title("title.result"));
            task.setStatus(OrderTaskStatus.Completed.name());
            task.save(dbManager);
            return null;
        }

        //remove if not valid
        List<Order> validOrders = validateOrders(orders);

        if (CollectionUtils.isEmpty(validOrders)) {
            task.setStatus(OrderTaskStatus.Completed.name());
            task.save(dbManager);
            return null;
        }


        resultSummary = String.format("%d order(s) to be submitted.", validOrders.size());
        task.setTotalOrders(validOrders.size());
        LOGGER.info(resultSummary);
        messageListener.addMsg(resultSummary, validOrders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        return validOrders;
    }

    public void submit(Order order, BuyerPanel buyerPanel) {
        String spreadsheetId = order.spreadsheetId;
        long start = System.currentTimeMillis();
        try {
            //validate again!
            String error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);
            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error);
                return;
            }
            order.originalRemark = order.remark;
            orderFlowEngine.process(order, buyerPanel);

            if (StringUtils.isNotBlank(order.order_number)) {
                messageListener.addMsg(order, "order fulfilled successfully. " + order.basicSuccessRecord() + ", took " + Strings.formatElapsedTime(start));
            }
        } catch (OutOfBudgetException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error submit order {}", order.order_id, e);
            Pattern pattern = Pattern.compile(Pattern.quote("xception:"));
            String[] parts = pattern.split(e.getMessage());
            String msg = parts[parts.length - 1].trim();
            messageListener.addMsg(order, msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
            sheetService.fillUnsuccessfulMsg(spreadsheetId, order, msg);
        } finally {
            if (StringUtils.isNotBlank(order.order_number)) {
                ProgressUpdater.success();
            } else {
                ProgressUpdater.failed();
            }
        }


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
}
