package edu.olivet.harvester.job;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.model.CronjobLog;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class ShipmentConfirmationJob extends AbstractBackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentConfirmationJob.class);


    @Override
    public void execute() {
        ApplicationContext.getBean(ConfirmShipments.class).execute();
        CronjobLog log = new CronjobLog();
        log.setId(this.getClass().getName() + Dates.nowAsFileName());
        log.setJobName(this.getClass().getName());
        log.setRunTime(new Date());

        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        dbManager.insert(log, CronjobLog.class);
    }

    @Override
    public void runIfMissed() {
        DateTime dt = new DateTime();
        if (dt.getHourOfDay() < 17) {
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

    public static void main(String[] args) {
        ShipmentConfirmationJob bean = new ShipmentConfirmationJob();
        bean.runIfMissed();
    }


}
