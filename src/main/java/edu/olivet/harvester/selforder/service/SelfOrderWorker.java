package edu.olivet.harvester.selforder.service;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.BuyerAccountAuthenticationException;
import edu.olivet.harvester.fulfill.model.SubmitResult;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.fulfill.service.flowcontrol.OrderFlowEngine;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.utils.SelfOrderHelper;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.common.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/16/2018 3:00 PM
 */
public class SelfOrderWorker extends SwingWorker<Void, SubmitResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelfOrderWorker.class);
    private final MessageListener messageListener;
    private final OrderFlowEngine orderFlowEngine;

    private List<SelfOrder> orders;
    private final SelfOrderSheetService sheetService;
    private final OrderFulfillmentRecordService orderFulfillmentRecordService;
    private final CountDownLatch latch;

    public SelfOrderWorker(List<SelfOrder> orders, CountDownLatch latch) {
        super();
        messageListener = ApplicationContext.getBean(MessageListener.class);
        orderFlowEngine = ApplicationContext.getBean(OrderFlowEngine.class);
        sheetService = ApplicationContext.getBean(SelfOrderSheetService.class);
        orderFulfillmentRecordService = ApplicationContext.getBean(OrderFulfillmentRecordService.class);

        this.latch = latch;
        this.orders = orders;
    }

    @Override
    protected void done() {
        latch.countDown();
    }

    @Override
    protected Void doInBackground() throws Exception {
        for (SelfOrder selfOrder : orders) {
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
                String msg = Strings.getExceptionMsg(e);
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

        return null;
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
        } finally {
            TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
        }
    }

    public void updateInfoToSheet(SelfOrder order) {
        try {
            sheetService.fillFulfillmentOrderInfo(order);
        } catch (Exception e) {
            LOGGER.error("", e);
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

        if (StringUtils.startsWithIgnoreCase(selfOrder.ownerAccountSellerId, selfOrder.buyerAccountCode)) {
            messageListener.addMsg("Row " + selfOrder.row + " is invalid as it's on same account", InformationLevel.Negative);
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
}
