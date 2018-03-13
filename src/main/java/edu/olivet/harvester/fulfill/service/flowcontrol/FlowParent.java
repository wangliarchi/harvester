package edu.olivet.harvester.fulfill.service.flowcontrol;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.steps.AfterOrderPlaced;
import edu.olivet.harvester.utils.MessageListener;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 3:47 PM
 */
public abstract class FlowParent {
    @Inject
    MessageListener messageListener;


    public void processSteps(Step step, FlowState state) {

        while (true) {
            if (step == null) {
                break;
            }

            //paused
            while (PSEventListener.paused() || state.getBuyerPanel().paused()) {
                state.getBuyerPanel().disablePauseButton();
                //messageListener.addMsg("Process paused, wait for " + WaitTime.Short.val() + " seconds...");
                WaitTime.Normal.execute();
                if (isStopped(state)) {
                    break;
                }
            }
            state.getBuyerPanel().enablePauseButton();
            //stopped
            if (isStopped(state)) {
                //cant stop if order is actually placed.
                if (AfterOrderPlaced.class.getName().equals(step.stepName)) {
                    PSEventListener.resume();
                    state.getBuyerPanel().resume();
                } else {
                    throw new OrderSubmissionException("Process stopped as requested.");
                }
            }


            step = step.processStep(state);

        }
    }

    @Inject private OrderSubmissionTaskService orderSubmissionTaskService;

    private boolean isStopped(FlowState state) {
        boolean taskStopped = false;
        try {
            OrderSubmissionTask task = orderSubmissionTaskService.get(state.getOrder().getTask().getId());
            taskStopped = task.stopped();
        } catch (Exception e) {
            //return false;
        }

        return taskStopped || PSEventListener.stopped() || state.getBuyerPanel().stopped();

    }
}