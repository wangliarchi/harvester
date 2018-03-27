package edu.olivet.harvester.spreadsheet.service;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.google.GoogleAPIHelper;
import edu.olivet.foundations.google.GoogleServiceProvider;
import edu.olivet.foundations.google.SpreadService;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/11/17 1:53 PM
 */
public class SheetAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetAPI.class);

    @Inject protected GoogleServiceProvider googleServiceProvider;

    @Inject protected SpreadService spreadService;

    @Inject protected GoogleAPIHelper googleAPIHelper;

    protected Sheets sheetService;
    protected Drive driveService;


    private final static String TMM_EMAIL = "olivetrnd153.tmm@gmail.com";
    private final static String SF_EMAIL = "olivetrnd153.sf@gmail.com";
    private final static String SF_EMAIL_2 = "olivetrnd153.sf2@gmail.com";
    private final static String RS_EMAIL = "olivetrnd153.rs@gmail.com";

    @Inject
    public void init() {
        sheetService = googleServiceProvider.getSheetsService(getSheetServiceEmail());
        driveService = googleServiceProvider.getDriveService(Constants.RND_EMAIL);
    }

    /**
     * 0-60，84
     * 61-99，88
     * 100-249，81
     * 250-999，88
     */
    protected String getSheetServiceEmail() {
        //return "olivetrnd153.2@gmail.com";


        String sid;
        try {
            sid = Settings.load().getSid();
        } catch (Exception e) {
            return Constants.RND_EMAIL;
        }

        if (StringUtils.startsWith(sid, "7") && StringUtils.length(sid) == 3) {
            return TMM_EMAIL;
        }

        if (StringUtils.startsWith(sid, "9") && StringUtils.length(sid) == 3) {
            return RS_EMAIL;
        }

        try {
            int id = Integer.parseInt(sid);
            if (id < 80) {
                return SF_EMAIL;
            }


            if (id < 160) {
                return SF_EMAIL_2;
            }
        } catch (Exception e) {
            //
        }

        return Constants.RND_EMAIL;
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
            if (StringUtils.containsIgnoreCase(Strings.getExceptionMsg(e), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                WaitTime.Short.execute();
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public void sharePermissions(String fileId, String emailAddress) {
        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
                // Handle error
                System.err.println(e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission, HttpHeaders responseHeaders) throws IOException {
                System.out.println("Permission ID: " + permission.getId());
            }
        };

        try {
            BatchRequest batch = driveService.batch();
            Permission userPermission = new Permission()
                    .setRole("writer")
                    .setType("user")
                    .setEmailAddress(emailAddress);
            driveService.permissions().create(fileId, userPermission)
                    .setFields("id")
                    .queue(batch, callback);
            batch.execute();
            WaitTime.Short.execute();
        } catch (IOException e) {
            LOGGER.error("", e);
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 5)
    public Spreadsheet getSpreadsheet(String spreadsheetId, List<String> ranges) {
        try {
            final long start = System.currentTimeMillis();
            Sheets.Spreadsheets.Get request =
                    sheetService.spreadsheets().get(spreadsheetId).setIncludeGridData(true).setRanges(ranges);
            Spreadsheet response = request.execute();
            response.setSpreadsheetId(spreadsheetId);
            LOGGER.info("读取{} SHEETS，耗时{}", spreadsheetId, Strings.formatElapsedTime(start));
            return response;
        } catch (IOException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 5)
    public List<ValueRange> batchGetSpreadsheetValues(Spreadsheet spreadsheet, List<String> ranges) {
        return batchGetSpreadsheetValues(spreadsheet.getSpreadsheetId(), ranges);
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 5)
    public List<ValueRange> batchGetSpreadsheetValues(String spreadsheetId, List<String> ranges) {
        try {
            Sheets.Spreadsheets.Values.BatchGet request = sheetService.spreadsheets().values().batchGet(spreadsheetId).setRanges(ranges);
            BatchGetValuesResponse response = request.execute();
            return response.getValueRanges();
        } catch (IOException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 10)
    public BatchUpdateSpreadsheetResponse batchUpdate(String spreadsheetId, BatchUpdateSpreadsheetRequest request) {
        try {
            return sheetService.spreadsheets().batchUpdate(spreadsheetId, request).execute();
        } catch (IOException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 10)
    public void batchUpdateValues(String spreadsheetId, List<ValueRange> dateToUpdate) {

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest().setData(dateToUpdate)
                .setValueInputOption("USER_ENTERED");
        try {
            sheetService.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
        } catch (IOException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class, times = 10)
    public void spreadsheetValuesAppend(String spreadsheetId, String range, ValueRange values) {
        try {
            Sheets.Spreadsheets.Values.Append request = sheetService.spreadsheets().values()
                    .append(spreadsheetId, range, values)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS");
            WaitTime.Shortest.execute();
            request.execute();
        } catch (IOException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 10)
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
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }

    @Repeat(expectedExceptions = BusinessException.class, times = 10)
    public SheetProperties duplicateSheet(String spreadsheetId, int templateSheetId, String newSheetName) {
        DuplicateSheetRequest duplicateSheetRequest = new DuplicateSheetRequest();
        duplicateSheetRequest.setSourceSheetId(templateSheetId)
                .setInsertSheetIndex(0)
                .setNewSheetName(newSheetName);

        List<Request> requests = new ArrayList<>();
        Request request = new Request().setDuplicateSheet(duplicateSheetRequest);
        requests.add(request);
        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            BatchUpdateSpreadsheetResponse response = this.batchUpdate(spreadsheetId, body);
            return response.getReplies().get(0).getDuplicateSheet().getProperties();
        } catch (Exception e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw new BusinessException(e);
        }

    }

    @Repeat(expectedExceptions = BusinessException.class, times = 10)
    public SheetProperties sheetCopyTo(String spreadsheetId, int templateSheetId, String destSpreadId) {

        CopySheetToAnotherSpreadsheetRequest requestBody = new CopySheetToAnotherSpreadsheetRequest();
        requestBody.setDestinationSpreadsheetId(destSpreadId);

        try {
            Sheets.Spreadsheets.SheetsOperations.CopyTo request =
                    sheetService.spreadsheets().sheets().copyTo(spreadsheetId, templateSheetId, requestBody);

            return request.execute();
        } catch (IOException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "403 Forbidden")) {
                //authorize
                sharePermissions(spreadsheetId, getSheetServiceEmail());
                throw new BusinessException(e);
            }
            throw googleAPIHelper.wrapException(e);
        }
    }


    public void moveSheetToIndex(SheetProperties sheetProperties, String spreadsheetId, int moveTo) {

        sheetProperties.setIndex(moveTo);
        List<Request> requests = new ArrayList<>();
        Request request = new Request().setUpdateSheetProperties(
                new UpdateSheetPropertiesRequest().setProperties(sheetProperties).setFields("title,index"));
        requests.add(request);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            BatchUpdateSpreadsheetResponse response = this.batchUpdate(spreadsheetId, body);
        } catch (BusinessException e) {
            LOGGER.error("Fail to rename sheet and move to first for {} {}. Try to delete the sheet - {}",
                    sheetProperties.getTitle(), spreadsheetId, e);
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
            BatchUpdateSpreadsheetResponse response = this.batchUpdate(spreadsheetId, body);
        } catch (BusinessException e) {
            LOGGER.error("Fail to delete sheet {} {} - {}", sheetId, spreadsheetId, e);
            throw new BusinessException(e);
        }

    }


    public void deleteRow(String spreadsheetId, int sheetId, int row) {
        List<Request> requests = new ArrayList<>();
        Request request = new Request().setDeleteDimension(new DeleteDimensionRequest().setRange(
                new DimensionRange().setSheetId(sheetId)
                        .setStartIndex(row - 1).setEndIndex(row)
                        .setDimension("ROWS")
        ));
        requests.add(request);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            BatchUpdateSpreadsheetResponse response = this.batchUpdate(spreadsheetId, body);
        } catch (BusinessException e) {
            LOGGER.error("Fail to delete row {} from sheet {} {} - {}", row, sheetId, spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    public int lockSheet(String spreadsheetId, int sheetId, String description) {
        List<Request> requests = new ArrayList<>();
        Request request = new Request().setAddProtectedRange(new AddProtectedRangeRequest()
                .setProtectedRange(new ProtectedRange().setDescription(description).setWarningOnly(false)
                        .setEditors(new Editors().setUsers(Lists.newArrayList(Constants.RND_EMAIL)))
                        .setRange(new GridRange().setSheetId(sheetId))
                )
        );
        requests.add(request);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            BatchUpdateSpreadsheetResponse response = this.batchUpdate(spreadsheetId, body);
            return response.getReplies().get(0).getAddProtectedRange().getProtectedRange().getProtectedRangeId();
        } catch (BusinessException e) {
            LOGGER.error("Fail to local sheet {} {} - {}", sheetId, spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    public void unlockSheet(String spreadsheetId, int protecttedRangeId) {
        List<Request> requests = new ArrayList<>();
        Request request = new Request().setDeleteProtectedRange(new DeleteProtectedRangeRequest()
                .setProtectedRangeId(protecttedRangeId)
        );
        requests.add(request);

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);

        try {
            BatchUpdateSpreadsheetResponse response = this.batchUpdate(spreadsheetId, body);
        } catch (BusinessException e) {
            LOGGER.error("Fail to unlock sheet {} {} - {}", protecttedRangeId, spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    @Inject
    private AppScript appScript;

    public void markBuyerCancelOrders(List<Order> orders, Worksheet worksheet) {
        //mark gray
        orders.forEach(order -> {
            try {
                appScript.markColor(worksheet.getSpreadsheet().getSpreadsheetId(), worksheet.getSheetName(),
                        order.row, OrderEnums.OrderColor.DarkGray2);
            } catch (Exception e) {
                LOGGER.error("Failed to mark row {} of {} as gray: ", order.row, worksheet, e);
            }
        });
        String spreadsheetId = worksheet.getSpreadsheet().getSpreadsheetId();
        String sheetName = worksheet.getSheetName();

        final long start = System.currentTimeMillis();

        List<ValueRange> dateToUpdate = new ArrayList<>();

        for (Order order : orders) {
            //update remark cell

            if (!StringUtils.containsIgnoreCase(order.remark, "cancel")) {
                StringBuilder remarkText = new StringBuilder("Buyer Canceled. ");
                if (StringUtils.isNotEmpty(order.remark)) {
                    remarkText.append(order.remark).append(" ");
                }

                ValueRange remarkData = new ValueRange()
                        .setValues(Collections.singletonList(Collections.singletonList(remarkText.toString())))
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

        LOGGER.info("{} rows updated as buyer canceled on spreadsheet {}, took {}",
                orders.size(), worksheet.toString(), Strings.formatElapsedTime(start));


    }


    @Repeat(expectedExceptions = BusinessException.class, times = 10)
    public List<File> getAvailableSheets(String sid, Country country, String dataSourceId) {
        List<File> sheets = spreadService.getAvailableSheets(sid, country, dataSourceId, Constants.RND_EMAIL);

        List<String> toRemoveKeywordList = Stream.of("fba resale", "cancel", "test", "history", "histroy", "copy of", "grey", "gray", "pl",
                "backup", "to white", "国际转运", "forward", "top reviewer", "german book", "special sheet")
                .collect(Collectors.toList());

        for (int i = 2011; i < Dates.getYear(new Date()); i++) {
            toRemoveKeywordList.add(String.valueOf(i));
        }

        String[] toRemoveKeywords = toRemoveKeywordList.toArray(new String[toRemoveKeywordList.size()]);

        sheets.removeIf(it -> Strings.containsAnyIgnoreCase(it.getName().toLowerCase(), toRemoveKeywords));

        return sheets;
    }


    public int rowNoFromRange(String range) {
        String[] aparts = StringUtils.split(range, ":");
        String[] bparts = StringUtils.split(aparts[ArrayUtils.getLength(aparts) - 1], "!");
        return IntegerUtils.parseInt(bparts[ArrayUtils.getLength(bparts) - 1].replaceAll("[^0-9]", ""), 0);
    }


    public static void main(String[] args) {
        SheetAPI sheetAPI = ApplicationContext.getBean(SheetAPI.class);
        sheetAPI.sharePermissions("1cPFBnjxwLd2AFNcuODIsVFi5eafYslBIVA0FN_3CJgs", "olivetrnd153.2@gmail.com");
    }
}
