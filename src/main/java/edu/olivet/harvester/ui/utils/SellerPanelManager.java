package edu.olivet.harvester.ui.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.ui.panel.SellerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/28/2018 4:23 PM
 */
public class SellerPanelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerPanelManager.class);

    public static SellerPanel getTab(Account seller, Country country) {
        final long start = System.currentTimeMillis();

        String tabKey = SellerPanel.getKey(country, seller);

        SellerPanel sellerPanel;
        try {
            sellerPanel = (SellerPanel) TabbedBuyerPanel.getInstance().getWebPanel(tabKey);
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            return sellerPanel;
        } catch (Exception e) {
            //
        }

        sellerPanel = new SellerPanel(country, seller);
        TabbedBuyerPanel.getInstance().addTab(sellerPanel);

        LOGGER.info("Finished init panel {} ，took{}", sellerPanel.getKey(), Strings.formatElapsedTime(start));

        return sellerPanel;
    }
}
