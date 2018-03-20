package edu.olivet.harvester.utils;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/9/2018 11:57 AM
 */
public class BuyerUtils {
    static Map<String, Boolean> AccountPrimeCheckedCache = new ConcurrentHashMap<>();

    //public static synchronized void setAsValidPrime(Country country, Account buyer) {
    //    String key = country.name() + buyer.getEmail();
    //    AccountPrimeCheckedCache.put(key, true);
    //}

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static synchronized boolean isValidPrime(Country country, Account buyer) {
        String key = country.name() + buyer.getEmail();
        //only check once
        if (AccountPrimeCheckedCache.containsKey(key)) {
            return AccountPrimeCheckedCache.get(key);
        }

        if (country != Country.US && country != Country.UK) {
            AccountPrimeCheckedCache.put(key, true);
            return true;
        }

        BuyerAccountSetting buyerAccountSetting = BuyerAccountSettingUtils.load().getByEmail(buyer.getEmail());
        if (buyerAccountSetting.getLastPrimeCheck() != null && buyerAccountSetting.getLastPrimeCheck().after(DateUtils.addDays(new Date(), -1))) {
            AccountPrimeCheckedCache.put(key, buyerAccountSetting.isValidPrime());
            return buyerAccountSetting.isValidPrime();
        }

        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        LoginPage loginPage = new LoginPage(buyerPanel);
        for (int i = 0; i < 3; i++) {
            try {
                loginPage.execute(null);
                break;
            } catch (Exception e) {
                //
            }
        }

        if (LoginPage.needLoggedIn(browser)) {
            throw new BusinessException("Log in buyer account " + buyer.getEmail() + " failed");
        }

        JXBrowserHelper.waitUntilVisible(browser, "#nav-link-prime");
        DOMElement primeLink = JXBrowserHelper.selectVisibleElement(browser, "#nav-link-prime");
        String href = primeLink.getAttribute("href");
        boolean result = !Strings.containsAnyIgnoreCase(href, "nav_prime_try_btn");
        buyerAccountSetting.setValidPrime(result);
        buyerAccountSetting.setLastPrimeCheck(new Date());
        BuyerAccountSettingUtils.load().save(buyerAccountSetting);

        AccountPrimeCheckedCache.put(key, result);
        return result;
    }
}
