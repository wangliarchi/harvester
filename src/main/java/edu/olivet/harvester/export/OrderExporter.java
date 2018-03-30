package edu.olivet.harvester.export;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.DateFormat;
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
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
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

    @Inject MessageListener messageListener;

    @Inject Hunter hunter;

    /**
     * triggered by cronjob
     */
    public void execute() {
        setMessagePanel(new ProgressDetail(Actions.ExportOrders));
        //list all marketplaces
        Settings settings = Settings.load();
        List<Country> marketplaces = settings.listAllCountries();
        if (CollectionUtils.isEmpty(marketplaces)) {
            messagePanel.displayMsg("No marketplace found. please check if you have correct settings.", LOGGER, InformationLevel.Negative);
            return;
        }
        exportOrdersForMarketplaces(marketplaces, null, null);
    }

    /**
     * triggered by export orders button
     */
    public void exportOrders(OrderExportParams params) {
        //setMessagePanel(new ProgressDetail(Actions.ExportOrders));
        messagePanel.displayMsg(String.format("Exporting orders from %s between %s and %s",
                params.getMarketplaces(), params.getFromDate(), params.getToDate()), LOGGER, InformationLevel.Information);
        exportOrdersForMarketplaces(params.getMarketplaces(), params.getFromDate(), params.getToDate());
    }


    public void exportOrdersForMarketplaces(List<Country> marketplaces, Date fromDate, Date toDate) {
        List<Country> marketplacesExportedOrders = new ArrayList<>();
        for (Country marketplace : marketplaces) {
            try {
                boolean result = exportOrdersForMarketplace(marketplace, fromDate, toDate);
                if (result) {
                    marketplacesExportedOrders.add(marketplace);
                }
            } catch (Exception e) {
                messagePanel.displayMsg(String.format("Error exporting orders from %s - %s. ",
                        marketplace, Strings.getExceptionMsg(e)), InformationLevel.Negative);
                errorAlertService.sendMessage("Error exporting orders from " + marketplace, e.getMessage(), marketplace);
                LOGGER.info("Error exporting orders from {}. ", marketplace, e);
            }
        }


        //hunt sellers
        if (CollectionUtils.isNotEmpty(marketplacesExportedOrders)) {
            //messagePanel.addMsgSeparator();
            //.displayMsg("Starting to hunt sellers");
            hunter.setMessagePanel(messageListener);
            for (Country marketplace : marketplacesExportedOrders) {
                List<String> spreadsheetIds = Settings.load().getConfigByCountry(marketplace).listSpreadsheetIds();
                for (String spreadsheetId : spreadsheetIds) {
                    try {
                        hunter.execute(spreadsheetId);
                    } catch (Exception e) {
                        //messagePanel.displayMsg("Error while hunting sellers: " + Strings.getExceptionMsg(e));
                    }
                }
            }
        }
    }

    public boolean exportOrdersForMarketplace(Country country, Date fromDate, Date toDate) {
        long start = System.currentTimeMillis();
        messagePanel.addMsgSeparator();
        messagePanel.displayMsg(String.format("Exporting orders from %s at %s.",
                country, Dates.toDateTime(start)), LOGGER);
        List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();
        if (CollectionUtils.isEmpty(spreadsheetIds)) {
            messagePanel.displayMsg("No spreadsheet configuration found.", LOGGER, InformationLevel.Negative);
            return false;
        }

        //check if exporting service is running, load last updated date.
        if (fromDate == null) {
            int hoursBack = (Dates.getDayOfWeek(now.get()) == Calendar.MONDAY) ? -50 : -26;
            fromDate = DateUtils.addHours(now.get(), hoursBack);
            //Date lastExportedDate;
            //try {
            //    lastExportedDate = exportStatService.lastOrderDate(country);
            //    lastExportedDate = DateUtils.addHours(lastExportedDate, -1);
            //    if (lastExportedDate.after(DateUtils.addHours(now.get(), -26))) {
            //        fromDate = DateUtils.addHours(now.get(), -26);
            //    } else {
            //        fromDate = lastExportedDate;
            //    }
            //} catch (Exception e) {
            //    LOGGER.error("", e);
            //    messagePanel.displayMsg(Strings.getExceptionMsg(e), LOGGER, InformationLevel.Negative);
            //    return false;
            //}
        }


        //if not manually set to date, set to 5 mins before now.
        if (toDate == null || toDate.after(now.get())) {
            toDate = DateUtils.addMinutes(now.get(), -5);
        }

        if (fromDate.after(toDate)) {
            fromDate = DateUtils.addHours(toDate, -26);
        }

        messagePanel.displayMsg("Fetching orders updated between " + DateFormat.DATE_TIME.format(fromDate) + " and " + DateFormat.DATE_TIME.format(toDate), LOGGER);

        //list all unexported orders
        List<Order> orders;
        try {
            exportOrderService.setMessagePanel(messagePanel);
            orders = exportOrderService.listUnexportedOrders(fromDate, toDate, country);
        } catch (Exception e) {
            LOGGER.error("", e);
            messagePanel.displayMsg("Error fetch orders from amazon for " + country + ", " + Strings.getExceptionMsg(e), InformationLevel.Negative);
            exportStatService.updateStat(country, fromDate, 0);
            return false;
        }

        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No new order(s) found from " + country, LOGGER, InformationLevel.Negative);
            exportStatService.updateStat(country, fromDate, 0);
            return false;
        }

        messagePanel.displayMsg("Totally " + orders.size() + " row records found from " + country, LOGGER);
        messagePanel.displayMsg("Writing orders to order update sheets");
        //fill orders to order update sheets
        try {
            sheetService.fillOrders(country, orders, messagePanel);
        } catch (Exception e) {
            messagePanel.displayMsg("Error export orders " + Strings.getExceptionMsg(e), LOGGER, InformationLevel.Negative);
            errorAlertService.sendMessage("Error export orders", e.getMessage());
        }

        try {
            exportStatService.updateStat(country, toDate, orders.size());
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        messagePanel.displayMsg(String.format("Finish exporting orders from %s in %s.",
                country, Strings.formatElapsedTime(start)), LOGGER);
        return true;
    }
}
