package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.OrderSubmitter;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.utils.validation.RuntimeSettingsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:15 AM
 */
public class SubmitOrdersEvent extends Observable implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitOrdersEvent.class);

    @Inject
    RuntimeSettingsValidator validator;

    @Inject
    OrderSubmitter orderSubmitter;

    @Inject
    OrderSubmissionTaskService orderSubmissionTaskService;

    public void execute() {
        RuntimeSettings settings = RuntimeSettings.load();
        orderSubmitter.execute(settings);
    }


}
