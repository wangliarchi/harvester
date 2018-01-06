package edu.olivet.harvester.ui.panel;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/22/2017 9:52 AM
 */

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyCode;
import com.teamdev.jxbrowser.chromium.Cookie;
import com.teamdev.jxbrowser.chromium.CookieStorage;
import com.teamdev.jxbrowser.chromium.dom.*;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.exception.AuthenticationFailException;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.fulfill.exception.Exceptions.RobotFoundException;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * 单个卖家后台面板
 *
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 10/19/2017 9:35 AM
 */
public class SellerPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerPanel.class);

    /**
     * 面板显示编号，从1开始
     */
    @Getter private final int id;

    @Getter private final Country country;

    @Getter private final Account seller;

    @Getter private BrowserView browserView;

    @Getter private Browser browser;

    public SellerPanel(int id, Country country, Account seller, double zoomLevel) {
        super(new BorderLayout());

        this.id = id;
        this.country = country;
        this.seller = seller;
        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel);
        this.add(browserView, BorderLayout.CENTER);
        this.browser = browserView.getBrowser();
    }

    private String profilePathName() {
        return this.seller.key() + Constants.HYPHEN + this.id;
    }


    public CookieStorage loginSellerCentral(final Country country) {

        //load seller center page
        JXBrowserHelper.loadPage(browser, country.ascBaseUrl());

        DOMElement email = JXBrowserHelper.selectElementByCssSelector(browser, "#ap_email,input[name=email]");
        if (email == null) {
            LOGGER.warn("{} ASC可能已经登录：{} -> {}", country.name(), browser.getTitle(), browser.getURL());
            this.selectMarketplace(country);
            return browser.getCookieStorage();
        } else {
            JXBrowserHelper.fillValueForFormField(browser, "#ap_email,input[name=email]", seller.getEmail());
            JXBrowserHelper.fillValueForFormField(browser, "#ap_password,input[name=password]", seller.getPassword());
            WaitTime.Shorter.execute();
            JXBrowserHelper.insertChecker(browser);
            ((DOMFormControlElement) email).getForm().submit();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);

            LOGGER.info("{} ASC登录操作执行完毕之后：{} -> {}", country.name(), browser.getTitle(), browser.getURL());
            // 如果出现Remind me later直接点击进入下一个环节
            if (JXBrowserHelper.selectElementByCssSelector(browser, "#merchant-picker-btn-enroll-in-2sv") != null) {
                JXBrowserHelper.loadPage(browser, country.ascBaseUrl() + "/merchant-picker/status/skip-2sv");
            }

            DOMElement error = JXBrowserHelper.selectElementByCssSelector(browser, "#message_error");
            if (error != null && StringUtils.isNotBlank(error.getInnerText())) {
                throw new AuthenticationFailException(String.format("无法登录%s卖家后台：%s", country.name(), error.getInnerText().trim()));
            }

            String cssSelector = "input.cvf-widget-input-code, span.cvf-widget-btn-verify, h1:contains(Verifying that it's you)";
            String sellerEmail = seller.key();
            if ((browser.getTitle().contains("Please confirm your identity") ||
                    JXBrowserHelper.selectElementByCssSelector(browser, cssSelector) != null) &&
                    JXBrowserHelper.selectElementByName(browser, "code") != null) {
                String errorMsg = String.format("尝试登录%s ASC失败，请输入%s对应Verification Code继续", country.name(), sellerEmail);
                throw new RobotFoundException(errorMsg);
            }

            if (browser.getTitle().contains("Two-Step Verification") ||
                    JXBrowserHelper.selectElementByCssSelector(browser, "#auth-mfa-otpcode,#auth-mfa-remember-device") != null) {
                String errorMsg = String.format("尝试登录%s ASC失败，请输入%s对应Two-Step Verification Code继续", country.name(), sellerEmail);
                throw new RobotFoundException(errorMsg);
            }

            if (JXBrowserHelper.selectElementByCssSelector(browser, "#ap_captcha_img") != null) {
                throw new RobotFoundException(String.format("尝试登录%s ASC失败，请输入%s对应验证码继续", country.name(), sellerEmail));
            }

            this.selectMarketplace(country);
            // 尝试保存一份Cookie供其他窗口复用
            return browser.getCookieStorage();
        }
    }

    /**
     * 在需要时，在ASC切换到给定卖场
     *
     * @param country 给定卖场
     */
    private void selectMarketplace(Country country) {
        if (this.isMarketplaceSelected(country)) {
            return;
        }

        List<DOMElement> options = JXBrowserHelper.selectElementsByCssSelector(browser, OPTION_SELECTOR);
        for (DOMElement option : options) {
            String url = StringUtils.defaultString(option.getAttribute("value"));
            if (StringUtils.isNotBlank(url) && url.contains(country.marketPlaceId())) {
                LOGGER.info("准备切换卖场到{} -> {}", StringUtils.defaultString(option.getTextContent()).trim(), url);
                JXBrowserHelper.loadPage(browser, country.ascBaseUrl() + url);
                WaitTime.Short.execute();
                break;
            }
        }

        // 确认卖场是否正确切换、选中
        if (!this.isMarketplaceSelected(country)) {
            String filePath = new File(Directory.Tmp.path(),
                    seller.key() + "/switch-marketplace-" + country.name() + "-" + Dates.nowAsFileName() + ".png").getAbsolutePath();
            JXBrowserHelper.saveScreenshot(filePath, browserView);
            throw new BusinessException("Failed to switch to marketplace of " + country.name());
        }
    }

    private static final String OPTION_SELECTOR = "#sc-mkt-picker-switcher-select option";

    private boolean isMarketplaceSelected(Country country) {
        Browser browser = browserView.getBrowser();
        JXBrowserHelper.wait(browser, By.cssSelector(OPTION_SELECTOR));
        DOMSelectElement selection = (DOMSelectElement) JXBrowserHelper.selectElementByCssSelector(browser, "#sc-mkt-picker-switcher-select");
        List<DOMOptionElement> options = selection.getOptions();
        for (DOMElement option : options) {
            if (!(option instanceof DOMOptionElement)) {
                continue;
            }

            String url = option.getAttribute("value");
            if (((DOMOptionElement) option).isSelected() && url.contains(country.marketPlaceId())) {
                LOGGER.info("{}卖场已经选中：{}", country.name(), StringUtils.defaultString(option.getTextContent()).trim());
                return true;
            }
        }

        return false;
    }


    public MarketWebServiceIdentity fetchMWSInfo() {

        loginSellerCentral(country);
        WaitTime.Short.execute();
        JXBrowserHelper.waitUntilVisible(browser,".sc-mkt-picker-switcher-txt");
        LOGGER.info("");

        //gp/account-manager/home.html
        JXBrowserHelper.loadPage(browser, country.ascBaseUrl() + "/gp/account-manager/home.html");
        JXBrowserHelper.waitUntilVisible(browser, "#view-credentials-button");

        String sellerId = JXBrowserHelper.text(browser, "#merchant-id");
        String storeName = JXBrowserHelper.text(browser, ".sc-mkt-picker-switcher-txt");


        JXBrowserHelper.selectElementByCssSelector(browser, "#view-credentials-button").click();
        JXBrowserHelper.waitUntilVisible(browser, "#mws-selfauth-secret-show");
        JXBrowserHelper.selectVisibleElement(browser, "#mws-selfauth-secret-show").click();
        JXBrowserHelper.waitUntilVisible(browser, "#mws-selfauth-secret-value");

        String mwsAccessKey = JXBrowserHelper.text(browser, "#mws-selfauth-access-key");
        String mwsSecretKey = JXBrowserHelper.text(browser, "#mws-selfauth-secret-value");

        MarketWebServiceIdentity marketWebServiceIdentity = new MarketWebServiceIdentity();
        marketWebServiceIdentity.setSellerId(sellerId + "\t" + storeName);
        marketWebServiceIdentity.setMarketPlaceId(country.marketPlaceId());
        marketWebServiceIdentity.setSecretKey(mwsSecretKey);
        marketWebServiceIdentity.setAccessKey(mwsAccessKey);

        return marketWebServiceIdentity;

        //
    }


    private static final String SUBMIT_BTN_SELECTOR = ".sm-view-footer-buttons span.a-button-primary input.a-button-input";

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");


    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(1400, 900));
        frame.setTitle("Seller Panel Demo");

        Configuration config = Settings.load().getConfigByCountry(Country.US);
        SellerPanel sellerPanel = new SellerPanel(1, Country.US, config.getSeller(), 1);
        frame.getContentPane().add(sellerPanel);

        UITools.setDialogAttr(frame, true);

       MarketWebServiceIdentity identity =  sellerPanel.fetchMWSInfo();
       System.out.println(identity);
    }
}
