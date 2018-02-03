package edu.olivet.harvester.fulfill.service.flowcontrol;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.service.steps.ClearShoppingCart;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.service.steps.EbatesTransfer;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 4:13 PM
 */
public class OrderFlowEngine extends FlowParent {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderFlowEngine.class);

    @Inject private EbatesTransfer ebatesTransfer;

    @Inject private ClearShoppingCart clearShoppingCart;

    @Inject private MessageListener messageListener;

    @Inject private SheetService sheetService;

    @SuppressWarnings("UnusedReturnValue")
    public FlowState process(Order order, BuyerPanel buyerPanel) {

        FlowState state = new FlowState();
        order.originalRemark = order.remark;

        buyerPanel.setOrder(order);
        state.setOrder(order);
        state.setBuyerPanel(buyerPanel);
        state.setMessageListener(messageListener);
        Step step = ebatesTransfer;
        step.stepName = ebatesTransfer.getClass().toString();

        Exception exception = null;

        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                processSteps(step, state);
                return state;
            } catch (OrderSubmissionException e) {
                clearShoppingCart.processStep(state);
                throw e;
            } catch (Exception e) {
                LOGGER.error("", e);
                if (Strings.containsAnyIgnoreCase(e.getMessage(), JXBrowserHelper.CHANNEL_CLOSED_MESSAGE)) {
                    buyerPanel = TabbedBuyerPanel.getInstance().reInitTabForOrder(order, buyerPanel.getBuyer());
                    state.setBuyerPanel(buyerPanel);
                    WaitTime.Short.execute();
                }
                //noinspection UnusedAssignment
                order = sheetService.reloadOrder(order);
                state.setOrder(order);

                exception = e;

                //throw
            }


        }

        throw new BusinessException(exception);
    }


}
