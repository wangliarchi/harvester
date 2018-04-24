package edu.olivet.harvester.common.model;

import com.alibaba.fastjson.JSON;
import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.ui.Harvester;
import lombok.Data;

import java.io.File;
import java.time.LocalTime;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 3:35 PM
 */
@Data
@Singleton
public class SystemSettings {
    private boolean enableOrderExport = false;
    private LocalTime orderExportTime = LocalTime.of(7, 0);
    private Integer orderExportAllowedRange = 15;

    private boolean enableOrderConfirmation = true;
    private LocalTime orderConfirmationTime = LocalTime.of(13, 0);
    private Integer orderConfirmationAllowedRange = 60;

    private boolean enableInvoiceDownloading = false;
    private LocalTime invoiceDownloadTime = LocalTime.of(3, 0);
    private Integer invoiceDownloadingAllowedRange = 15;


    private boolean enableASINsSyncing = false;
    private LocalTime asinSyncTime = LocalTime.of(22, 0);
    private Integer asinSyncAllowedRange = 60;

    private int maxOrderProcessingThread = 5;
    private boolean orderSubmissionDebugModel = false;

    private String selfOrderSpreadsheetId = "";
    private String selfOrderStatsSpreadsheetId = "";
    private float selfOrderCostLimit = 1f;
    private String selfOrderFreeShippingTemplateName = "FreeShipping";
    private boolean postFeedback = true;

    private boolean enableAutoSendGrayLabelLetters = false;
    private LocalTime grayLabelLetterSendingTime = LocalTime.of(20, 0);
    private Integer grayLabelLetterSendingAllowedRange = 15;
    private Integer grayLabelLetterMaxDays = 7;
    private String grayLabelLetterSendingMethod = "Both ASC and Email";
    private boolean grayLabelLetterDebugModel = false;

    public static final String SYSTEM_SETTINGS_FILE_PATH = Directory.Customize.path() + "/system-settings.json";
    public static final String TEST_SYSTEM_SETTINGS_FILE_PATH = "src/test/resources/conf/system-settings.json";


    public synchronized void save() {
        File file = new File(getConfigPath());
        Tools.writeStringToFile(file, JSON.toJSONString(this, true));
        //clear cache
        instance = null;
    }

    private static SystemSettings instance;

    private SystemSettings() {

    }

    public boolean sendGrayLabelLettersViaASC() {
        return Strings.containsAnyIgnoreCase(grayLabelLetterSendingMethod, "both", "amazon") && !isOrderSubmissionDebugModel();
    }

    public boolean sendGrayLabelLettersViaEmail() {
        return Strings.containsAnyIgnoreCase(grayLabelLetterSendingMethod, "both", "mail");
    }

    public static synchronized SystemSettings reload() {
        File file = new File(getConfigPath());
        if (file.exists() && file.isFile()) {
            instance = JSON.parseObject(Tools.readFileToString(file), SystemSettings.class);
        } else {
            instance = new SystemSettings();
        }
        return instance;
    }

    public static SystemSettings load() {
        if (instance == null) {
            return reload();
        }

        return instance;
    }

    private static String getConfigPath() {
        if (Harvester.debugFlag) {
            return TEST_SYSTEM_SETTINGS_FILE_PATH;
        }

        return SYSTEM_SETTINGS_FILE_PATH;
    }
}
