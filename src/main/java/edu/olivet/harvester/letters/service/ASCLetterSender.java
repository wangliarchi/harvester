package edu.olivet.harvester.letters.service;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.ui.panel.SellerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 4:02 PM
 */
public class ASCLetterSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ASCLetterSender.class);

    public void sendForOrder(Order order) {
        Country marketplaceCountry = OrderCountryUtils.getMarketplaceCountry(order);
        Country settingCountry = marketplaceCountry.europe() ? Country.UK : marketplaceCountry;
        Account seller = Settings.load().getConfigByCountry(settingCountry).getSeller();
        SellerPanel sellerPanel = addTab(seller, settingCountry);
        sellerPanel.loginSellerCentral(marketplaceCountry);
    }


    public SellerPanel addTab(Account seller, Country country) {
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
