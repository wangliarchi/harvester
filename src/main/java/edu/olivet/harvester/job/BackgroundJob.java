package edu.olivet.harvester.job;

import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.AutoUpgradeJob;
import lombok.Getter;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 2:59 PM
 */
public enum BackgroundJob {

    ShipmentConfirmation("0 30 16 ? * MON,TUE,WED,THU,FRI,SAT *", ShipmentConfirmationJob.class),

    ConfigUpload("0 0 5,13,21 1/1 * ? *", ConfigUploadJob.class),

    AutoUpgrade("0 15 2 1/1 * ? *", AutoUpgradeJob.class);

    @Getter private final String cron;
    @Getter private final Class<? extends AbstractBackgroundJob> clazz;

    BackgroundJob(String cron, Class<? extends AbstractBackgroundJob> clazz) {
        this.cron = cron;
        this.clazz = clazz;
    }
}
