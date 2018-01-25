package edu.olivet.harvester.hunt;


import com.google.inject.Inject;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.HuntService;
import edu.olivet.harvester.hunt.service.SheetService;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.MessageListener;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/19/2018 2:50 PM
 */
public class Hunter {

    @Inject HuntService huntService;
    @Inject AppScript appScript;
    @Inject private MessageListener messageListener;
    @Inject SheetService sheetService;

    public void execute(RuntimeSettings runtimeSettings) {
        List<Order> orders = appScript.readOrders(runtimeSettings);

        if (CollectionUtils.isEmpty(orders)) {
            messageListener.addMsg("No orders found for  " + runtimeSettings.getAdvancedSubmitSetting().toString(), InformationLevel.Negative);
            return;
        }

        huntForOrders(orders);

    }

    public void huntForWorksheets(List<Worksheet> worksheets) {
        for (Worksheet worksheet : worksheets) {
            try {
                huntForWorksheet(worksheet);
            } catch (Exception e) {
                //
            }
        }
    }

    public void huntForWorksheet(Worksheet worksheet) {
        List<Order> orders = appScript.readOrders(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName());

        if (CollectionUtils.isEmpty(orders)) {
            messageListener.addMsg("No orders found for worksheet " + worksheet, InformationLevel.Negative);
            return;
        }

        huntForOrders(orders);

    }

    public void huntForOrders(List<Order> orders) {
        ProgressUpdater.setProgressBarComponent(
                SimpleOrderSubmissionRuntimePanel.getInstance().progressBar,
                SimpleOrderSubmissionRuntimePanel.getInstance().progressTextLabel);
        ProgressUpdater.updateTotal(orders.size());
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.start();

        for (Order order : orders) {
            if (!OrderCountryUtils.getShipToCountry(order).equalsIgnoreCase("US")) {
                messageListener.addMsg(order, "only support us domestic orders only.", InformationLevel.Negative);
                ProgressUpdater.failed();
                continue;
            }
            Seller seller;
            try {
                seller = huntService.huntForOrder(order);
            } catch (Exception e) {
                messageListener.addMsg(order, "Failed to find seller - " + e.getMessage(), InformationLevel.Negative);
                ProgressUpdater.failed();
                continue;
            }

            try {
                order.setSellerData(seller);
                sheetService.fillSellerInfo(order);
                ProgressUpdater.success();
                messageListener.addMsg(order, "Find seller  - " + seller.toSimpleString(), InformationLevel.Positive);

            } catch (Exception e) {
                messageListener.addMsg(order, "Failed to write seller info to sheet - " + e.getMessage(), InformationLevel.Negative);
                ProgressUpdater.failed();
            }
        }

        PSEventListener.end();
    }

}
