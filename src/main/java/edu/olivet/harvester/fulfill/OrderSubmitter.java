package edu.olivet.harvester.fulfill;


import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.fulfill.utils.*;
import edu.olivet.harvester.logger.StatisticLogger;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.ui.TabbedBuyerPanel;
import edu.olivet.harvester.ui.dialog.ItemCheckResultDialog;
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

    public void execute(RuntimeSettings settings) {
        messageListener.empty();

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
        if (!DUPLICATION_CHECK_CACHE.containsKey(settings.getSpreadsheetId())) {
            Spreadsheet spreadsheet = sheetService.getSpreadsheet(settings.getSpreadsheetId());
            List<Order> duplicatedOrders = orderService.findDuplicates(spreadsheet);
            if (CollectionUtils.isNotEmpty(duplicatedOrders)) {
                String msg = String.format("%s duplicated orders found in %s, %s", duplicatedOrders.size(), spreadsheet.getProperties().getTitle(),
                        StringUtils.join(duplicatedOrders.stream().map(it -> it.order_id + " @ " + it.sheetName).collect(Collectors.toSet()).toArray(new String[duplicatedOrders.size()]), ", "));
                UITools.error(msg + "\n\n Please fix before submitting orders.");
                return;
            }
        }


        //mark status first
        long start = System.currentTimeMillis();
        markStatusService.excute(settings, false);
        List<Order> orders = appScript.readOrders(settings);
        String resultSummary = String.format("Finished loading orders to submit for %s, %d orders found, took %s", settings.toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        messageListener.addLongMsg(resultSummary, orders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        if (CollectionUtils.isEmpty(orders)) {
            UITools.info(UIText.message("message.info.nostatus"), UIText.title("title.result"));
            return;
        }

        //remove if not valid
        List<Order> validOrders = new ArrayList<>();
        for (Order order : orders) {
            String error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);

            if (StringUtils.isBlank(error) && (OrderCountryUtils.getFulfillementCountry(order) != Country.US)) {
                error = "Harvest can only support US marketplace at this moment. Sorry for inconvenience.";
            }


            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error, InformationLevel.Negative);
            } else {
                validOrders.add(order);
            }
        }

        if (CollectionUtils.isEmpty(validOrders)) {
            _noOrders();
            return;
        }


        if (OrderValidator.needCheck(OrderValidator.SkipValidation.ItemName)) {
            List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(validOrders);
            ItemCheckResultDialog dialog = UITools.setDialogAttr(new ItemCheckResultDialog(null, true, results));

            if (dialog.isValidReturn()) {
                List<ItemCompareResult> sync = dialog.getIsbn2Sync();
                sync.forEach(it -> {
                    if (it.isManualCheckPass() == false) {
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

//        if (!UITools.confirmed(UIText.message("message.info.statusupdated", validOrders.size(), Strings.formatElapsedTime(start)))) {
//            return;
//        }

        //inform event listener.
        PSEventListener.start();

        resultSummary = String.format("%d order(s) to be submitted.", validOrders.size());
        LOGGER.info(resultSummary);
        messageListener.addMsg(resultSummary, validOrders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        ProgressUpdator.init(validOrders);
        for (Order order : validOrders) {
            //if stop btn clicked, break the process
            if (PSEventListener.stopped()) {
                break;
            }

            try {
                Account buyer = OrderBuyerUtils.getBuyer(order);
                Country country = OrderCountryUtils.getFulfillementCountry(order);
                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
                TabbedBuyerPanel.getInstance().highlight(buyerPanel);

                submit(order, buyerPanel);


            } catch (Exception e) {
                LOGGER.error("Error submit order {}", order.order_id, e);
                messageListener.addMsg(order, e.getMessage(), InformationLevel.Negative);
            }
        }

        StatisticLogger.log(String.format("%s\t%s", ProgressUpdator.toTable(), Strings.formatElapsedTime(start)));

        //reset after done
        PSEventListener.end();

    }

    public void submit(Order order, BuyerPanel buyerPanel) {
        String spreadsheetId = RuntimeSettings.load().getSpreadsheetId();
        long start = System.currentTimeMillis();
        try {

            //validate again!
            String error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);
            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error);
                return;
            }
            //dailyBudgetHelper.getRemainingBudget(spreadsheetId, new Date());

            messageListener.addMsg(order, String.format("start submitting. Buyer account %s, marketplace %s", buyerPanel.getBuyer().getEmail(), buyerPanel.getCountry().baseUrl()));

            orderFlowEngine.process(order, buyerPanel);

            if (StringUtils.isNotBlank(order.order_number)) {
                messageListener.addMsg(order, "order fulfilled successfully. took " + Strings.formatElapsedTime(start));
            }


        } catch (Exception e) {
            LOGGER.error("Error submit order {}", order.order_id, e);

            Pattern pattern = Pattern.compile(Pattern.quote("xception:"));
            String[] parts = pattern.split(e.getMessage());
            String msg = parts[parts.length - 1].trim();

            messageListener.addMsg(order, msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
            sheetService.fillUnsuccessfulMsg(spreadsheetId, order, msg);
        }

        if (StringUtils.isNotBlank(order.order_number)) {
            ProgressUpdator.success();
        } else {
            ProgressUpdator.failed();
        }


    }

    public void _noOrders() {
        LOGGER.info("No valid orders to submit.");
        UITools.error("No valid orders to be submitted. See failed record log for more detail.");
        messageListener.addMsg("No valid orders to be submitted.");
    }


}
