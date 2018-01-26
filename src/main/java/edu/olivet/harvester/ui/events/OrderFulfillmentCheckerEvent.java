package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.By;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.model.page.checkout.PlacedOrderDetailPage;
import edu.olivet.harvester.fulfill.utils.validation.PreValidator;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.Actions;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.nutz.dao.Cnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class OrderFulfillmentCheckerEvent implements HarvesterUIEvent {

    @Inject
    private AppScript appScript;
    @Inject
    private OrderService orderService;
    @Inject
    DBManager dbManager;
    @Inject
    MessageListener messageListener;

    public void execute() {
        new Thread(() -> {
            //MessagePanel messagePanel = new ProgressDetail(Actions.OrderFulfillmentChecker);

            List<OrderFulfillmentRecord> list = dbManager.query(OrderFulfillmentRecord.class,
                    Cnd.where("quantityPurchased", ">", 1).desc("fulfillDate"));


            for (OrderFulfillmentRecord record : list) {

                //messageListener.addMsg("Start checking  order " + record.getOrderId() + " - " + record.getSheetName() + " - " + record.getOrderNumber());

                Account buyer;
                try {
                    buyer = BuyerAccountSettingUtils.load().getByEmail(record.getBuyerAccount()).getBuyerAccount();
                } catch (Exception e) {
                    messageListener.addMsg("No buyer for " + record.getBuyerAccount() + " found.", InformationLevel.Negative);
                    continue;
                }


                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(Country.US, buyer);
                Browser browser = buyerPanel.getBrowserView().getBrowser();
                LoginPage loginPage = new LoginPage(buyerPanel);
                loginPage.execute(null);

                String url = "https://www.amazon.com/gp/your-account/order-details/?orderID=" + record.getOrderNumber();
                JXBrowserHelper.loadPage(browser, url);

                PlacedOrderDetailPage placedOrderDetailPage = new PlacedOrderDetailPage(buyerPanel);
                try {
                    JXBrowserHelper.wait(browser, By.cssSelector("#orderDetails"));
                    Map<String, Integer> items = placedOrderDetailPage.parseItems();
                    int totalQty = items.values().stream().mapToInt(Number::intValue).sum();
                    if (totalQty < record.getQuantityBought()) {
                        messageListener.addMsg(String.format("%s \t %s \t %s \t %s \t %d \t %d \t %d",
                                record.getSheetName(), record.getOrderId(), record.getSku(), record.getOrderNumber(), record.getQuantityPurchased(), record.getQuantityBought(), totalQty),InformationLevel.Negative);
                    } else {
                        messageListener.addMsg(String.format("%s \t %s \t %s \t %s \t %d \t %d \t %d",
                                record.getSheetName(), record.getOrderId(), record.getSku(), record.getOrderNumber(), record.getQuantityPurchased(), record.getQuantityBought(), totalQty),InformationLevel.Positive);

                    }
                } catch (Exception e) {

                }
                //break;
            }
        }).start();
    }
}
