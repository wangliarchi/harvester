package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMFormControlElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.LoginVerificationService;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 10:58 AM
 */
public class LoginPage extends FulfillmentPage implements PageObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginPage.class);

    private static final String EMAIL_SELECTOR = "#ap_email,input[name=email]";
    private static final String PASSWORD_SELECTOR = "#ap_password,input[name=password]";
    private static final String CONTINUE_BTN_SELECTOR = "#continue";

    private Order order;

    public LoginPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    public void execute(Order order) {
        if (isLoggedIn()) {
            LOGGER.info("{} buyer account{} already logged in.", country.name(), buyer.getEmail());
            return;
        }

        this.order = order;

        login();

    }

    public boolean isLoggedIn() {

        //let the browser go to order history page
        JXBrowserHelper.loadPage(browser, country.baseUrl() + "/" + AmazonPage.OrderList.urlMark());

        DOMElement email = JXBrowserHelper.selectElementByCssSelector(browser, EMAIL_SELECTOR);

        return email == null;

    }

    @Repeat(expectedExceptions = BusinessException.class)
    public void login() {
        long start = System.currentTimeMillis();
        if (order != null) {
            JXBrowserHelper.saveOrderScreenshot(order, buyerPanel, "0");
        }

        DOMElement email = JXBrowserHelper.selectElementByCssSelector(browser, EMAIL_SELECTOR);
        ((DOMFormControlElement) email).setValue(buyer.getEmail());
        WaitTime.Shortest.execute();

        //amazon sometimes split login process into 2 pages, one page to enter email and next page to enter pw.
        DOMElement continueBtn = JXBrowserHelper.selectElementByCssSelector(browser, CONTINUE_BTN_SELECTOR);
        if (continueBtn != null) {
            Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> ((DOMFormControlElement) email).getForm().submit());
        }

        DOMElement pawssword = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, PASSWORD_SELECTOR);
        ((DOMFormControlElement) pawssword).setValue(buyer.getPassword());
        WaitTime.Shortest.execute();
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> ((DOMFormControlElement) pawssword).getForm().submit());


        if (StringUtils.containsIgnoreCase(browser.getURL(), AmazonPage.Login.urlMark())) {
            throw new IllegalStateException("Error while logging in");
        }

        //check if verification code is requested
        DOMElement codeRequested = JXBrowserHelper.selectElementByName(browser, "claimspicker");
        if (codeRequested != null) {
            JXBrowserHelper.selectElementByCssSelector(browser, "#continue").click();
            WaitTime.Shortest.execute();
            enterVerificationCode();
       }


        LOGGER.info("{} logged in successfully, now at {} -> {}, took {}", country.name(), browser.getTitle(), browser.getURL(), Strings.formatElapsedTime(start));

    }

    @Repeat
    public void enterVerificationCode() {

        //fetch code from email
        DOMElement codeField = JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, ".cvf-widget-input.cvf-widget-input-code");

        try {
            WaitTime.Long.execute();
            String verificationCode = LoginVerificationService.readVerificationCodeFromGmail(buyer.getEmail());
            JXBrowserHelper.fillValueForFormField(browser, ".cvf-widget-input.cvf-widget-input-code", verificationCode);
            WaitTime.Short.execute();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch verification code.",e);
            UITools.info("Please enter login verification code.");
            WaitTime.Short.execute();
            while (true) {
                String enteredCode = codeField.getAttribute("value");
                if (StringUtils.length(enteredCode) >= 6) {
                    break;
                }
            }
        }

        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> ((DOMFormControlElement) codeField).getForm().submit());

        if(JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, ".cvf-widget-input.cvf-widget-input-code") != null) {
            throw new BusinessException("Invalid code. Please check your code and try again.");
        }
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Order Submission Demo");
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);

        //buyer = new Account("reviewsstudy@gmail.com/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);

        frame.getContentPane().add(buyerPanel);
        frame.setVisible(true);
        frame.setSize(new Dimension(1260, 736));
        LoginPage page = new LoginPage(buyerPanel);

        if (page.isLoggedIn()) {
            LOGGER.info("buyer account already logged in.");
            return;
        }

        page.login();

    }
}
