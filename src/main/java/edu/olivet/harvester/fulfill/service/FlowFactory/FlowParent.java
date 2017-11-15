package edu.olivet.harvester.fulfill.service.FlowFactory;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.utils.MessageListener;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 3:47 PM
 */
public abstract class FlowParent {
    @Inject
    MessageListener messageListener;
    protected void processSteps(Step step, FlowState state) {
        Boolean keepGoing = true;
        while (keepGoing) {


            while (PSEventListener.pause) {
                messageListener.addMsg("Process paused, wait for 10 seconds...");
                WaitTime.Long.execute();
            }

            if (PSEventListener.stop) {
                messageListener.addMsg("Process stopped as requested.");
                keepGoing = false;
                return;
            }
            step = step.processStep(state);
            if (step == null) {
                keepGoing = false;
            }
        }
    }
}