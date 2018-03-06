package edu.olivet.harvester.job;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.common.model.CronjobLog;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.feeds.StockUpdator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class SyncASINJob extends AbstractBackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncASINJob.class);


    @Override
    public void execute() {
        SystemSettings systemSettings = SystemSettings.load();
        if (!systemSettings.isEnableInvoiceDownloading()) {
            LOGGER.info("Auto ASIN sync was not enabled. To enable this function, go to Settings->System Settings->ASN Sync");
            return;
        }

        ApplicationContext.getBean(StockUpdator.class).execute();
        CronjobLog log = new CronjobLog();
        log.setId(this.getClass().getName() + Dates.nowAsFileName());
        log.setJobName(this.getClass().getName());
        log.setRunTime(new Date());

        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        dbManager.insert(log, CronjobLog.class);
    }

    @Override
    public void runIfMissed(Date nextTriggerTime) {
        //
    }


}
