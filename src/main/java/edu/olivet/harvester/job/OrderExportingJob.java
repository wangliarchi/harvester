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
        SystemSettings systemSettings = SystemSettings.load();
        if (!systemSettings.isEnableOrderExport()) {
            LOGGER.info("Auto order exporting was not enabled. To enable this function, go to Settings->System Settings->Order Export");
            return;
        }

        Date now = new Date();
        //if current hour is less than next trigger time, cron job has not run yet, since its daily job
        if (Dates.getField(now, Calendar.HOUR_OF_DAY) < Dates.getField(nextTriggerTime, Calendar.HOUR_OF_DAY)) {
            return;
        }

        if (nextTriggerTime.getTime() - now.getTime() < systemSettings.getOrderExportAllowedRange() * 60 * 1000) {
            return;
        }



        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        List<CronjobLog> list = dbManager.query(CronjobLog.class,
                Cnd.where("jobName", "=", this.getClass().getName())
                        .and("runTime", ">", Dates.beginOfDay(new DateTime()).toDate())
                        .desc("runTime"));

        if (CollectionUtils.isEmpty(list)) {
            LOGGER.info("{} executed at program startup.", this.getClass().getName());
            execute();
        }


    }


}
