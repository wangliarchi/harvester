package edu.olivet.harvester.fulfill.service.flowcontrol;

import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/27/17 3:48 PM
 */
@Data
public class FlowState {
    public List<Step> steps;
    private Step prevStep;
    public boolean stopFlag = false;
    private BuyerPanel buyerPanel;
    private Order order;
    private MessageListener messageListener;

    public FlowState() {
        this.steps = new ArrayList<>();
    }

    public void saveScreenshot() {
        String substep = "0";
        if (this.steps.get(this.steps.size() - 1) == this.prevStep) {
            substep = "1";
        }

        JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, steps.size() + "." + substep);


    }


}
