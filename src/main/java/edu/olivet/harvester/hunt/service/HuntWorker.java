package edu.olivet.harvester.hunt.service;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntResult;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
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
    private final MessagePanel messageListener;
    private final CountDownLatch latch;

    public HuntWorker(List<Order> orders, CountDownLatch latch, MessagePanel messageListener) {
        this.huntService = ApplicationContext.getBean(HuntService.class);
        this.orders = orders;
        this.latch = latch;
        sheetService = ApplicationContext.getBean(SheetService.class);
        this.messageListener = messageListener;
    }

    @Override
    protected Void doInBackground() throws Exception {
        for (Order order : orders) {
            if (PSEventListener.stopped()) {
                break;
            }

            try {
                huntForOrder(order);
            } catch (Exception e) {
                LOGGER.error("", e);
                publish(new HuntResult(order, "failed to find seller - " + Strings.getExceptionMsg(e), ReturnCode.FAILURE));
            }
        }

        return null;
    }


    public void huntForOrder(Order order) {
        //find seller
        Seller seller;
        try {
            seller = huntService.huntForOrder(order);
        } catch (Exception e) {
            LOGGER.error("", e);
            publish(new HuntResult(order, "failed to find seller - " + Strings.getExceptionMsg(e), ReturnCode.FAILURE));
            return;
        }

        try {
            SellerHuntUtils.setSellerDataForOrder(order, seller);
            sheetService.fillSellerInfo(order);
            publish(new HuntResult(order, "find seller  - " + seller.toSimpleString(), ReturnCode.SUCCESS));
        } catch (Exception e) {
            LOGGER.error("", e);
            publish(new HuntResult(order, "failed to write seller info to sheet - " + Strings.getExceptionMsg(e), ReturnCode.SUCCESS));
        }
    }

    @Override
    protected void done() {
        latch.countDown();
    }

    @Override
    protected void process(final List<HuntResult> chunks) {
        for (HuntResult huntResult : chunks) {
            Country country = OrderCountryUtils.getMarketplaceCountry(huntResult.getOrder());
            String msg = String.format("%s %s %s row %d - %s %s %s",
                    country != null ? (country.europe() ? "EU" : country.name()) : huntResult.getOrder().getSpreadsheetId(), huntResult.getOrder().type().name().toLowerCase(),
                    huntResult.getOrder().sheetName, huntResult.getOrder().row, huntResult.getOrder().order_id, huntResult.getResult(), ProgressUpdater.timeSpent());
            if (huntResult.getCode() == ReturnCode.SUCCESS) {
                ProgressUpdater.success();
                messageListener.displayMsg(ProgressUpdater.progress() + " - " + msg);
            } else {
                ProgressUpdater.failed();
                messageListener.displayMsg(ProgressUpdater.progress() + " - " + msg, InformationLevel.Negative);
            }
        }
    }
}
