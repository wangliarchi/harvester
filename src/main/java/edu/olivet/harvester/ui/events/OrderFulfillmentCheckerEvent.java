package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.By;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.model.page.checkout.PlacedOrderDetailPage;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import org.nutz.dao.Cnd;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 10:59 AM
 */
public class OrderFulfillmentCheckerEvent implements HarvesterUIEvent {

    @Inject
    DBManager dbManager;
    @Inject
    MessageListener messageListener;

    public void execute() {
        new Thread(() -> {
            //MessagePanel messagePanel = new ProgressDetail(Actions.OrderFulfillmentChecker);

            List<OrderFulfillmentRecord> list = dbManager.query(OrderFulfillmentRecord.class,
                    Cnd.where("quantityPurchased", ">", 1).desc("fulfillDate"));

            messageListener.empty();
            ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
            ProgressUpdater.updateTotal(list.size());
            SimpleOrderSubmissionRuntimePanel.getInstance().showProgressBar();
            for (OrderFulfillmentRecord record : list) {

                //messageListener.addMsg("Start checking  order " + record.getOrderId() + " - " + record.getSheetName() + " - " + record.getOrderNumber());

                Account buyer;
                try {
                    buyer = BuyerAccountSettingUtils.load().getByEmail(record.getBuyerAccount()).getBuyerAccount();
                } catch (Exception e) {
                    messageListener.addMsg("No buyer for " + record.getBuyerAccount() + " found.", InformationLevel.Negative);
                    ProgressUpdater.failed();
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
                        String c = "";
                        try {
                            Country country = Settings.load().getSpreadsheetCountry(record.getSpreadsheetId());
                            OrderItemType itemType = Settings.load().getSpreadsheetType(record.getSpreadsheetId());
                            c = country.name() + "\t" + itemType.name();
                        } catch (Exception e) {
                            //
                        }

                        messageListener.addMsg(String.format("%s \t %s \t %s \t %s \t %s \t %d \t %d \t %d \t %s",
                                c, record.getSheetName(), record.getOrderId(), record.getSku(), record.getOrderNumber(), record.getQuantityPurchased(), record.getQuantityBought(), totalQty, record.getBuyerAccount()), InformationLevel.Negative);
                        ProgressUpdater.failed();
                    } else {
                        messageListener.addMsg(String.format("%s \t %s \t %s \t %s \t %d \t %d \t %d",
                                record.getSheetName(), record.getOrderId(), record.getSku(), record.getOrderNumber(), record.getQuantityPurchased(), record.getQuantityBought(), totalQty), InformationLevel.Positive);
                        ProgressUpdater.success();
                    }
                } catch (Exception e) {
                    messageListener.addMsg(String.format("%s \t %s \t %s \t %s \t %d \t %d \t %d",
                            record.getSheetName(), record.getOrderId(), record.getSku(), record.getOrderNumber(), record.getQuantityPurchased(), record.getQuantityBought(), 0), InformationLevel.Negative);
                    ProgressUpdater.failed();
                }
                //break;
            }
        }).start();
    }
}
