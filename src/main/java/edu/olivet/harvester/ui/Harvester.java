package edu.olivet.harvester.ui;

import edu.olivet.deploy.Application;
import edu.olivet.foundations.job.AutoUpgradeJob;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.harvester.job.ContextUploadJob;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Migration;
import edu.olivet.harvester.utils.SettingValidator;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;

/**
 * Harvester: The Automated Order Fulfillment Solution
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:58 AM
 */
public class Harvester {

    public static final String CONFIG_FILE_PATH = Directory.Customize.path() + "/harvester-config.json";


    public static final String APP_NAME = "Harvester";

    private static final Application APP = new Application(APP_NAME);

    public static void main(String[] args) {
        UITools.init(APP);

        if (SystemUtils.IS_OS_WINDOWS) {
            new AutoUpgradeJob().execute();
        }

        if (!new File(CONFIG_FILE_PATH).exists()) {

            if (Migration.hasMigrationFile() && UITools.confirmed("OrderMan configuration is found. Do you want to migrate it to Harvester?")) {
                Migration.setUseMigration(true);
            } else {
                Migration.setUseMigration(false);
                UITools.info("Please configure fulfillment requirements for Harvester.");
            }

            SettingsDialog dialog = UITools.setDialogAttr(new SettingsDialog(new SettingValidator(new AppScript())));
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

        try {
            new AppScript().preloadAllSpreadsheets();
        } catch (Exception e) {
            //ignore, error logged by appscript
        }
    }

}
