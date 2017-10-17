package edu.olivet.harvester.job;

import edu.olivet.foundations.job.AutoUpgradeJob;
import edu.olivet.foundations.utils.Tools;
import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/17/17 10:23 AM
 */
public class HarvesterAutoUpgrade extends AutoUpgradeJob {
    @Override
    public void execute() {
        // Sleep randomly to reduce concurrency load of dropbox server
        Tools.sleep(RandomUtils.nextInt(0, 8), TimeUnit.MINUTES);
        Tools.sleep(RandomUtils.nextInt(0, 64), TimeUnit.SECONDS);
        super.execute();
    }

    public void executeWithoutWait() {
        super.execute();
    }


}
