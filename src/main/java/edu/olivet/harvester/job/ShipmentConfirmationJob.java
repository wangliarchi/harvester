package edu.olivet.harvester.job;

import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.feeds.ConfirmShipments;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class ShipmentConfirmationJob extends AbstractBackgroundJob {

    @Override
    public void execute() {
        ApplicationContext.getBean(ConfirmShipments.class).execute();
    }

}
