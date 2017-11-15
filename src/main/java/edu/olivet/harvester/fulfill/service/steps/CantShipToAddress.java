package edu.olivet.harvester.fulfill.service.steps;

import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.fulfill.service.flowfactory.FlowState;
import edu.olivet.harvester.fulfill.service.flowfactory.Step;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 3:32 PM
 */
public class CantShipToAddress extends Step {
    @Override
    public Step createDynamicInstance(FlowState state) {
        return null;
    }

    @Override
    protected void process(FlowState state) {
        throw new BusinessException("Sorry, this item can't be shipped to selected address.  ");
    }
}
