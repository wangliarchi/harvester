package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.service.StepHelper;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.fulfill.utils.pagehelper.GiftOptionHelper;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/18/2017 1:41 PM
 */
public class GiftOption extends Step {


    @Override
    protected void process(FlowState state) {
        GiftOptionHelper.giftOption(state.getBuyerPanel().getBrowserView().getBrowser(),state.getOrder());
    }

    @Inject private StepHelper stepHelper;
    @Override
    public Step createDynamicInstance(FlowState state) {
        return stepHelper.detectStep(state);
    }
}
