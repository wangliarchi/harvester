package edu.olivet.harvester.ui.panel;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMDocument;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Account.AccountType;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.letters.service.LetterTemplateService;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;

import javax.swing.*;
import java.awt.*;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 11:04 AM
 */
public class GmailWebPanel extends GeneralWebPanel {

    private Account gmailAccount;

    public GmailWebPanel(Account gmailAccount) {
        super(getKey(gmailAccount));
        this.gmailAccount = gmailAccount;
    }


    public static String getKey(Account gmailAccount) {
        return "Email-" + gmailAccount.getKey();
    }

    public void sendMessage(String recipient, String subject, String message) {
        Browser browser = getBrowserView().getBrowser();
        String url = String.format("https://mail.google.com/mail/u/0/?view=cm&fs=1&to=%s&su=%s&tf=1", recipient, subject);
        JXBrowserHelper.loadPage(browser, url);
        WaitTime.Short.execute();
        JXBrowserHelper.loginGoogleAccount(browser, gmailAccount.getEmail(), gmailAccount.getPassword());

        JXBrowserHelper.waitUntilVisible(browser, ".editable");
        DOMElement editor = JXBrowserHelper.selectElementByCssSelector(browser, "div.editable");

        String msg = message.replaceAll("\n", "<br/>");

        DOMDocument document = browser.getDocument();
        DOMElement bodyElement = document.createElement("div");
        bodyElement.setInnerHTML(msg);
        editor.appendChild(bodyElement);
        WaitTime.Short.execute();

        JXBrowserHelper.waitUntilVisible(browser, ".aoO");
        List<DOMElement> buttons = JXBrowserHelper.selectElementsByCssSelector(browser, "div.aoO");
        for (DOMElement button : buttons) {
            String text = JXBrowserHelper.textFromElement(button);
            if ("Send".equalsIgnoreCase(text)) {
                button.click();
                WaitTime.Shortest.execute();
                break;
            }
        }
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setTitle("Gmail Panel Demo");
        frame.setVisible(true);

        Account emailAccount = new Account("johnnyxiang2017@gmail.com/q1w2e3AA", AccountType.Email);

        GmailWebPanel webPanel = new GmailWebPanel(emailAccount);
        frame.getContentPane().add(webPanel);

        UITools.setDialogAttr(frame, true);

        Order order = new Order();
        order.row = 1;
        order.status = "sg";
        order.order_id = "002-1578027-1397838";
        order.recipient_name = "Nicholas Adamo";
        order.purchase_date = "10/24/2014 21:00:00";
        order.sku_address = "https://sellercentral.amazon.com/myi/search/OpenListingsSummary?keyword=new18140915a160118";
        order.sku = "new18140915a160118";
        order.price = "14.48";
        order.quantity_purchased = "1";
        order.shipping_fee = "16.95";
        order.ship_state = "NSW";
        order.isbn_address = "http://www.amazon.com/dp/0545521378";
        order.isbn = "0545521378";
        order.item_name = "Shoot the Moon by Billie Letts (2004-07-01) [Audio CD] [1662]";

        order.sales_chanel = "Amazon.com";
        order.spreadsheetId = Settings.load().listAllSpreadsheets().get(0);

        Letter letter = ApplicationContext.getBean(LetterTemplateService.class).getLetter(order);
        webPanel.sendMessage("johnnyxiang2015@gmail.com", letter.getSubject(), letter.getBody());

    }
}
