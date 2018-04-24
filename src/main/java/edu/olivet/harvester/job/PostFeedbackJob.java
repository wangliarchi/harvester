package edu.olivet.harvester.job;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.common.model.CronjobLog;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.selforder.StatsManager;
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
public class PostFeedbackJob extends AbstractBackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostFeedbackJob.class);


    @Override
    public void execute() {
        if (!enabled()) {
            LOGGER.info("Auto feedback posting was not enabled. To enable this function, go to Settings->System Settings->Self Order");
            return;
        }
        ApplicationContext.getBean(StatsManager.class).postFeedbackJob();
        CronjobLog log = new CronjobLog();
        log.setId(this.getClass().getName() + Dates.nowAsFileName());
        log.setJobName(this.getClass().getName());
        log.setRunTime(new Date());

        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        dbManager.insert(log, CronjobLog.class);
    }

    @Override
    public boolean enabled() {
        SystemSettings systemSettings = SystemSettings.load();
        return systemSettings.isPostFeedback();
    }


}
