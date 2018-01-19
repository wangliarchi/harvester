package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.OrderSubmitter;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;

import java.util.Observable;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:15 AM
 */
public class SubmitOrdersEvent extends Observable implements HarvesterUIEvent {
    //private static final Logger LOGGER = LoggerFactory.getLogger(SubmitOrdersEvent.class);


    @Inject private
    OrderSubmitter orderSubmitter;


    public void execute() {
        RuntimeSettings settings = RuntimeSettings.load();
        orderSubmitter.execute(settings);
    }


}
