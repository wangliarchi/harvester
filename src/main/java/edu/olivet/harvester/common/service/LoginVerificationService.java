package edu.olivet.harvester.common.service;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMFormControlElement;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Account.AccountType;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 3:16 PM
 */
public class LoginVerificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginVerificationService.class);

    public static String readVerificationCodeFromGmail(Account buyer) {
        String gmailAddress = buyer.getEmail();
        if (!StringUtils.contains(gmailAddress.toLowerCase(), "gmail.com")) {
            throw new BusinessException("Non gmail is not supported to fetch verification code.");
        }

        try {
            for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
                String code = Jsoup.connect("https://script.google.com/macros/s/AKfycbzLHToSdGh4tSGM-D5ETYnmo_EH1VdpnWzvDBzZIgFe5X3MFfE/exec")
                        .ignoreContentType(true)
                        .data("func", "readCode").data("email", gmailAddress)
                        .method(Connection.Method.GET).timeout(WaitTime.Long.valInMS()).execute().body();
                if (StringUtils.isNumeric(code) && StringUtils.length(code) == 6) {
                    return code;
                }

                WaitTime.Short.execute();

            }
        } catch (IOException e) {
            LOGGER.error("Failed to read the verification code from Gmail for {}: ", gmailAddress, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred while reading verification code from Gmail {}: ", gmailAddress, e);
        }

        //read code from gmail
        String code = readFromGmail(buyer);
        if (StringUtils.isNotBlank(code)) {
            return code;
        }
        throw new BusinessException("Failed to read the verification code from " + gmailAddress);


    }

    public static String readFromGmail(Account buyer) {
        //init frame
        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setTitle("Buyer gmail panel");
        frame.setVisible(true);
        BrowserView browserView = JXBrowserHelper.init(buyer.key() + "-gmail", -1);
        Browser browser = browserView.getBrowser();
        frame.getContentPane().add(browserView);
        UITools.setDialogAttr(frame, false);

        JXBrowserHelper.loadPage(browser, "https://mail.google.com");
        WaitTime.Short.execute();

        //enter email address
        DOMElement nextBtn = JXBrowserHelper.selectElementByCssSelector(browser, "#identifierNext");
        if (nextBtn != null) {
            JXBrowserHelper.fillValueForFormField(browser, "#identifierId", buyer.getEmail());
            WaitTime.Short.execute();
            JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, "#identifierNext").click();
            //JXBrowserHelper.waitUntilNewPageLoaded(browser);

            //enter password
            JXBrowserHelper.wait(browser, By.id("passwordNext"));
            DOMElement password = JXBrowserHelper.selectElementByName(browser, "password");
            assert password != null;
            ((DOMFormControlElement) password).setValue(buyer.getPassword());
            WaitTime.Short.execute();

            JXBrowserHelper.insertChecker(browser);
            JXBrowserHelper.selectElementByCssSelectorWaitUtilLoaded(browser, "#passwordNext").click();
            JXBrowserHelper.waitUntilNewPageLoaded(browser);
        }

        //find the email
        List<DOMElement> lists = JXBrowserHelper.selectElementsByCssSelector(browser, "table.F.cf.zt td.xY.a4W .xS");
        for (DOMElement list : lists) {
            if (JXBrowserHelper.text(list, ".bog").equals("Your Amazon verification code")) {
                list.click();
                WaitTime.Short.execute();
                break;
            }
        }

        //
        List<DOMElement> ps = JXBrowserHelper.selectElementsByCssSelector(browser, "td p");
        String code = "";
        for (DOMElement p : ps) {
            String t = p.getInnerHTML().trim();
            if (t.length() == 6) {
                code = t.replaceAll(Regex.NON_ALPHA_LETTER_DIGIT.val(), "");
                if (code.length() == 6) {
                    break;
                }
            }
        }

        frame.setVisible(false); //you can't see me!
        frame.dispose();

        return code;

    }

    public static void main(String[] args) {
        Account buyer = new Account("MaisonBridge2016@gmail.com/MB2016!!!", AccountType.Buyer);
        LoginVerificationService.readVerificationCodeFromGmail(buyer);
    }
}
