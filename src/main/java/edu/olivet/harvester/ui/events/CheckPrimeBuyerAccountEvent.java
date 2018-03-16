package edu.olivet.harvester.ui.events;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/9/2018 11:32 AM
 */
public class CheckPrimeBuyerAccountEvent implements HarvesterUIEvent {
    @Override
    public void execute() {
        List<BuyerAccountSetting> accountSettings = BuyerAccountSettingUtils.load().getAccountSettings();
        accountSettings.removeIf(it -> !it.isPrime());

        if (CollectionUtils.isEmpty(accountSettings)) {
            UITools.error("No prime buyer accounts found.");
            return;
        }

        MessagePanel messagePanel = new ProgressDetail(Actions.CheckPrimeBuyerAccount);
        for (BuyerAccountSetting accountSetting : accountSettings) {
            Country country = "all".equalsIgnoreCase(accountSetting.getCountryName()) ? Country.US : Country.fromCode(accountSetting.getCountryName());
            if (country == Country.JP || country == Country.MX) {
                continue;
            }
            BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, accountSetting.getBuyerAccount());
            Browser browser = buyerPanel.getBrowserView().getBrowser();

            LoginPage loginPage = new LoginPage(buyerPanel);
            loginPage.execute(null);

            WaitTime.Short.execute();

            JXBrowserHelper.waitUntilVisible(browser, "#nav-link-prime");
            DOMElement primeLink = JXBrowserHelper.selectVisibleElement(browser, "#nav-link-prime");
            String href = primeLink.getAttribute("href");


            if (Strings.containsAnyIgnoreCase(href, "nav_prime_try_btn")) {
                accountSetting.setValidPrime(false);
                messagePanel.displayMsg(country + " " + accountSetting.getBuyerAccount().getEmail() + " is not a valid prime account", InformationLevel.Negative);
            } else {
                accountSetting.setValidPrime(true);
                messagePanel.displayMsg(country + " " + accountSetting.getBuyerAccount().getEmail() + " is a valid prime account", InformationLevel.Positive);
            }

            accountSetting.setLastPrimeCheck(new Date());

            BuyerAccountSettingUtils.load().save(accountSetting);
            ///gp/prime/ref=nav_prime_try_btn
            WaitTime.Short.execute();
        }
    }
}
