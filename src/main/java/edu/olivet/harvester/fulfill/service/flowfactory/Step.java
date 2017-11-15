package edu.olivet.harvester.fulfill.service.flowfactory;

import com.google.inject.Inject;
import edu.olivet.harvester.utils.MessageListener;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 3:49 PM
 */
public abstract class Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(Step.class);
    @Getter
    @Setter
    protected Step nextStep;

    @Getter
    @Setter
    protected Step prevStep;

    public String stepName;


    public abstract Step createDynamicInstance(FlowState state);

    protected abstract void process(FlowState state);

    @Inject
    MessageListener messageListener;

    // Step children override with unique processing
    public Step processStep(FlowState state) {
        LOGGER.info("Starting " + this.getClass().getName());
        if (StringUtils.isBlank(stepName)) {
            stepName = this.getClass().getName();
        }
        state.steps.add(this);
        //save screenshot
        //state.saveScreenshot();
        process(state);
        state.setPrevStep(this);
        state.saveScreenshot();

        this.nextStep = createDynamicInstance(state);
        return this.nextStep;
    }


}
