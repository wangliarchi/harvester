package edu.olivet.harvester.job;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.common.model.CronjobLog;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class ProductTitleCheckJob extends AbstractBackgroundJob {
    @Override
    public void execute() {

        List<Spreadsheet> spreadsheets = new ArrayList<>();

        for (Country country : Settings.load().listAllCountries()) {
            spreadsheets.addAll(Settings.load().listSpreadsheets(country, new AppScript()));
        }

        if (CollectionUtils.isEmpty(spreadsheets)) {
            return;
        }

        //wait for random seconds
        int random = RandomUtils.nextInt(0, 60 * 60);
        Tools.sleep(random * 1000);

        OrderService orderService = ApplicationContext.getBean(OrderService.class);
        SheetAPI sheetAPI = ApplicationContext.getBean(SheetAPI.class);

        for (Spreadsheet spreadsheet : spreadsheets) {
            com.google.api.services.sheets.v4.model.Spreadsheet spreadsheet1 = sheetAPI.getSpreadsheet(spreadsheet.getSpreadsheetId());
            List<Order> orders = orderService.fetchOrders(spreadsheet1, DateUtils.addDays(new Date(), -7));
            if (CollectionUtils.isNotEmpty(orders)) {
                PreValidator.compareItemNames4Orders(orders);
            }
        }

        CronjobLog log = new CronjobLog();
        log.setId(this.getClass().getName() + Dates.nowAsFileName());
        log.setJobName(this.getClass().getName());
        log.setRunTime(new Date());

        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        dbManager.insert(log, CronjobLog.class);
    }

}
