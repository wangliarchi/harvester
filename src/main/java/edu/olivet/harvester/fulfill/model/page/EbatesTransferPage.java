package edu.olivet.harvester.fulfill.model.page;

import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.service.OrderItemTypeHelper;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 9:35 AM
 */
public class EbatesTransferPage extends FulfillmentPage implements PageObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(EbatesTransferPage.class);
    private static final String TRANSFER_URL = "http://www.ebates.com/Amazon.com_1-xfas?navigation_id=19512&mpl_id=40301&sourceName=Web-Desktop";

    public EbatesTransferPage(BuyerPanel buyerPanel) {
        super(buyerPanel);
    }

    @Override
    public void execute(Order order) {
        //only work for amazon.com
        if (OrderCountryUtils.getFulfillmentCountry(order) != Country.US) {
            return;
        }

        //only for products, no cash back for books
        if (OrderItemTypeHelper.getItemTypeBySku(order) != OrderItemType.PRODUCT) {
            return;
        }

        Account ebatesBuyer = Settings.load().getConfigByCountry(OrderCountryUtils.getMarketplaceCountry(order)).getEbatesBuyer();

        if (ebatesBuyer == null || !ebatesBuyer.valid()) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            login(ebatesBuyer);

            //ebates is redirecting to amazon.com, wait...
            if (StringUtils.containsIgnoreCase(browser.getURL(), "ebates.com")) {
                JXBrowserHelper.waitUntilNotFound(browser, ".moment");
            }
        } catch (Exception e) {
            LOGGER.error("fail to log into ebates - ", e);
            throw new BusinessException(e);
        }


        if (!StringUtils.containsIgnoreCase(browser.getTitle(), "Amazon.")) {
            String msg = UIText.text("ebates.navigate.failed", Strings.formatElapsedTime(start), browser.getTitle());
            LOGGER.error(msg);
            throw new BusinessException(msg);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class)
    private void login(Account ebatesBuyer) {

        JXBrowserHelper.loadPage(browser, TRANSFER_URL);

        DOMElement emailElement = JXBrowserHelper.selectVisibleElement(browser, ".eb-auth-form .email-address");
        if (emailElement == null) {
            LOGGER.info("已经成功登陆过Ebates网站，无需再次重复登陆");
            return;
        }

        JXBrowserHelper.fillValueForFormField(browser, "form .email-address", ebatesBuyer.getEmail());
        JXBrowserHelper.fillValueForFormField(browser, "form .password", ebatesBuyer.getPassword());


        DOMElement submitBtn = JXBrowserHelper.selectElementByCssSelector(browser, "input[type=button]");
        if (submitBtn == null) {
            throw new BusinessException("No submit button found");
        }

        JXBrowserHelper.insertChecker(browser);
        submitBtn.click();
        LOGGER.info("输入Ebates用户名、密码，点击登陆动作完成");

        //check if error
        //error invalid
        DOMElement errorMsgElement = JXBrowserHelper.selectVisibleElement(browser, ".error.invalid");
        if (errorMsgElement != null) {
            throw new BusinessException(JXBrowserHelper.textFromElement(errorMsgElement));
        }

        JXBrowserHelper.waitUntilNewPageLoaded(browser);
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Ebates Demo");
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);

        //buyer = new Account("reviewsstudy@gmail.com/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);

        frame.getContentPane().add(buyerPanel);
        frame.setVisible(true);
        frame.setSize(new Dimension(1260, 736));
        EbatesTransferPage page = new EbatesTransferPage(buyerPanel);

        Order order = new Order();
        order.sales_chanel = "Amazon.com";
        page.execute(order);

    }
}
