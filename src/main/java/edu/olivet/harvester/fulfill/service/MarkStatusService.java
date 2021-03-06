package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.fulfill.utils.validation.RuntimeSettingsValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.MessageListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 10:41 AM
 */
public class MarkStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkStatusService.class);

    @Inject
    RuntimeSettingsValidator validator;
    @Inject private
    AppScript appScript;
    @Inject private
    SheetService sheetService;
    @Inject private
    OrderValidator orderValidator;

    @Inject private
    MessageListener messageListener;

    public void execute() {
        RuntimeSettings settings = RuntimeSettings.load();
        execute(settings);
    }

    public void execute(RuntimeSettings settings) {
        execute(settings, true);
    }

    public void execute(RuntimeSettings settings, boolean showErrorMsg) {

        long start = System.currentTimeMillis();
        List<Order> orders = appScript.readOrders(settings);

        String resultSummary = String.format("Finished loading orders to update status for %s, %d orders found, took %s",
                settings.toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        //if (showErrorMsg) {
        //    messageListener.addMsg(resultSummary);
        //}

        if (CollectionUtils.isEmpty(orders)) {
            UITools.info(UIText.message("message.info.nostatus"), UIText.title("title.result"));
            return;
        }

        //remove if not valid
        List<Order> validOrders = new ArrayList<>();
        for (Order order : orders) {
            String error = orderValidator.canMarkStatus(order);
            if (StringUtils.isNotBlank(error)) {
                if (showErrorMsg) {
                    messageListener.addMsg(order, error, InformationLevel.Negative);
                }
            } else {
                validOrders.add(order);
            }
        }


        if (CollectionUtils.isEmpty(validOrders)) {
            LOGGER.info("No valid orders to update status.");
            if (showErrorMsg) {
                messageListener.addMsg("No valid orders to update status.");
            }
            return;
        }

        resultSummary = String.format("%d order(s) to be updated status.", validOrders.size());
        LOGGER.info(resultSummary);
        if (showErrorMsg) {
            messageListener.addMsg(resultSummary);
        }

        Map<String, List<String>> results = sheetService.updateStatus(settings.getSpreadsheetId(), validOrders);

        if (showErrorMsg) {
            if (results == null) {
                messageListener.addMsg("Failed to update order status.", InformationLevel.Negative);
            }

            assert results != null;
            if (results.containsKey("s")) {
                for (String r : results.get("s")) {
                    messageListener.addMsg(r);
                }
            }

            if (results.containsKey("f")) {
                for (String r : results.get("f")) {
                    messageListener.addMsg(r, InformationLevel.Negative);
                }
            }
        }

    }

}
