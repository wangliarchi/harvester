package edu.olivet.harvester.ui;

import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.teamdev.jxbrowser.chromium.dom.DOMFormControlElement;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import com.teamdev.jxbrowser.chromium.swing.internal.LightWeightWidget;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Account.AccountType;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.model.Order;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Order fulfillment browser frame
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
class BrowserFrame extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserFrame.class);
    @Getter private final Account buyer;
    @Getter private final Browser browser;
    private final BrowserView browserView;

    BrowserFrame(Account buyer) {
        final long start = System.currentTimeMillis();
        this.buyer = buyer;
        UITools.setIcon(this, "chrome.png");

        BrowserContext context = new BrowserContext(new BrowserContextParams(Directory.Tmp.path() + File.separator + buyer.getEmail()));
        this.browser = new Browser(BrowserType.LIGHTWEIGHT, context);
        this.browser.setZoomEnabled(true);
        this.browserView = new BrowserView(browser);

        BrowserPreferences preferences = browser.getPreferences();
        preferences.setImagesEnabled(true);
        preferences.setJavaScriptEnabled(true);
        browser.setPreferences(preferences);
        this.add(browserView, BorderLayout.CENTER);
        this.setSize(480, 720);
        this.setVisible(true);
        this.setTitle("Progress of orders fulfilled by " + buyer.getEmail());
        LOGGER.info("Browser frame of {} initialized in {}.", buyer.key(), Strings.formatElapsedTime(start));
    }

    void submit(Order order, Account buyer) {
        Country country = Country.fromSalesChanel(order.sales_chanel);
        String offerUrl = country.baseUrl() + "/gp/offer-listing/" + Strings.fillMissingZero(order.isbn) +
            "/ref=olp_f_" + order.condition.split("-")[0].trim().toLowerCase() + "?ie=UTF8&f_new=true&qid=1503435533&sr=8-1" +
            "&seller=" + order.seller_id;
        // Open offering list page
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(offerUrl));

        // Check whether there is offer existing or disappeared, or even the listing was deleted
        if (browser.getDocument().findElement(By.name("submit.addToCart")) == null) {
            throw new IllegalStateException("Cannot add expected offer to shopping cart.");
        }

        // Add to cart
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.getDocument().findElement(By.name("submit.addToCart")).click());
        // Proceed to checkout and prepare for login
        Browser.invokeAndWaitFinishLoadingMainFrame(browser,
            it -> it.getDocument().findElement(By.cssSelector(".sc-ptc-agent button, #hlb-ptc-btn-native")).click());
        // Check whether login is needed or not
        DOMFormControlElement email = (DOMFormControlElement) browser.getDocument().findElement(By.name("email"));
        if (email != null) {
            email.setValue(buyer.getEmail());
            ((DOMFormControlElement) browser.getDocument().findElement(By.name("password"))).setValue(buyer.getPassword());
            Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.getDocument().findElement(By.id("signInSubmit")).click());
        } else {
            LOGGER.info("Buyer {} login already.", buyer.getEmail());
        }
        Document doc = Jsoup.parse(browser.getDocument().getDocumentElement().getInnerHTML());
        LOGGER.info("Page title after login: {} VS {}", browser.getTitle(), doc.title());

        // Switch to address selection page explicitly if needed
        String url = browser.getURL();
        if (url.contains("gp/buy/spc/handlers") && browser.getDocument().findElement(By.name("placeYourOrder1")) != null) {
            Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> {
                String addressUrl = country.baseUrl() + "/gp/buy/addressselect/handlers/display.html?hasWorkingJavascript=1";
                it.loadURL(addressUrl);
            });
        }

        browser.getDocument().findElement(By.cssSelector("a#add-new-address-popover-link, a[href='#new-address'], a[data-pipeline-link-to-page='newaddress']")).click();
        WaitTime.Shorter.execute();
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressFullName"))).setValue(order.recipient_name);
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressAddressLine1"))).setValue(order.ship_address_1);
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressAddressLine2"))).setValue(order.ship_address_2);
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressCity"))).setValue(order.ship_city);
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressStateOrRegion"))).setValue(order.ship_state);
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressPostalCode"))).setValue(order.ship_zip);
        ((DOMFormControlElement) browser.getDocument().findElement(By.id("enterAddressPhoneNumber"))).setValue(order.ship_phone_number);
        // Submit form rather than click submit button which can have various kinds of selectors
        Browser.invokeAndWaitFinishLoadingMainFrame(browser,
            it -> ((DOMFormControlElement) it.getDocument().findElement(By.id("enterAddressPhoneNumber"))).getForm().submit());

        for (int j = 0; j < 3; j++) {
            DOMElement button = browser.getDocument().findElement(By.cssSelector("#continue-top, #continueButton"));
            if (button != null) {
                button.click();
                break;
            } else {
                LOGGER.warn("Payment button is not available currently, we will wait for a little while.");
                WaitTime.Shorter.execute();
            }
        }

        WaitTime.Short.execute();
        saveScreenshots(Directory.WebPage.path() + "/" + order.row + "_" + order.order_id + ".png");
    }

    private void saveScreenshots(String filePath) {
        LightWeightWidget lightWeightWidget = (LightWeightWidget) this.browserView.getComponent(0);
        Image image = lightWeightWidget.getImage();
        try {
            File file = new File(filePath);
            Tools.createFileIfNotExist(file);
            ImageIO.write((RenderedImage) image, "PNG", file);
        } catch (IOException e) {
            LOGGER.error("Failed to save screenshots of current page to {}:", filePath, e);
        }
    }

    void clearShoppingCart() {
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL("https://www.amazon.com/gp/cart/view.html/ref=nav_cart"));
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            By by = By.cssSelector(".sc-action-delete input[name*='submit.delete']");
            List<DOMElement> elements = browser.getDocument().findElements(by);
            if (CollectionUtils.isEmpty(elements)) {
                break;
            }
            elements.forEach(it -> {
                it.click();
                WaitTime.Shorter.execute();
            });
        }
    }

    void loginMobilePage() {
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(Country.US.baseUrl() + "/mobile"));
        DOMElement element = browser.getDocument().findElement(By.id("smart-app-banner-close"));
        if (element != null) {
            element.click();
        }

        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(Country.US.baseUrl() +
            "/gp/primecentral?ie=UTF8&ref_=ya_manage_prime&"));
        DOMElement email = browser.getDocument().findElement(By.name("email"));
        if (email == null) {
            LOGGER.info("{}: {}", browser.getTitle(), browser.getURL());
        } else {
            ((DOMFormControlElement) email).setValue(buyer.getEmail());
            ((DOMFormControlElement) browser.getDocument().findElement(By.name("password"))).setValue(buyer.getPassword());
            browser.getDocument().findElement(By.name("rememberMe")).click();
            Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> ((DOMFormControlElement) email).getForm().submit());

            LOGGER.info("After login: '{} -> {}'", browser.getTitle(), browser.getURL());
        }
    }

    public static void main(String[] args) {
        BrowserFrame dialog = new BrowserFrame(new Account(args[0], AccountType.Buyer));
        UITools.setDialogAttr(dialog, true);
        dialog.loginMobilePage();
    }
}
