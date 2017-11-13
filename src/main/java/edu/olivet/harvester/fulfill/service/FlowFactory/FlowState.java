package edu.olivet.harvester.fulfill.service.FlowFactory;

import edu.olivet.foundations.utils.Directory;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
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

    public FlowState() {
        this.steps = new ArrayList<Step>();
    }

    public void saveScreenshot() {
        String substep = "0";
        if (this.steps.get(this.steps.size() - 1) == this.prevStep) {
            substep = "1";
        }
        String filePath = Directory.WebPage.path() + "/orders/" + order.sheetName.replaceAll("/", "") + "/" + order.row + "_" + order.order_id + "/images/" + steps.size() + "." + substep + "-" + buyerPanel.getBrowserView().getBrowser().getTitle().replaceAll(" ", "") + ".png";
        JXBrowserHelper.saveScreenshot(filePath, buyerPanel.getBrowserView());

        String htmlFilePath = filePath.replaceAll(".png", ".html").replaceAll("/images/", "/html/");
        JXBrowserHelper.saveHTMLSourceFile(htmlFilePath, buyerPanel.getBrowserView());

    }


}
