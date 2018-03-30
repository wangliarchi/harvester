package edu.olivet.harvester.selforder;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.exception.Exceptions.BuyerAccountAuthenticationException;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.service.SelfOrderSheetService;
import edu.olivet.harvester.ui.panel.SellerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.utils.SellerPanelManager;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import edu.olivet.harvester.utils.common.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/28/2018 4:56 PM
 */
public class ProductManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductManager.class);
    @Inject SelfOrderSheetService sheetService;
    @Inject MessageListener messageListener;

    public void submit(List<SelfOrder> selfOrders) {
        if (CollectionUtils.isEmpty(selfOrders)) {
            UITools.error("No ASINs to process");
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

        List<String> addedASINs = new ArrayList<>();
        for (SelfOrder selfOrder : selfOrders) {
            if (PSEventListener.stopped()) {
                messageListener.addMsg("process stopped", InformationLevel.Negative);
                break;
            }

            if (addedASINs.contains(selfOrder.asin)) {
                messageListener.addMsg("Row " + selfOrder.row + " " + selfOrder.asin + " already added", InformationLevel.Negative);
                continue;
            }
            long start = System.currentTimeMillis();
            try {
                addProduct(selfOrder);
                messageListener.addMsg(selfOrder.row + " " + selfOrder.ownerAccountCode + " ASIN " + selfOrder.asin + " added for " + selfOrder.country);
                ProgressUpdater.success();
                addedASINs.add(selfOrder.asin);
            } catch (Exception e) {
                ProgressUpdater.failed();
                LOGGER.error("Error add asin {}", selfOrder.asin, e);
                String msg = Strings.getExceptionMsg(e);
                messageListener.addMsg("Row " + selfOrder.row + " " + msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);

                if (e instanceof BuyerAccountAuthenticationException) {
                    break;
                }
            }
        }

        PSEventListener.end();
    }

    public void addProduct(SelfOrder selfOrder) {
        Country country = Country.fromCode(selfOrder.country);
        Country settingCountry = country.europe() ? Country.UK : country;
        Configuration configuration = Settings.load().getConfigByCountry(settingCountry);
        try {

            Account seller = Settings.load().getConfigByCountry(settingCountry).getSeller();
            SellerPanel sellerPanel = SellerPanelManager.getTab(seller, settingCountry);
            sellerPanel.loginSellerCentral(country);
            sellerPanel.addProduct(selfOrder.asin, country);
            WaitTime.Short.execute();
        } catch (Exception e) {
            if (!Strings.containsAnyIgnoreCase(e.getMessage(), "template existed")) {
                throw e;
            }

        }

        selfOrder.ownerAccountSellerId = configuration.getMwsCredential().getSellerId();
        selfOrder.ownerAccountStoreName = configuration.getStoreName();
        sheetService.fillSellerId(selfOrder);
    }
}
