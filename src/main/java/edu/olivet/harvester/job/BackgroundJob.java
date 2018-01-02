package edu.olivet.harvester.job;

import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.AutoUpgradeJob;
import edu.olivet.harvester.model.SystemSettings;
import edu.olivet.harvester.utils.common.DatetimeHelper;
import lombok.Getter;

import java.time.LocalTime;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 2:59 PM
 */
public enum BackgroundJob {
    /**
     * confirm shipment.
     * run all weekdays and Saturnday. random time between 15:00-17:00pm
     */
    ShipmentConfirmation("0 30 16 ? * MON,TUE,WED,THU,FRI,SAT *", ShipmentConfirmationJob.class),

    //OrderExporting("0 30 7 ? * MON,TUE,WED,THU,FRI,SAT *", OrderExportingJob.class),
    /**
     * check unshipped orders, and send notification to account owner
     * run all weekdays and Saturnday. random time between 17:00-18:00pm
     */
    //UnshippedOrderCheck("0 30 17 ? * MON,TUE,WED,THU,FRI,SAT *",UnshippedOrderCheckJob.class),

    //ConfigUpload("0 0 5,13,21 1/1 * ? *", ConfigUploadJob.class),

    HarvesterAutoUpgrade("0 15 2 1/1 * ? *", AutoUpgradeJob.class),

    ContextUploadJob("0 0 5,13,21 1/1 * ? *", ContextUploadJob.class),

    ProductTitleCheck("0 0 3 1/1 * ? *", ProductTitleCheckJob.class),

    LogUpload("0 45 8,18,23 1/1 * ? *", LogUploader.class);


    private final String cron;
    @Getter
    private final Class<? extends AbstractBackgroundJob> clazz;

    BackgroundJob(String cron, Class<? extends AbstractBackgroundJob> clazz) {
        this.cron = cron;
        this.clazz = clazz;
    }

    public String getCron() {
        SystemSettings systemSettings = SystemSettings.load();
        if (this == ShipmentConfirmation) {
//            @SuppressWarnings("ConstantConditions") int hour = new Random().ints(1, 12, 14).findFirst().getAsInt();
//            @SuppressWarnings("ConstantConditions") int min = new Random().ints(1, 0, 55).findFirst().getAsInt();
//            return String.format("0 %d %d ? * MON,TUE,WED,THU,FRI,SAT *", min, hour);
            LocalTime orderConfirmationTime = systemSettings.getOrderConfirmationTime();
            int allowedRange = systemSettings.getOrderConfirmationAllowedRange();
            LocalTime scheduledTime = DatetimeHelper.randomTimeBetween(orderConfirmationTime, allowedRange);
            return String.format("%d %d %d ? * MON,TUE,WED,THU,FRI,SAT *", scheduledTime.getSecond(), scheduledTime.getMinute(), scheduledTime.getHour());
        }

//        if (this == OrderExporting) {
//            LocalTime orderExportTime = systemSettings.getOrderExportTime();
//            int allowedRange = systemSettings.getOrderExportAllowedRange();
//            LocalTime scheduledTime = DatetimeHelper.randomTimeBetween(orderExportTime, allowedRange);
//            return String.format("%d %d %d ? * MON,TUE,WED,THU,FRI,SAT *", scheduledTime.getSecond(), scheduledTime.getMinute(), scheduledTime.getHour());
//        }

        return this.cron;
    }
}
