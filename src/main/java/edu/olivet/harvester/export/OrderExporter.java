package edu.olivet.harvester.export;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.export.service.ExportOrderService;
import edu.olivet.harvester.export.service.ExportStatService;
import edu.olivet.harvester.export.service.SheetService;
import edu.olivet.harvester.message.EmailService;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
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
    @Inject
    ExportOrderService exportOrderService;
    @Inject
    SheetAPI sheetAPI;
    @Inject
    SheetService sheetService;
    @Inject
    Now now;
    @Inject
    EmailService emailService;
    @Inject
    ErrorAlertService errorAlertService;

    private MessagePanel messagePanel = new VirtualMessagePanel();

    public void execute() {
        //this method is for cronjob, keep silent.
        setMessagePanel(new VirtualMessagePanel());

        //list all marketplaces
        Settings settings = Settings.load();
        List<Country> marketplaces = settings.listAllCountries();
        if (CollectionUtils.isEmpty(marketplaces)) {
            throw new BusinessException("No marketplace found. please check if you have correct settings.");
        }

        marketplaces.forEach(it -> exportOrdersForMarketplace(it));
    }

    public void exportOrdersForSelectedMarketplaces(List<String> marketplaces) {
        for (String marketplace : marketplaces) {
            long start = System.currentTimeMillis();

            messagePanel.wrapLineMsg(String.format("Starting exporting orders from %s at %s.", marketplace, start), LOGGER);


            Country country = Country.valueOf(marketplace);
            try {
                exportOrdersForMarketplace(country);
                messagePanel.displayMsg(String.format("Finish exporting orders from %s in %s.", marketplace, Strings.formatElapsedTime(start)), LOGGER);
            } catch (Exception e) {
                LOGGER.info("Error exporting orders from {}. ", marketplace, e);

                errorAlertService.sendMessage("Error exporting orders from " + marketplace,
                        e.getMessage(), country);
            }

        }
    }

    public void exportOrdersForMarketplace(Country country) {

        now.set(Dates.parseDate("11/30/2017"));
        List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();
        if (CollectionUtils.isEmpty(spreadsheetIds)) {
            messagePanel.displayMsg("No spreadsheet configuration found.", LOGGER, InformationLevel.Negative);
            return;
        }


        Date lastExportedDate;
        try {
            lastExportedDate = exportStatService.initExport(country);
        } catch (Exception e) {
            LOGGER.error("", e);
            messagePanel.displayMsg(e.getMessage(), LOGGER, InformationLevel.Negative);
            return;
        }

        // messagePanel.displayMsg(String.format("No new orders between %s and %s", dateMap.get(OrderFetcher.DateRangeType.LastUpdatedAfter), dateMap.get(OrderFetcher.DateRangeType.LastUpdatedBefore)), LOGGER, InformationLevel.Negative);
        List<Order> orders = exportOrderService.listUnshippedOrders(lastExportedDate, country);
        messagePanel.displayMsg(orders.size() + " order(s) found from " + country + " since " + lastExportedDate, LOGGER);

        if (CollectionUtils.isEmpty(orders)) {
            return;
        }

        try {
            sheetService.fillOrders(country, orders, messagePanel);
        } catch (Exception e) {
            errorAlertService.sendMessage("Error export orders", e.getMessage());
        }

    }

    public void setMessagePanel(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
    }


}
