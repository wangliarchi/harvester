package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.MarkStatusService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.utils.RuntimeSettingsValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Observable;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:15 AM
 */
public class MarkStatusEvent extends Observable implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkStatusEvent.class);

    @Inject
    RuntimeSettingsValidator validator;
    @Inject
    MarkStatusService markStatusService;

    public void excute() {
        //validate runtime setting
        PSEventListener.reset();
        RuntimeSettings settings = RuntimeSettings.load();
        RuntimeSettingsValidator.CheckResult result = validator.validate(settings, FulfillmentEnum.Action.UpdateStatus);

        List<String> messages = result.getErrors();
        if (CollectionUtils.isNotEmpty(messages)) {
            UITools.error(StringUtils.join(messages, StringUtils.LF), UIText.title("title.conf_error"));
            return;
        }
        markStatusService.excute(settings);
    }


}
