package edu.olivet.harvester.job;

import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.AutoUpgradeJob;
import lombok.Getter;

import java.util.Random;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 2:59 PM
 */
public enum BackgroundJob {
    /**
     * confirm shipment.
     * run all weekdays and saturnday. random time between 15:00-17:00pm
     */
    ShipmentConfirmation("0 30 16 ? * MON,TUE,WED,THU,FRI,SAT *", ShipmentConfirmationJob.class),

    /**
     * check unshipped orders, and send notification to account owner
     * run all weekdays and saturnday. random time between 17:00-18:00pm
     */
    //UnshippedOrderCheck("0 30 17 ? * MON,TUE,WED,THU,FRI,SAT *",UnshippedOrderCheckJob.class),

    ConfigUpload("0 0 5,13,21 1/1 * ? *", ConfigUploadJob.class),

    AutoUpgrade("0 15 2 1/1 * ? *", AutoUpgradeJob.class);

    private final String cron;
    @Getter private final Class<? extends AbstractBackgroundJob> clazz;

    BackgroundJob(String cron, Class<? extends AbstractBackgroundJob> clazz) {
        this.cron = cron;
        this.clazz = clazz;
    }

    public String getCron() {

        if (this.clazz == ShipmentConfirmationJob.class) {
            int hour = new Random().ints(1, 15, 17).findFirst().getAsInt();
            int min = new Random().ints(1, 0, 15).findFirst().getAsInt();
            return String.format("0 %d %d ? * MON,TUE,WED,THU,FRI,SAT *", min, hour);
        } else if (this.clazz == UnshippedOrderCheckJob.class) {
            int min = new Random().ints(1, 0, 15).findFirst().getAsInt();
            return String.format("0 %d 17 ? * MON,TUE,WED,THU,FRI,SAT *", min);
        }

        return this.cron;
    }
}
