package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.service.FlowFactory.FlowParent;
import edu.olivet.harvester.fulfill.service.FlowFactory.FlowState;
import edu.olivet.harvester.fulfill.service.FlowFactory.Step;
import edu.olivet.harvester.fulfill.service.steps.ClearShoppingCart;
import edu.olivet.harvester.fulfill.service.steps.Login;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.MessageListener;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 4:13 PM
 */
public class OrderFlowEngine extends FlowParent {

    public static final Map<Integer, String> STEPS = new HashMap<>();

    @Inject
    Login login;

    @Inject
    ClearShoppingCart clearShoppingCart;

    @Inject
    MessageListener messageListener;

    public FlowState process(Order order, BuyerPanel buyerPanel) {

        FlowState state = new FlowState();
        buyerPanel.setOrder(order);
        state.setOrder(order);
        state.setBuyerPanel(buyerPanel);
        Step step = login;
        step.stepName = login.getClass().toString();

        try {
            processSteps(step, state);
        } catch (Exception e) {
            //clear shopping cart
            state.setStopFlag(true);
            clearShoppingCart.processStep(state);
            throw new BusinessException(e);
        }
        return state;
    }


}
