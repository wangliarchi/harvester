package edu.olivet.harvester.fulfill.service.FlowFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 3:47 PM
 */
abstract public class FlowParent {
    protected void processSteps(Step step, FlowState state) {
        Boolean keepGoing = true;
        while (keepGoing) {
            step = step.processStep(state);
            if (step == null) {
                keepGoing = false;
            }
        }
    }
}