package edu.olivet.harvester.fulfill.service.steps;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.page.EbatesTransferPage;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 10:43 AM
 */
public class EbatesTransfer extends Step {
    private static final Logger LOGGER = LoggerFactory.getLogger(EbatesTransfer.class);

    @Inject Login login;
    @Override
    protected Step createDynamicInstance(FlowState state) {
        return login;
    }

    @Override
    protected void process(FlowState state) {
        EbatesTransferPage ebatesTransferPage = new EbatesTransferPage(state.getBuyerPanel());
        ebatesTransferPage.execute(state.getOrder());
    }
}
