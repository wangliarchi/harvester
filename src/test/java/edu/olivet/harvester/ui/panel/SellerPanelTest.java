package edu.olivet.harvester.ui.panel;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class SellerPanelTest extends BaseTest {
    @Test
    public void addProduct() {


        BrowserView browserView = JXBrowserHelper.getGeneralBrowser();
        Browser browser = browserView.getBrowser();

        browser.loadHTML("<div class=\"actions\">\n" +
                "\t\t<button class=\"secondaryAUIButton \" type=\"button\" onclick=\"if (confirm('Are you sure you want to cancel?')) window.top.location.href='/abis/edit/AbandonEdit.amzn';\">Cancel</button>\n" +
                "\t\t<button id=\"main_submit_button\" disabled=\"\" class=\"primaryAUIButton \" type=\"submit\">Save and finish</button>\n" +
                "\t\t\t<img class=\"spinnerSubmit\" src=\"https://images-na.ssl-images-amazon.com/images/G/01/abis-ui/ajax-loader-orange.gif\" style=\"display: none;\">\n" +
                "\t\t\t<div class=\"view-toggle-button-bar\">\n" +
                "\t\t\t</div>\n" +
                "\t\t</div>");
        WaitTime.Short.execute();
        DOMElement submitBtn = JXBrowserHelper.selectElementByCssSelector(browser, "#main_submit_button");
        System.out.println(submitBtn.hasAttribute("disabled"));
        String pageUrl = "ManageInventory.html";
        browser.loadHTML(Tools.readFileToString(new File(TEST_DATA_ROOT + File.separator + "pages" + File.separator + pageUrl)));
        WaitTime.Short.execute();

        String freeShippingTemplateName = "FreeShipping";
        List<DOMElement> lists = JXBrowserHelper.selectElementsByCssSelector(browser, ".mt-table tr.mt-row");
        for (DOMElement trElement : lists) {
            //status
            String status = JXBrowserHelper.textFromElement(trElement, "div[data-column=\"status\"] a");
            String text = JXBrowserHelper.textFromElement(trElement);
            String priceText = JXBrowserHelper.getValueFromFormField(trElement, "div[data-column=\"price\"] input");
            Money money = Money.fromText(priceText, Country.FR);
        }
    }

}