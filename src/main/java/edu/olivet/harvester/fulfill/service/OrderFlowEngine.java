package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Or;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.exception.OrderSubmissionException;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowParent;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;
import edu.olivet.harvester.fulfill.service.steps.ClearShoppingCart;
import edu.olivet.harvester.fulfill.service.steps.Login;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 4:13 PM
 */
public class OrderFlowEngine extends FlowParent {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderFlowEngine.class);

    @Inject
    Login login;

    @Inject
    ClearShoppingCart clearShoppingCart;

    @Inject
    MessageListener messageListener;

    @Inject SheetService sheetService;

    @Repeat(expectedExceptions = BusinessException.class)
    public FlowState process(Order order, BuyerPanel buyerPanel) {

        FlowState state = new FlowState();
        buyerPanel.setOrder(order);
        state.setOrder(order);
        state.setBuyerPanel(buyerPanel);
        state.setMessageListener(messageListener);
        Step step = login;
        step.stepName = login.getClass().toString();

        try {
            processSteps(step, state);
        } catch (OrderSubmissionException e) {
            clearShoppingCart.processStep(state);
            throw e;
        } catch (Exception e) {
            LOGGER.error("", e);
            clearShoppingCart.processStep(state);
            order = sheetService.reloadOrder(order);
            throw new BusinessException(e);
        }


        return state;
    }


}
