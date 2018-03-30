package edu.olivet.harvester.letters.service;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.ui.panel.GmailWebPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 4:02 PM
 */
public class GmailLetterSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(GmailLetterSender.class);

    public void sendForOrder(Order order, Letter letter) {
        if (StringUtils.isBlank(order.buyer_email)) {
            throw new BusinessException("No buyer email found for row " + order.row + " " + order.order_id);
        }
        Country marketplaceCountry = OrderCountryUtils.getMarketplaceCountry(order);
        Country settingCountry = marketplaceCountry.europe() ? Country.UK : marketplaceCountry;
        Account sellerEmailAccount = Settings.load().getConfigByCountry(settingCountry).getSellerEmail();
        GmailWebPanel webPanel = addTab(sellerEmailAccount);

        if (SystemSettings.reload().isOrderSubmissionDebugModel()) {
            order.buyer_email = Constants.RND_EMAIL;
        }
        webPanel.sendMessage(order.buyer_email, letter.getSubject(), letter.getBody());

    }

    public void sendMessageToCS(Order order, Letter letter) {
        Country marketplaceCountry = OrderCountryUtils.getMarketplaceCountry(order);
        Country settingCountry = marketplaceCountry.europe() ? Country.UK : marketplaceCountry;
        Account sellerEmailAccount = Settings.load().getConfigByCountry(settingCountry).getSellerEmail();
        GmailWebPanel webPanel = addTab(sellerEmailAccount);

        webPanel.sendMessage(sellerEmailAccount.getEmail(), letter.getSubject(), letter.getBody());

    }

    public GmailWebPanel addTab(Account emailAccount) {
        final long start = System.currentTimeMillis();
        String tabKey = GmailWebPanel.getKey(emailAccount);
        GmailWebPanel webPanel;
        try {
            webPanel = (GmailWebPanel) TabbedBuyerPanel.getInstance().getWebPanel(tabKey);
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            return webPanel;
        } catch (Exception e) {
            //
        }

        webPanel = new GmailWebPanel(emailAccount);
        TabbedBuyerPanel.getInstance().addTab(webPanel);

        LOGGER.info("Finished init panel {} ，took{}", webPanel.getKey(), Strings.formatElapsedTime(start));
        return webPanel;
    }

}
