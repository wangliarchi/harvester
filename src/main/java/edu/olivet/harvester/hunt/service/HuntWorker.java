package edu.olivet.harvester.hunt.service;

import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntResult;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.utils.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static edu.olivet.harvester.hunt.model.HuntResult.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/27/2018 2:32 PM
 */
public class HuntWorker extends SwingWorker<Void, HuntResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuntWorker.class);

    private final HuntService huntService;
    private final List<Order> orders;
    private final SheetService sheetService;
    private final MessageListener messageListener;
    private final CountDownLatch latch;

    public HuntWorker(List<Order> orders, CountDownLatch latch) {
        this.huntService = ApplicationContext.getBean(HuntService.class);
        this.orders = orders;
        this.latch = latch;
        sheetService = ApplicationContext.getBean(SheetService.class);
        this.messageListener = ApplicationContext.getBean(MessageListener.class);
    }

    @Override
    protected Void doInBackground() throws Exception {
        for (Order order : orders) {
            if (PSEventListener.stopped()) {
                break;
            }

            Seller seller;
            try {
                seller = huntService.huntForOrder(order);
            } catch (Exception e) {
                LOGGER.error("", e);
                publish(new HuntResult(order, "Failed to find seller - " + e.getMessage(), ReturnCode.FAILURE));
                continue;
            }

            try {
                order.setSellerData(seller);
                sheetService.fillSellerInfo(order);
                publish(new HuntResult(order, "Find seller  - " + seller.toSimpleString(), ReturnCode.SUCCESS));
            } catch (Exception e) {
                LOGGER.error("", e);
                publish(new HuntResult(order, "Failed to write seller info to sheet - " + e.getMessage(), ReturnCode.SUCCESS));

            }
        }

        return null;
    }

    @Override
    protected void done() {
        latch.countDown();
    }

    @Override
    protected void process(final List<HuntResult> chunks) {
        for (HuntResult huntResult : chunks) {
            if (huntResult.getCode() == ReturnCode.SUCCESS) {
                messageListener.addMsg(huntResult.getOrder(), huntResult.getResult(), InformationLevel.Positive);
                ProgressUpdater.success();
            } else {
                messageListener.addMsg(huntResult.getOrder(), huntResult.getResult(), InformationLevel.Negative);
                ProgressUpdater.failed();
            }
        }
    }
}
