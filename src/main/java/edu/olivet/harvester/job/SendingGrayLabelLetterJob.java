package edu.olivet.harvester.job;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.common.model.CronjobLog;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.letters.CommonLetterSender;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class SendingGrayLabelLetterJob extends AbstractBackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendingGrayLabelLetterJob.class);


    @Override
    public void execute() {

        //check store name
        Settings.load().validateAndFixStoreName();
        if (!enabled()) {
            LOGGER.info("Auto sending gray label letters was not enabled. To enable this function, go to Settings->System Settings->Common Letters");
            return;
        }

        ApplicationContext.getBean(CommonLetterSender.class).execute();

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

    @Override
    public boolean enabled() {
        SystemSettings systemSettings = SystemSettings.load();
        return systemSettings.isEnableAutoSendGrayLabelLetters();
    }

}
