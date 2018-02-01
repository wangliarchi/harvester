package edu.olivet.harvester.hunt;


import com.google.inject.Inject;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntResult;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.HuntService;
import edu.olivet.harvester.hunt.service.HuntWorker;
import edu.olivet.harvester.hunt.service.SheetService;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.common.ThreadHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nutz.aop.interceptor.async.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/19/2018 2:50 PM
 */
public class Hunter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Hunter.class);

    @Inject AppScript appScript;
    @Inject private MessageListener messageListener;

    private final static int HUNTER_JOB_NUMBER = 2;

    public void execute(RuntimeSettings runtimeSettings) {
        List<Order> orders = appScript.readOrders(runtimeSettings);

        if (CollectionUtils.isEmpty(orders)) {
            messageListener.addMsg("No orders found for  " + runtimeSettings.getAdvancedSubmitSetting().toString(), InformationLevel.Negative);
            return;
        }

        huntForOrders(orders);
    }

    public void huntForWorksheets(List<Worksheet> worksheets) {
        for (Worksheet worksheet : worksheets) {
            try {
                huntForWorksheet(worksheet);
            } catch (Exception e) {
                LOGGER.error("Error when hunting sellers for {} - ", worksheet, e);
            }
        }
    }

    public void huntForWorksheet(Worksheet worksheet) {
        List<Order> orders = appScript.readOrders(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName());

        if (CollectionUtils.isEmpty(orders)) {
            messageListener.addMsg("No orders found for worksheet " + worksheet, InformationLevel.Negative);
            return;
        }

        huntForOrders(orders);

    }

    public void huntForOrders(List<Order> orders) {
        messageListener.empty();

        //remove invalid orders
        orders.removeIf(order -> order.sellerHunted() || order.colorIsGray() || order.buyerCanceled());

        if (CollectionUtils.isEmpty(orders)) {
            messageListener.addMsg("No orders to hunt", InformationLevel.Negative);
            return;
        }

        ProgressUpdater.updateTotal(orders.size());
        PSEventListener.start();

        // 定长swingworker list
        List<HuntWorker> jobs = new ArrayList<>(HUNTER_JOB_NUMBER);

        // 把所有order按照线程数发到几个list里面。
        List<List<Order>> list = ThreadHelper.assign(orders, HUNTER_JOB_NUMBER);

        final CountDownLatch latch = new CountDownLatch(HUNTER_JOB_NUMBER);

        // 把order list分配给几个SwingWorker
        for (List<Order> assignedOrders : list) {
            jobs.add(new HuntWorker(assignedOrders, latch));
        }

        // SwingWorker线程执行
        for (HuntWorker job : jobs) {
            job.execute();
        }


        // 负责监视是否所有找seller线程都完成的线程。
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

}
