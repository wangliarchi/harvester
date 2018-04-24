package edu.olivet.harvester.fulfill.service.flowcontrol;

import edu.olivet.harvester.utils.common.Strings;
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

    public String stepName = this.getClass().getName();


    protected abstract Step createDynamicInstance(FlowState state);

    protected abstract void process(FlowState state);


    // Step children override with unique processing
    public Step processStep(FlowState state) {
        long start = System.currentTimeMillis();
        LOGGER.info("Starting {}", this.getClass().getName());
        if (StringUtils.isBlank(stepName)) {
            stepName = this.getClass().getName();
        }
        state.steps.add(this);

        process(state);

        state.setPrevStep(this);
        state.saveScreenshot();

        this.nextStep = createDynamicInstance(state);
        LOGGER.info("Finished {}, took {}", this.getClass().getName(), Strings.formatElapsedTime(start));
        return this.nextStep;
    }


}
