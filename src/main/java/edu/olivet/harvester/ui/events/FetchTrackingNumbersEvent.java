package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import javafx.scene.control.Tab;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/13/2018 1:14 PM
 */
public class FetchTrackingNumbersEvent implements HarvesterUIEvent {
    @Inject AppScript appScript;
    @Inject SheetAPI sheetAPI;
    @Inject private OrderService orderService;
    @Inject SheetService sheetService;

    @Override
    public void execute() {
        if (PSEventListener.isRunning()) {
            UITools.error("Other task is running...");
            return;
        }
        List<Spreadsheet> spreadsheets = new ArrayList<>();
        Settings settings = Settings.load();
        for (Country country : settings.listAllCountries()) {
            spreadsheets.addAll(settings.listSpreadsheetsForOrderSubmission(country, appScript, sheetAPI));
        }

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));

        if (dialog.isOk()) {
            MessagePanel messagePanel = new ProgressDetail(Actions.FetchTrackingNumbers);
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            String spreadsheetId = selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetId();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());

            messagePanel.displayMsg("Start to check titles for sheets " + sheetNames + " from " + selectedWorksheets.get(0).getSpreadsheet().getTitle());
            List<Order> orders = orderService.fetchOrders(spreadsheetId, sheetNames);
            orders.removeIf(order -> order.selfBuy() || order.colorIsGray() || !order.fulfilled() || StringUtils.isNotBlank(order.tracking_number));

            if (CollectionUtils.isEmpty(orders)) {
                messagePanel.displayMsg("No orders found to be processed.");
                return;
            }
            messagePanel.displayMsg(orders.size() + " orders found to be processed.");

            PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
            PSEventListener.start();
            for (Order order : orders) {
                Account buyer;
                try {
                    buyer = BuyerAccountSettingUtils.load().getByEmail(order.account).getBuyerAccount();
                } catch (Exception e) {
                    messagePanel.displayMsg("No buyer for " + order.account + " found.", InformationLevel.Negative);
                    ProgressUpdater.failed();
                    continue;
                }

                Country country = OrderCountryUtils.getFulfillmentCountry(order);
                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
                TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);

                try {
                    Browser browser = buyerPanel.getBrowserView().getBrowser();
                    String url = "https://www.amazon.com/gp/your-account/order-details/?orderID=" + order.order_number;
                    JXBrowserHelper.loadPage(browser, url);

                    for (int i = 0; i < 3; i++) {
                        if (LoginPage.needLoggedIn(browser)) {
                            LoginPage loginPage = new LoginPage(buyerPanel);
                            loginPage.login();
                            WaitTime.Short.execute();
                        } else {
                            JXBrowserHelper.loadPage(browser, url);
                            break;
                        }
                    }

                    if (LoginPage.needLoggedIn(browser)) {
                        messagePanel.displayMsg("Fail to log in buyer account " + order.buyer_email, InformationLevel.Negative);
                        break;
                    }


                    WaitTime.Shortest.execute();

                    DOMElement orderDetails = JXBrowserHelper.selectVisibleElement(browser, "#orderDetails");
                    if (orderDetails == null) {
                        messagePanel.displayMsg(order.row + " " + order.order_number + " not found.", InformationLevel.Negative);
                        continue;
                    }
                    DOMElement trackLink = JXBrowserHelper.selectVisibleElement(browser, ".a-button.track-package-button a");
                    if (trackLink == null) {
                        messagePanel.displayMsg(order.row + " " + order.order_number + " no tracking found.", InformationLevel.Negative);
                        continue;
                    }

                    JXBrowserHelper.insertChecker(browser);
                    trackLink.click();
                    JXBrowserHelper.waitUntilNewPageLoaded(browser);
                    WaitTime.Shortest.execute();
                    JXBrowserHelper.waitUntilVisible(browser, "#primaryStatus");

                    String carrier = JXBrowserHelper.textFromElement(browser, "#carrierRelatedInfo-container .carrierRelatedInfo-mfn-carrierNameTitle,#carrierRelatedInfo-container .widgetHeader");
                    carrier = carrier.replace("Shipped with ", "");
                    order.carrier = carrier;

                    String tracking = JXBrowserHelper.textFromElement(browser, "#carrierRelatedInfo-container .carrierRelatedInfo-trackingId-text");
                    tracking = tracking.replace("Tracking ID ", "");
                    order.tracking_number = tracking;

                    order.seller_estimated_delivery_date = JXBrowserHelper.textFromElement(browser, "#primaryStatus");
                    //Shipped with USPS

                    if (StringUtils.isNotBlank(order.carrier)) {
                        sheetService.fillTrackingInfo(order.spreadsheetId, order);
                    }
                    messagePanel.displayMsg(order.order_number + ": " + order.carrier + " " + order.tracking_number + " " + order.seller_estimated_delivery_date);
                    WaitTime.Shortest.execute();
                } catch (Exception e) {
                    //
                } finally {
                    TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
                }

            }

            PSEventListener.end();
        }
    }
}
