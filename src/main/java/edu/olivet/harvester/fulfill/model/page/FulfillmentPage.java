package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import lombok.Getter;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 10:49 AM
 */
public abstract class FulfillmentPage implements PageObject {
    @Getter
    protected BuyerPanel buyerPanel;
    protected Browser browser;
    protected Account buyer;
    protected Country country;

    public FulfillmentPage(BuyerPanel buyerPanel) {
        this.buyerPanel = buyerPanel;
        this.browser = buyerPanel.getBrowserView().getBrowser();
        try {
            this.buyer = BuyerAccountSettingUtils.load().getByEmail(buyerPanel.getBuyer().getEmail()).getBuyerAccount();
        } catch (Exception e) {
            //
            this.buyer = buyerPanel.getBuyer();
        }
        this.country = buyerPanel.getCountry();
    }
}
