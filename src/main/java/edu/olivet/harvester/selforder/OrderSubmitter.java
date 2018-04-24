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
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.service.OrderFulfillmentRecordService;
import edu.olivet.harvester.selforder.service.SelfOrderService;
import edu.olivet.harvester.selforder.service.SelfOrderService.OrderAction;
import edu.olivet.harvester.selforder.service.SelfOrderSheetService;
import edu.olivet.harvester.selforder.service.SelfOrderWorker;
import edu.olivet.harvester.selforder.utils.SelfOrderHelper;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nutz.aop.interceptor.async.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/1/2018 8:48 AM
 */
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);

    @Inject SelfOrderSheetService sheetService;

    private ExecutorService threadPool;

    @Inject
    public void init() {
        threadPool = Executors.newFixedThreadPool(SystemSettings.reload().getMaxOrderProcessingThread());
    }

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

        Map<String, List<SelfOrder>> ordersByBuyer = new HashMap<>();
        for (SelfOrder selfOrder : selfOrders) {
            List<SelfOrder> s = ordersByBuyer.getOrDefault(selfOrder.buyerAccountEmail, new ArrayList<>());
            s.add(selfOrder);
            ordersByBuyer.put(selfOrder.buyerAccountEmail, s);
        }

        final CountDownLatch latch = new CountDownLatch(selfOrders.size());
        ordersByBuyer.forEach((buyerEmail, orders) -> {
            SelfOrderWorker job = new SelfOrderWorker(orders, latch);
            threadPool.submit(job);
        });

        // 负责监视是否所有找线程都完成的线程。
        new SwingWorker<Void, String>() {
            @Override
            @Async
            protected Void doInBackground() throws Exception {
                latch.await();
                return null;
            }

            @Override
            @Async
            protected void done() {
                PSEventListener.end();
            }
        }.execute();


    }


    public static void main(String[] args) {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        SelfOrderService selfOrderService = ApplicationContext.getBean(SelfOrderService.class);
        List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, "03/01", OrderAction.Process);
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

        //OrderSubmitter orderSubmitter = ApplicationContext.getBean(OrderSubmitter.class);
        //orderSubmitter.submitSelfOrder(selfOrder);
    }
}
