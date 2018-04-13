package edu.olivet.harvester.job;

import com.dropbox.core.DbxException;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.ui.Harvester;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/17/17 10:25 AM
 */
public class LogUploader extends AbstractBackgroundJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUploader.class);
    private static final long ZERO_BYTES = 0L;

    @Override
    public void execute() {
        try {
            // Sleep randomly to reduce concurrency load of dropbox server
            Tools.sleep(RandomUtils.nextInt(0, 8), TimeUnit.MINUTES);
            Tools.sleep(RandomUtils.nextInt(0, 64), TimeUnit.SECONDS);
            this.executeWithoutWait();
        } catch (IOException | DbxException e) {
            LOGGER.error("Failed to upload log file of {} to dropbox server:", Dates.today(), e);
        }
    }


    private void executeWithoutWait() throws IOException, DbxException {
        File log = new File(Directory.Log.path(), "harvester." + Dates.today() + ".log");
        if (log.exists() && log.length() > ZERO_BYTES) {
            //noinspection CheckStyle
            DropboxAssistant DBX_CLIENT = new DropboxAssistant(Harvester.APP_NAME);
            // Relatively big log file will wait extra time to upload
            if (log.length() > FileUtils.ONE_MB) {
                WaitTime.Long.execute();
            }
            Settings settings = Settings.load();
            DBX_CLIENT.upload(log, "/logs/" + settings.getSid() + "_" + SystemUtils.USER_NAME.trim());
        }
    }

    public static void main(String[] args) {
        LogUploader logUploader = new LogUploader();
        try {
            logUploader.executeWithoutWait();
        } catch (IOException | DbxException e) {
            e.printStackTrace();
        }
    }
}

