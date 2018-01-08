package edu.olivet.harvester.ui;

import edu.olivet.deploy.Application;
import edu.olivet.foundations.job.AutoUpgradeJob;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.job.ContextUploadJob;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.dialog.SettingsDialog;
import edu.olivet.harvester.ui.panel.ProgressLogsPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Migration;
import edu.olivet.harvester.utils.SettingValidator;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;

/**
 * Harvester: The Automated Order Fulfillment Solution
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:58 AM
 */
public class Harvester {
    public static final String APP_NAME = "Harvester";
    public static boolean debugFlag = false;

    private static final Application APP = new Application(APP_NAME);

    public static void main(String[] args) {
        UITools.init(APP);

        if (SystemUtils.IS_OS_WINDOWS) {
            new AutoUpgradeJob().execute();
        }

        if (!new File(Settings.CONFIG_FILE_PATH).exists()) {

            if (Migration.hasMigrationFile() &&
                    UITools.confirmed("OrderMan configuration is found. Do you want to migrate it to Harvester?")) {
                Migration.setUseMigration(true);
            } else {
                Migration.setUseMigration(false);
                UITools.info("Please configure fulfillment requirements for Harvester.");
            }

            SettingsDialog dialog = new SettingsDialog(new SettingValidator(new AppScript(), ApplicationContext.getBean(SheetAPI.class)));
            UITools.setDialogAttr(dialog);
            if (!dialog.isOk() || dialog.getSettings() == null) {
                UITools.error("Harvester cannot run without necessary configurations!");
                System.exit(4);
            }

        }


        UIHarvester uiHarvester = ApplicationContext.getBean(UIHarvester.class);
        UITools.setIconAndPosition(uiHarvester);

        uiHarvester.setVisible(true);
        uiHarvester.startBackgroundJobs();

        ApplicationContext.getBean(ContextUploadJob.class).execute();


        MessageListener messageListener = ApplicationContext.getBean(MessageListener.class);
        messageListener.setContainer(ProgressLogsPanel.getInstance());
        messageListener.start();
        messageListener.addLongMsg("Harvester Started Successfully!", InformationLevel.Positive);

        Migration.migrateCreditCardSettings();
        try {
            new AppScript().preloadAllSpreadsheets();
        } catch (Exception e) {
            //ignore, error logged by appscript
        }
    }

}
