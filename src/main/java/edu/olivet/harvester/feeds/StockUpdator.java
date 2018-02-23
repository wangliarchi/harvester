package edu.olivet.harvester.feeds;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.feeds.helper.ConfirmShipmentEmailSender;
import edu.olivet.harvester.feeds.helper.FeedGenerator;
import edu.olivet.harvester.feeds.helper.FeedGenerator.BatchFileType;
import edu.olivet.harvester.feeds.helper.FeedSubmissionEmailSender;
import edu.olivet.harvester.feeds.helper.InventoryUpdateTypeHelper;
import edu.olivet.harvester.feeds.helper.InventoryUpdateTypeHelper.UpdateType;
import edu.olivet.harvester.feeds.model.InventoryUpdateRecord;
import edu.olivet.harvester.feeds.service.*;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.stream.Collectors;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 11:09 AM
 */
public class StockUpdator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockUpdator.class);
    @Getter
    private MessagePanel messagePanel = new VirtualMessagePanel();
    @Inject private SheetAPI sheetAPI;
    @Inject private OrderService orderService;

    @Inject
    private OrderInventoryUpdateLogService orderInventoryUpdateLogService;

    @Inject private InventoryReportManager inventoryReportManager;
    @Inject FeedUploadService feedUploadService;
    @Inject AsinAppScript asinAppScript;
    /**
     * max days backward based on today to locate all possible sheets of given spreadsheet
     */
    private static final int MAX_DAYS = 7;


    /**
     * confirm shipments by worksheet. Errors handled by each confirmShipmentForWorksheet call separately.
     */
    public void asyncASINsForWorksheets(List<Worksheet> worksheets) {
        List<Order> orders = orderService.fetchOrders(worksheets);
        messagePanel.displayMsg(orders.size() + " order(s) found.");
        processForOrders(orders, worksheets.get(0).getSpreadsheet().getTitle(), worksheets.get(0).getSpreadsheet().getSpreadsheetId());
    }

    public void execute() {
        //this method is for cronjob, keep silent.
        setMessagePanel(new VirtualMessagePanel());

        //get all order update sheets for account
        List<String> spreadIds;
        try {
            spreadIds = Settings.load().listAllSpreadsheets();
        } catch (BusinessException e) {
            LOGGER.error("No configuration file found. {}", e);
            throw new BusinessException("No configuration file found.");
        }

        //then confirm shipment for each spreadsheet
        for (String spreadId : spreadIds) {
            try {
                asyncASINsForSpreadsheetId(spreadId);
            } catch (Exception e) {
                LOGGER.error("Error when confirm shipment for sheet {} {}", spreadId, e);
            }
        }
    }

    private void asyncASINsForSpreadsheetId(String spreadsheetId) {
        //set spreadsheet id, check if the given spreadsheet id is valid
        Spreadsheet workingSpreadsheet;
        try {
            workingSpreadsheet = sheetAPI.getSpreadsheet(spreadsheetId);
        } catch (Exception e) {
            messagePanel.displayMsg(String.format("No google spreadsheet found for %s. Please make sure the correct order update google sheet id is entered.",
                    spreadsheetId), LOGGER, InformationLevel.Negative);
            return;
        }

        String spreadsheetTitle = workingSpreadsheet.getProperties().getTitle();
        Date minDate = DateUtils.addDays(new Date(), -MAX_DAYS);
        List<Order> orders = orderService.fetchOrders(workingSpreadsheet, minDate);

        if (CollectionUtils.isEmpty(orders)) {
            messagePanel.displayMsg("No orders found for sheet " + spreadsheetTitle, LOGGER, InformationLevel.Negative);
            return;
        }

        processForOrders(orders, spreadsheetTitle, spreadsheetId);
    }


    private void processForOrders(List<Order> orders, String spreadsheetTitle, String spreadsheetId) {
        List<InventoryUpdateRecord> records = getUpdateRecords(orders);

        if (CollectionUtils.isEmpty(records)) {
            messagePanel.displayMsg("No records found to be updated for sheet " + spreadsheetTitle, LOGGER, InformationLevel.Negative);
            return;
        }

        //update qty
        Country country = Settings.load().getSpreadsheetCountry(spreadsheetId);
        OrderItemType type = Settings.load().getSpreadsheetType(spreadsheetId);

        List<InventoryUpdateRecord> toUpdateQty = records.stream().filter(it -> it.getType().updateQty()).collect(Collectors.toList());
        updateSkuQty(toUpdateQty, country, type);

        List<InventoryUpdateRecord> toBeRemoved = records.stream().filter(it -> it.getType().deleteASIN()).collect(Collectors.toList());
        List<String> asinsToAsync = toBeRemoved.stream().filter(it -> it.getType() == UpdateType.DeleteASINSYNC)
                .map(it -> it.getAsin()).collect(Collectors.toList());

        //load asins to be removed from other accounts
        List<String> skus = getSyncSkus(country);
        if (CollectionUtils.isNotEmpty(skus)) {
            skus.forEach(sku -> toBeRemoved.add(new InventoryUpdateRecord(sku, null, UpdateType.DeleteASINSYNC, null)));
        }
        deleteASINs(toBeRemoved, country, type);

        //sync asins
        if (CollectionUtils.isNotEmpty(asinsToAsync)) {
            messagePanel.displayMsg(asinsToAsync.size() + " asins to be uploaded to sync database.\n" + asinsToAsync);
            String context = Settings.load().getContext(country);
            String content = StringUtils.join(asinsToAsync, Constants.COMMA);
            asinAppScript.writeASINSInventoryLoaderSync(context, content);
        }


    }

    public List<String> getSyncSkus(Country country) {
        List<String> skus = new ArrayList<>();
        try {
            List<String> asins = asinAppScript.getAsinInventoryLoaderSync();
            if (CollectionUtils.isNotEmpty(asins)) {
                messagePanel.displayMsg("sync inventory from amazon seller center for " + country.zoneCode() + ". this may take several minutes.");
                Long start = System.currentTimeMillis();
                inventoryReportManager.download(country.zoneCode());
                messagePanel.displayMsg("finished sync inventory in " + Strings.formatElapsedTime(start));

                List<String> invFilePaths = InventoryReportManager.getInventoryFilePaths(country);

                for (String invFilePath : invFilePaths) {
                    List<String> list = InventoryReportManager.getSKUs(asins, invFilePath);
                    skus.addAll(list);
                }

                messagePanel.displayMsg(skus.size() + " skus found to be removed from sync database.\n" + skus);

            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        return skus;
    }

    @Inject private FeedGenerator feedGenerator;
    @Inject private FeedSubmissionEmailSender feedSubmissionEmailSender;

    public void updateSkuQty(List<InventoryUpdateRecord> records, Country country, OrderItemType type) {
        if (CollectionUtils.isEmpty(records)) {
            messagePanel.displayMsg("No asin qty to be update for  " + country.name() + " " + type.name());
            return;
        }

        messagePanel.displayMsg(records.size() + " asins qty to be updated for " + country.name() + " " + type.name());
        try {
            File feedFile = feedGenerator.generateQtyUpdateFeedFromRows(records, country, type);
            messagePanel.displayMsg("Feed file generated and saved at " + feedFile.getAbsolutePath());
            messagePanel.wrapLineMsg(Tools.readFileToString(feedFile));
            //submit feed to amazon via MWS Feed API
            submitFeed(feedFile, BatchFileType.ReQuantity, country, records);
        } catch (Exception e) {
            handleErrors(e, country);
        }
    }


    public void deleteASINs(List<InventoryUpdateRecord> records, Country country, OrderItemType type) {
        if (CollectionUtils.isEmpty(records)) {
            messagePanel.displayMsg("No asin to remove from inventory for " + country.name() + " " + type.name());
            return;
        }
        messagePanel.displayMsg(records.size() + " asins to be removed from inventory " + country.name() + " " + type.name());
        try {
            File feedFile = feedGenerator.generateASINRemovalFeedFromRows(records, country, type);
            messagePanel.displayMsg("Feed file generated and saved at " + feedFile.getAbsolutePath());
            messagePanel.wrapLineMsg(Tools.readFileToString(feedFile));
            //submit feed to amazon via MWS Feed API
            submitFeed(feedFile, BatchFileType.ListingDeletion, country, records);
        } catch (Exception e) {
            handleErrors(e, country);
        }
    }

    public void submitFeed(File feedFile, BatchFileType fileType, Country country, List<InventoryUpdateRecord> records) {
        String result;
        try {
            messagePanel.displayMsg("submitting feed to " + country.name() + " amazon via Feed API");
            result = feedUploadService.submitFeedAllZoneCountries(feedFile, fileType, country);
            if (StringUtils.isBlank(result)) {
                throw new BusinessException("No result returned.");
            }

            records.forEach(record -> {
                try {
                    orderInventoryUpdateLogService.saveFromRecord(record);
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            });

            //send email
            //if (messagePanel instanceof VirtualMessagePanel) {
            feedSubmissionEmailSender.sendSuccessEmail(fileType, result, feedFile, country);
            //}
        } catch (Exception e) {
            handleErrors(e, country);
        }
    }

    public void handleErrors(@NotNull Throwable e, Country country) {
        messagePanel.displayMsg("Error when submitting feed file. " + e.getMessage(), InformationLevel.Negative);
        LOGGER.error("Error when submitting feed file. ", e);
        messagePanel.displayMsg("Please try to submit the feed file via Amazon Seller Center.");
        AsinAsyncFailedLogService.logFailed(country, e.getMessage());
    }

    public void setMessagePanel(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
        feedUploadService.setMessagePanel(messagePanel);
    }

    public List<InventoryUpdateRecord> getUpdateRecords(List<Order> orders) {
        List<InventoryUpdateRecord> records = new ArrayList<>();
        orders.forEach(order -> {
            UpdateType updateType = InventoryUpdateTypeHelper.getUpdateType(order);
            if (updateType == null) {
                return;
            }
            if (!orderInventoryUpdateLogService.updated(order.order_id, order.sku, updateType)) {
                InventoryUpdateRecord inventoryUpdateRecord = new InventoryUpdateRecord(order.sku, order.getASIN(), updateType, order.order_id);
                records.add(inventoryUpdateRecord);
            }
        });

        return records;
    }


}
