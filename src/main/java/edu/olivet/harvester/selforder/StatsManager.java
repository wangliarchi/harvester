package edu.olivet.harvester.selforder;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.fulfill.exception.Exceptions.BuyerAccountAuthenticationException;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.model.SelfOrderRecord;
import edu.olivet.harvester.selforder.service.SelfOrderRecordService;
import edu.olivet.harvester.selforder.service.SelfOrderService;
import edu.olivet.harvester.selforder.service.SelfOrderSheetService;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.Strings;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/1/2018 8:48 AM
 */
public class StatsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsManager.class);

    @Inject SelfOrderSheetService sheetService;
    @Inject SelfOrderService selfOrderService;
    @Inject SelfOrderRecordService selfOrderRecordService;

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();
    @Inject MessageListener messageListener;

    public void postFeedbacks() {

        if (PSEventListener.isRunning()) {
            UITools.error("Other process is running, please submit when it's done.");
            return;
        }

        List<SelfOrderRecord> records = selfOrderRecordService.getRecordToPostFeedbacks();
        if (CollectionUtils.isEmpty(records)) {
            UITools.error("No feedbacks to post.");
            return;
        }

        if (!UITools.confirmed(records.size() + " feedback(s) to be post, please confirm to proceed.")) {
            return;
        }
        try {
            selfOrderRecordService.updateUniqueCode(records.get(0).spreadsheetId, records);
        } catch (Exception e) {
            UITools.error(Strings.getExceptionMsg(e));
            return;
        }

        ProgressUpdater.setProgressBarComponent(SimpleOrderSubmissionRuntimePanel.getInstance());
        ProgressUpdater.setTotal(records.size());
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.start();

        for (SelfOrderRecord record : records) {
            if (PSEventListener.stopped()) {
                messageListener.addMsg("process stopped", InformationLevel.Negative);
                break;
            }
            long start = System.currentTimeMillis();
            try {
                postFeedback(record);
                ProgressUpdater.success();
            } catch (Exception e) {
                //
                ProgressUpdater.failed();
                LOGGER.error("Error post feedback {}", record, e);
                String msg = Strings.getExceptionMsg(e);
                messageListener.addMsg("Row " + record.row + " " + msg + " - took " + Strings.formatElapsedTime(start), InformationLevel.Negative);
                //
                try {
                    selfOrderRecordService.fillFailedOrderInfo(record, msg);
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

    public void postFeedback(SelfOrderRecord record) {
        Country country = Country.fromCode(record.country);
        Account buyer;
        try {
            buyer = BuyerAccountSettingUtils.load().getByEmail(record.buyerAccountEmail).getBuyerAccount();
        } catch (Exception e) {
            throw new BusinessException("No buyer for " + record.buyerAccountEmail + " found");
        }

        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);
        Long start = System.currentTimeMillis();
        if (PSEventListener.stopped()) {
            buyerPanel.stop();
            messageListener.addMsg("Row " + record.row + " process stopped", InformationLevel.Negative);
            TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
            return;
        }

        buyerPanel.postFeedback(record.orderNumber, record.feedback);
        messageListener.addMsg("Row " + record.row + " " + record.orderNumber + " feedback posted - took " + Strings.formatElapsedTime(start), InformationLevel.Positive);

        try {
            selfOrderRecordService.fillSuccessInfo(record);
        } catch (Exception e1) {
            //
        }
        TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
    }

    public void asyncSelfOrderStats() {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        String statsSpreadsheetId = SystemSettings.reload().getSelfOrderStatsSpreadsheetId();
        if (StringUtils.isBlank(spreadsheetId) || StringUtils.isBlank(statsSpreadsheetId)) {
            UITools.error("Self order spreadsheet id not entered. Please config under Settings->System Settings->Self-Orders");
            return;
        }

        Date minDate = DateUtils.addDays(new Date(), -7);
        List<SelfOrder> orders = selfOrderService.fetchSelfOrders(spreadsheetId, minDate);
        orders.removeIf(it -> StringUtils.isBlank(it.orderNumber) || Regex.AMAZON_ORDER_NUMBER.isMatched(it.orderNumber));
        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No self orders placed.");
            return;
        }

        Settings settings = Settings.load();
        List<Country> countries = Settings.load().listAllCountries();
        for (Country country : countries) {
            messagePanel.addMsgSeparator();
            List<SelfOrder> ordersForCountry = orders.stream().filter(it -> it.country.equalsIgnoreCase(country.name())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ordersForCountry)) {
                messagePanel.displayMsg("No self orders placed for " + country);
                continue;
            }
            String sid = settings.getConfigByCountry(country).getAccountCode();
            List<String> existedOrderNumbers = selfOrderRecordService.existedOrderNumbers(sid);
            ordersForCountry.removeIf(it -> existedOrderNumbers.contains(it.orderNumber));

            if (CollectionUtils.isEmpty(ordersForCountry)) {
                messagePanel.displayMsg("No self orders to be synced for " + country);
                continue;
            }

            messagePanel.displayMsg(ordersForCountry.size() + " self orders to be synced for " + country);

            try {
                sheetService.fillStats(sid, ordersForCountry);
                messagePanel.displayMsg("Done");
            } catch (Exception e) {
                LOGGER.error("", e);
                messagePanel.displayMsg("Fail to sync order data for " + countries + ". Reason: " + Strings.getExceptionMsg(e));
            }
        }
    }
}
