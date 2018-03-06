package edu.olivet.harvester.selforder;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.service.flowcontrol.OrderFlowEngine;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.service.OrderFulfillmentRecordService;
import edu.olivet.harvester.selforder.service.SelfOrderService;
import edu.olivet.harvester.selforder.service.SheetService;
import edu.olivet.harvester.selforder.utils.SelfOrderHelper;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/1/2018 8:48 AM
 */
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);

    @Inject OrderFlowEngine orderFlowEngine;
    @Inject SheetService sheetService;
    @Inject MessageListener messageListener;
    @Inject OrderFulfillmentRecordService orderFulfillmentRecordService;

    public void submit(List<SelfOrder> selfOrders) {
        if (CollectionUtils.isEmpty(selfOrders)) {
            UITools.error("No orders to process");
            return;
        }

        if (PSEventListener.isRunning()) {
            UITools.error("Other process is running, please submit when it's done.");
            return;
        }

        try {
            sheetService.updateUniqueCode(selfOrders.get(0).spreadsheetId, selfOrders);
        } catch (Exception e) {
            UITools.error(Strings.getExceptionMsg(e));
            return;
        }

        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
        ProgressUpdater.setTotal(selfOrders.size());
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.start();

        for (SelfOrder selfOrder : selfOrders) {
            if (PSEventListener.stopped()) {
                messageListener.addMsg("process stopped", InformationLevel.Negative);
                break;
            }
            long start = System.currentTimeMillis();
            try {
                submitSelfOrder(selfOrder);
                ProgressUpdater.success();
            } catch (Exception e) {
                ProgressUpdater.failed();
                LOGGER.error("Error submit order {}", selfOrder, e);
                String msg = Strings.parseErrorMsg(e.getMessage());
                messageListener.addMsg("Row " + selfOrder.row + " " + msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
                //
                try {
                    sheetService.fillFailedOrderInfo(selfOrder, msg);
                } catch (Exception e1) {
                    //
                }

                if (e instanceof BuyerAccountAuthenticationException) {
                    break;
                }
            }
        }

        PSEventListener.end();
    }

    public void submitSelfOrder(SelfOrder selfOrder) {

        if (!selfOrderIsValid(selfOrder)) {
            return;
        }

        Order order = SelfOrderHelper.convertToOrder(selfOrder);
        Country country = Country.fromCode(selfOrder.country);
        Account buyer = BuyerAccountSettingUtils.load().getByEmail(selfOrder.buyerAccountEmail).getBuyerAccount();
        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);

        Long start = System.currentTimeMillis();
        if (PSEventListener.stopped()) {
            buyerPanel.stop();
            messageListener.addMsg("Row " + order.row + " process stopped", InformationLevel.Negative);
            return;
        }

        while (PSEventListener.paused()) {
            buyerPanel.paused();
            WaitTime.Short.execute();
        }

        TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);

        try {
            orderFlowEngine.process(order, buyerPanel);

            if (StringUtils.isNotBlank(order.order_number)) {
                messageListener.addMsg("Row " + order.row + " fulfilled successfully. " + order.basicSuccessRecord() + ", took " + Strings.formatElapsedTime(start));
                //update sheet
                selfOrder.orderNumber = order.order_number;
                if (order.orderTotalCost != null) {
                    selfOrder.cost = order.orderTotalCost.toUSDAmount().toPlainString();
                } else {
                    selfOrder.cost = order.cost;
                }

                updateInfoToSheet(selfOrder);

                //save to log
                try {
                    orderFulfillmentRecordService.save(selfOrder);
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            } else {
                messageListener.addMsg("Row " + order.row + " submission failed. " + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
        }
    }

    public boolean selfOrderIsValid(SelfOrder selfOrder) {
        if (StringUtils.isEmpty(selfOrder.getAsin()) || !Regex.ASIN.isMatched(selfOrder.getAsin())) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as ASIN is not found.", InformationLevel.Negative);
            return false;
        }

        if (StringUtils.isBlank(selfOrder.ownerAccountStoreName) && StringUtils.isBlank(selfOrder.ownerAccountSellerId)) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as both store name and id are not found.", InformationLevel.Negative);
            return false;
        }

        if (StringUtils.isEmpty(selfOrder.promoCode)) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as promo code is not found.", InformationLevel.Negative);
            return false;
        }

        if (StringUtils.isEmpty(selfOrder.country)) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as sales channel is not found.", InformationLevel.Negative);
            return false;
        }

        try {
            Country.fromCode(selfOrder.country);
        } catch (Exception e) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as country is not valid.", InformationLevel.Negative);
            return false;
        }


        if (StringUtils.isEmpty(selfOrder.buyerAccountEmail)) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as buyer account is not found.", InformationLevel.Negative);
            return false;
        }

        try {
            BuyerAccountSettingUtils.load().getByEmail(selfOrder.buyerAccountEmail).getBuyerAccount();
        } catch (Exception e) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as buyer account " + selfOrder.buyerAccountEmail + " is not found.", InformationLevel.Negative);
            return false;
        }

        return true;
    }

    public void updateInfoToSheet(SelfOrder order) {
        try {
            sheetService.fillFulfillmentOrderInfo(order);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    public static void main(String[] args) {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        SelfOrderService selfOrderService = ApplicationContext.getBean(SelfOrderService.class);
        List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, "03/01");
        SelfOrder selfOrder = selfOrders.get(0);
        selfOrder.buyerAccountEmail = "olivetrnd153.2@gmail.com";

        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setTitle("Seller Panel Demo");
        frame.setVisible(true);
        Settings settings = Settings.load();
        BuyerPanel buyerPanel = new BuyerPanel(1, Country.US, settings.getConfigByCountry(Country.US).getBuyer(), -1.3);
        frame.getContentPane().add(buyerPanel);

        UITools.setDialogAttr(frame, true);

        OrderSubmitter orderSubmitter = ApplicationContext.getBean(OrderSubmitter.class);
        orderSubmitter.submitSelfOrder(selfOrder);
    }
}
