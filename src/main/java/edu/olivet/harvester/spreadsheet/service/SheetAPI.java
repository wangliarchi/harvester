package edu.olivet.harvester.spreadsheet.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.google.GoogleAPIHelper;
import edu.olivet.foundations.google.GoogleServiceProvider;
import edu.olivet.foundations.google.SpreadService;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.Worksheet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/11/17 1:53 PM
 */
public class SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetAPI.class);

    @Inject
    protected GoogleServiceProvider googleServiceProvider;

    @Inject
    protected SpreadService spreadService;

    @Inject
    protected GoogleAPIHelper googleAPIHelper;

    protected Sheets sheetService;

    @Inject
    public void init() {
        sheetService = googleServiceProvider.getSheetsService(Constants.RND_EMAIL);
    }


    @Repeat(expectedExceptions = BusinessException.class)
    public Spreadsheet getSpreadsheet(String spreadsheetId) {
        try {
            final long start = System.currentTimeMillis();
            Sheets.Spreadsheets.Get request =
                    sheetService.spreadsheets().get(spreadsheetId).setFields("sheets.properties,properties.title");
            Spreadsheet response = request.execute();
            response.setSpreadsheetId(spreadsheetId);
            LOGGER.info("读取{} SHEETS，耗时{}", spreadsheetId, Strings.formatElapsedTime(start));
            return response;
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class)
    public List<ValueRange> bactchGetSpreadsheetValues(Spreadsheet spreadsheet, List<String> ranges) {
        try {
            Sheets.Spreadsheets.Values.BatchGet request = sheetService.spreadsheets().values().batchGet(spreadsheet.getSpreadsheetId()).setRanges(ranges);
            BatchGetValuesResponse response = request.execute();
            return response.getValueRanges();
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class)
    public void batchUpdate(String spreadsheetId, BatchUpdateSpreadsheetRequest request) {
        try {
            sheetService.spreadsheets().batchUpdate(spreadsheetId, request).execute();
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public void batchUpdateValues(String spreadsheetId, List<ValueRange> dateToUpdate) {

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest().setData(dateToUpdate)
                .setValueInputOption("USER_ENTERED");
        try {
            sheetService.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class)
    public void spreadsheetValuesAppend(String spreadsheetId, String range, ValueRange values) {
        try {
            Sheets.Spreadsheets.Values.Append request = sheetService.spreadsheets().values()
                    .append(spreadsheetId, range, values)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS");

            request.execute();
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public SheetProperties getSheetProperties(String spreadsheetId, String sheetName) {
        try {
            Spreadsheet response = sheetService.spreadsheets().get(spreadsheetId).setFields("sheets.properties")
                    .setRanges(Collections.singletonList(sheetName))
                    .execute();

            if (response.getSheets().size() == 0) {
                throw new BusinessException(String.format("No sheet %s from spreadsheet %s", sheetName, spreadsheetId));
            }
            return response.getSheets().get(0).getProperties();
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class)
    public SheetProperties sheetCopyTo(String spreadsheetId, int templateSheetId, String destSpreadId) {

        CopySheetToAnotherSpreadsheetRequest requestBody = new CopySheetToAnotherSpreadsheetRequest();
        requestBody.setDestinationSpreadsheetId(destSpreadId);

        try {
            Sheets.Spreadsheets.SheetsOperations.CopyTo request =
                    sheetService.spreadsheets().sheets().copyTo(spreadsheetId, templateSheetId, requestBody);

            return request.execute();
        } catch (IOException e) {
            throw googleAPIHelper.wrapException(e);
        }
    }





    public void moveSheetToIndex(SheetProperties sheetProperties, String spreadsheetId, int moveTo) {

        sheetProperties.setIndex(moveTo);
        List<Request> requests = new ArrayList<>();
        Request request = new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest().setProperties(sheetProperties).setFields("title,index"));
        requests.add(request);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            this.batchUpdate(spreadsheetId, body);
        } catch (BusinessException e) {
            LOGGER.error("Fail to rename sheet and move to first for {} {}. Try to delete the sheet - {}", sheetProperties.getTitle(), spreadsheetId, e.getMessage());
            deleteSheet(sheetProperties.getSheetId(), spreadsheetId);
        }


    }

    public void deleteSheet(int sheetId, String spreadsheetId) {

        List<Request> requests = new ArrayList<>();
        Request request = new Request().setDeleteSheet(new DeleteSheetRequest().setSheetId(sheetId));
        requests.add(request);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            this.batchUpdate(spreadsheetId, body);
        } catch (BusinessException e) {
            LOGGER.error("Fail to delete sheet {} {} - {}", sheetId, spreadsheetId, e.getMessage());
            throw new BusinessException(e);
        }

    }


    @Inject
    private AppScript appScript;

    public void markBuyerCancelOrders(List<Order> orders, Worksheet worksheet) {
        //mark gray
        orders.forEach(order -> {
            try {
                appScript.markColor(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName(), order.row, OrderEnums.OrderColor.DarkGray2);
            } catch (Exception e) {
                LOGGER.error("Failed to mark row {} of {} as gray: ", order.row, worksheet, e);
            }
        });
        String spreadsheetId = worksheet.getSpreadsheet().getSpreadsheetId();
        String sheetName = worksheet.getSheetName();

        final long start = System.currentTimeMillis();

        List<ValueRange> dateToUpdate = new ArrayList<>();

        for (Order order : orders) {
            //update remart cell

            if (!StringUtils.containsIgnoreCase(order.remark, "cancel")) {
                StringBuilder remarkText = new StringBuilder("Buyer Canceled. ");
                if (StringUtils.isNotEmpty(order.remark)) {
                    remarkText.append(order.remark).append(" ");
                }

                ValueRange remarkData = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(remarkText.toString())))
                        .setRange(sheetName + "!S" + order.row);

                dateToUpdate.add(remarkData);
            }


        }


        if (CollectionUtils.isEmpty(dateToUpdate)) {
            LOGGER.error("No rows to update buyer canceled info {}", worksheet.toString());
            return;
        }
        try {
            this.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update order update sheet remark {} - {}", worksheet.toString(), e);
            throw new BusinessException(e);
        }

        LOGGER.info("{} rows updated as buyer canceled on spreadsheet {}, took {}", orders.size(), worksheet.toString(), Strings.formatElapsedTime(start));


    }

}
