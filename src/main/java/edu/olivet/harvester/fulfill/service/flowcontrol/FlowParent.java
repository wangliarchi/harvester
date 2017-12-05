package edu.olivet.harvester.fulfill.service.flowcontrol;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.steps.AfterOrderPlaced;
import edu.olivet.harvester.fulfill.service.steps.ClearShoppingCart;
import edu.olivet.harvester.utils.MessageListener;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 3:47 PM
 */
public abstract class FlowParent {
    @Inject
    MessageListener messageListener;

    @Inject
    ClearShoppingCart clearShoppingCart;

    protected void processSteps(Step step, FlowState state) {
        Boolean keepGoing = true;
        while (keepGoing) {
            if (step == null) {
                break;
            }

            while (PSEventListener.paused()) {
                messageListener.addMsg("Process paused, wait for " + WaitTime.Normal.val() + " seconds...");
                WaitTime.Normal.execute();
            }

            if (PSEventListener.stopped()) {
                //cant stop if order is actually placed.
                if (AfterOrderPlaced.class.getName().equals(step.stepName)) {
                    PSEventListener.resume();
                } else {
                    messageListener.addMsg("Process stopped as requested.");
                    step = clearShoppingCart;
                }
            }

            step = step.processStep(state);

            if (step == null) {
                keepGoing = false;
            }
        }
    }
}