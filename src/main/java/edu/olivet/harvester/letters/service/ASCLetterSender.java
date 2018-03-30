package edu.olivet.harvester.letters.service;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.ui.panel.SellerPanel;
import edu.olivet.harvester.ui.utils.SellerPanelManager;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 4:02 PM
 */
public class ASCLetterSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ASCLetterSender.class);


    public void sendForOrder(Order order, Letter letter) {
        Country marketplaceCountry = OrderCountryUtils.getMarketplaceCountry(order);
        Country settingCountry = marketplaceCountry.europe() ? Country.UK : marketplaceCountry;
        Account seller = Settings.load().getConfigByCountry(settingCountry).getSeller();
        SellerPanel sellerPanel = SellerPanelManager.getTab(seller, settingCountry);
        sellerPanel.loginSellerCentral(marketplaceCountry);

        boolean result = sellerPanel.sendMessage(order, letter.toMessage());

        if (!result) {
            throw new BusinessException("Fail to send gray letter via asc");
        }

    }
}
