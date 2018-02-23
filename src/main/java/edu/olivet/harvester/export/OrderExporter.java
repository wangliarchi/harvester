package edu.olivet.harvester.export;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.export.model.OrderExportParams;
import edu.olivet.harvester.export.service.ExportOrderService;
import edu.olivet.harvester.export.service.ExportStatService;
import edu.olivet.harvester.export.service.SheetService;
import edu.olivet.harvester.hunt.Hunter;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/15/17 12:15 PM
 */
public class OrderExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderExporter.class);
    @Inject
    private ExportStatService exportStatService;
    @Inject private
    ExportOrderService exportOrderService;
    @Inject private
    SheetService sheetService;
    @Inject private
    Now now;
    @Inject private
    ErrorAlertService errorAlertService;

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    @Inject Hunter hunter;

    /**
     * triggered by cronjob
     */
    public void execute() {
        //this method is for cronjob, keep silent.
        setMessagePanel(new ProgressDetail(Actions.ExportOrders));
        //list all marketplaces
        Settings settings = Settings.load();
        List<Country> marketplaces = settings.listAllCountries();
        if (CollectionUtils.isEmpty(marketplaces)) {
            messagePanel.displayMsg("No marketplace found. please check if you have correct settings.", LOGGER, InformationLevel.Negative);
            return;
        }

        marketplaces.forEach(it -> exportOrdersForMarketplace(it, null, null));

        settings.listAllSpreadsheets().forEach(it -> {
            try {
                hunter.execute(it);
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        });
    }

    /**
     * triggered by export orders button
     */
    public void exportOrders(OrderExportParams params) {
        for (Country marketplace : params.getMarketplaces()) {
            long start = System.currentTimeMillis();

            messagePanel.wrapLineMsg(String.format("Starting exporting orders from %s at %s.",
                    marketplace, Dates.toDateTime(start)), LOGGER);

            try {
                exportOrdersForMarketplace(marketplace, params.getFromDate(), params.getToDate());
                messagePanel.displayMsg(String.format("Finish exporting orders from %s in %s.",
                        marketplace, Strings.formatElapsedTime(start)), LOGGER);

                //hunt sellers
                messagePanel.addMsgSeparator();
                messagePanel.displayMsg("Starting to hunt sellers");
                List<String> spreadsheetIds = Settings.load().getConfigByCountry(marketplace).listSpreadsheetIds();
                for (String spreadsheetId : spreadsheetIds) {
                    try {
                        hunter.execute(spreadsheetId);
                    } catch (Exception e) {
                        messagePanel.displayMsg("Error while hunting sellers: " + Strings.getExceptionMsg(e));
                    }
                }

            } catch (Exception e) {
                messagePanel.displayMsg(String.format("Error exporting orders from %s - %s. ",
                        marketplace, e.getMessage()), InformationLevel.Negative);
                LOGGER.info("Error exporting orders from {}. ", marketplace, e);

                errorAlertService.sendMessage("Error exporting orders from " + marketplace,
                        e.getMessage(), marketplace);
            }

        }
    }


    public void exportOrdersForMarketplace(Country country, Date fromDate, Date toDate) {
        messagePanel.wrapLineMsg("Exporting orders from " + country + "...");
        List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();
        if (CollectionUtils.isEmpty(spreadsheetIds)) {
            messagePanel.displayMsg("No spreadsheet configuration found.", LOGGER, InformationLevel.Negative);
            return;
        }

        //check if exporting service is running, load last updated date.
        Date lastExportedDate = DateUtils.addHours(now.get(), 26);
        //try {
        //lastExportedDate = exportStatService.lastOrderDate(country);
        //lastExportedDate = DateUtils.addHours(lastExportedDate, -1);
        //} catch (Exception e) {
        //LOGGER.error("", e);
        //messagePanel.displayMsg(e.getMessage(), LOGGER, InformationLevel.Negative);
        //}

        //if not manually set from date, use the date from init service
        if (fromDate != null) {
            lastExportedDate = fromDate;
        }

        //if not manually set to date, set to 5 mins before now.
        if (toDate == null || toDate.after(now.get())) {
            toDate = DateUtils.addMinutes(now.get(), -5);
        }

        messagePanel.displayMsg("Exporting orders updated between " + lastExportedDate + " and " + toDate, LOGGER);

        //list all unexported orders
        List<Order> orders;
        try {
            exportOrderService.setMessagePanel(messagePanel);
            orders = exportOrderService.listUnexportedOrders(lastExportedDate, toDate, country);
        } catch (Exception e) {
            LOGGER.error("", e);
            messagePanel.displayMsg("Error fetch orders from amazon for " + country + ", " + e.getMessage(), InformationLevel.Negative);
            exportStatService.updateStat(country, lastExportedDate, 0);
            return;
        }

        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No new order(s) found from " + country, LOGGER, InformationLevel.Negative);
            exportStatService.updateStat(country, lastExportedDate, 0);
            return;
        }

        messagePanel.displayMsg("Totally " + orders.size() + " row records found from " + country, LOGGER);
        messagePanel.displayMsg("Writing orders to order update sheets");
        //fill orders to order update sheets
        try {
            sheetService.fillOrders(country, orders, messagePanel);
        } catch (Exception e) {
            messagePanel.displayMsg("Error export orders " + e.getMessage(), LOGGER, InformationLevel.Negative);
            errorAlertService.sendMessage("Error export orders", e.getMessage());
        }

        try {
            exportStatService.updateStat(country, toDate, orders.size());
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }


}
