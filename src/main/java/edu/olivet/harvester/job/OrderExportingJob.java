package edu.olivet.harvester.job;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.export.OrderExporter;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.model.CronjobLog;
import edu.olivet.harvester.model.SystemSettings;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class OrderExportingJob extends AbstractBackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderExportingJob.class);


    @Override
    public void execute() {
        SystemSettings systemSettings = SystemSettings.load();
        if (!systemSettings.isEnableOrderExport()) {
            LOGGER.info("Auto order exporting was not enabled. To enable this function, go to Settings->System Settings->Order Export");
            return;
        }

        ApplicationContext.getBean(OrderExporter.class).execute();
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
