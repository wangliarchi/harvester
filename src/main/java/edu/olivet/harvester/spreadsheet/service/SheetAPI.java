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
import edu.olivet.foundations.google.DataSource;
import edu.olivet.foundations.google.GoogleAPIHelper;
import edu.olivet.foundations.google.GoogleServiceProvider;
import edu.olivet.foundations.google.SpreadService;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
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


    private static final String TMM_EMAIL = "olivetrnd153.tmm@gmail.com";
    private static final String SF_EMAIL = "olivetrnd153.sf@gmail.com";
    private static final String SF_EMAIL_2 = "olivetrnd153.sf2@gmail.com";
    private static final String RS_EMAIL = "olivetrnd153.rs@gmail.com";

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

    public void deleteSheet(String sheetName, String spreadsheetId) {
        try {
            SheetProperties sheetProperties = getSheetProperties(spreadsheetId, sheetName);
            deleteSheet(sheetProperties.getSheetId(), spreadsheetId);
        } catch (Exception e) {
            //LOGGER.error("", e);
            LOGGER.info("Sheet {} not found.", sheetName);
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
            LOGGER.info("No rows to update buyer canceled info {}", worksheet.toString());
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


    @Repeat(expectedExceptions = BusinessException.class, times = 5)
    public List<File> getAvailableSheets(String sid, Country country, String dataSourceId) {
        List<File> sheets = spreadService.getAvailableSheets(sid, country, dataSourceId, Constants.RND_EMAIL);
        removeSpecialSheets(sheets);
        return sheets;
    }

    @lombok.Setter
    private boolean debugMode = false;

    public void removeSpecialSheets(List<File> sheets) {
        List<String> toRemoveKeywordList = Stream.of("fba resale", "cancel", "copy of", "grey", "gray", "pl",
                "backup", "to white", "国际转运", "forward", "top reviewer", "german book", "special sheet")
                .collect(Collectors.toList());
        if (!debugMode) {
            toRemoveKeywordList.add("test");
        }

        //for (int i = 2011; i < Dates.getYear(new Date()); i++) {
        //    toRemoveKeywordList.add(String.valueOf(i));
        //}

        String[] toRemoveKeywords = toRemoveKeywordList.toArray(new String[toRemoveKeywordList.size()]);

        sheets.removeIf(it -> Strings.containsAnyIgnoreCase(it.getName().toLowerCase(), toRemoveKeywords));
    }

    /**
     * Query cache. Key: ${search.term}, Value: ${spreadsheets.metadata}
     */
    private final Map<String, List<File>> spreadsCache = new HashMap<>();

    public List<File> getAvailableOrderSheets(String sid, Country country, boolean debugMode) {
        this.debugMode = debugMode;
        return getAvailableOrderSheets(sid, country);
    }

    public List<File> getAvailableOrderSheets(String sid, Country country) {
        List<Predicate<String>> predicates = new ArrayList<>();
        predicates.add(new Identity(sid, country));
        predicates.add(new Historical(false));
        String key = StringUtils.join(predicates, ", ");
        LOGGER.debug("Search term：{}", key);
        List<File> list = spreadsCache.get(key);
        if (CollectionUtils.isEmpty(list)) {
            list = this._getAvailableOrderSheets(sid, predicates);
            spreadsCache.put(key, list);
        }
        return list;
    }

    public List<File> getAllOrderSheets(String sid, Country country) {
        List<Predicate<String>> predicates = new ArrayList<>();
        predicates.add(new Identity(sid, country, true));
        String key = StringUtils.join(predicates, ", ");
        LOGGER.debug("Search term：{}", key);
        List<File> list = spreadsCache.get(key);
        if (CollectionUtils.isEmpty(list)) {
            list = this._getAvailableOrderSheets(sid, predicates);
            spreadsCache.put(key, list);
        }
        return list;
    }

    public List<File> _getAvailableOrderSheets(String sid, List<Predicate<String>> predicates) {
        List<File> searchResult = new ArrayList<>();
        String query = String.format("mimeType='application/vnd.google-apps.spreadsheet' and (name contains '%s' or name contains '%s' or name contains '%s')",
                sid, "ACC" + sid, "ACC_" + sid);
        try {
            searchResult = spreadService.query(Constants.RND_EMAIL, query);
        } catch (Exception e) {
            //
        }
        removeSpecialSheets(searchResult);

        List<File> result = new ArrayList<>();
        for (File spread : searchResult) {
            if (this.evaluate(spread.getName(), predicates)) {
                result.add(spread);
            }
        }
        return result;
    }


    private static final String NAME_PATTERN_ORDER = "ORDER";
    private static final String NAME_PATTERN_UPDATE = "UPDATE";

    /**
     * <pre>
     * Validate whether the given spreadsheet name matches given account number and marketplace.
     *
     * Tips:
     * 1. The account number should match exactly, for example, 18 should match 18 only, while 718 and 918 not;
     * 2. Cross account fulfillment should ignore, for example, "ACC81 US Order Update 101 111 126" will match 81 only.
     * 101, 111, 126 should be skipped;
     * </pre>
     *
     * @param spreadTitle spreadsheet title, eg: "ACC 716 US  Book Order Update 711"
     * @param accSid account number. eg: 18, 24
     * @param nation marketplace, eg: US, UK
     * @return if all things go well <tt>null</tt> will be returned, otherwise the failure reason will be given
     */
    @Nullable String validateSpreadName(@NotNull String spreadTitle, String accSid, Country nation, boolean onlySelf) {
        // Remove year also, or 201 might match 2017, 2016 and etc - too many to choose
        String str = spreadTitle.toUpperCase().replaceAll("[0-9]{4}", StringUtils.EMPTY);

        //Only process texts before "Update", content after it will be removed as noises
        if (onlySelf) {
            str = str.replaceFirst(NAME_PATTERN_UPDATE + ".*", NAME_PATTERN_UPDATE);
        }
        try {
            validateCountry(str, nation);
        } catch (IllegalStateException e) {
            return UIText.message("error.appcfg.spread", spreadTitle, accSid, nation.toString());
        }

        String sidRegex = String.format("([^0-9]%s|^%s)($|[^0-9])", accSid, accSid);
        if (!RegexUtils.containsRegex(str, sidRegex) ||
                !Strings.containsAllIgnoreCase(str, accSid, NAME_PATTERN_ORDER, NAME_PATTERN_UPDATE)) {
            return UIText.message("error.appcfg.spread", spreadTitle, accSid, nation.toString());
        }

        return null;
    }

    /**
     * Validate whether spreadsheet is correctly mapped to a given country
     */
    void validateCountry(String spreadsheetTitle, Country country) {
        // Example: 'EB00_MX Books Order Update us uk-18'. US, UK are noise and need to be removed
        // 'Order' contains 'de' which is equal to country code of DE, needs to be removed
        String str = StringUtils.defaultString(spreadsheetTitle).toUpperCase()
                .replaceFirst(NAME_PATTERN_ORDER + ".*", StringUtils.EMPTY);
        // EU spreadsheet might be named 'UK' or 'EU'
        String regex = country.europe() ? "(UK|EU)" : country.name();
        if (!RegexUtils.containsRegex(str, regex)) {
            throw new IllegalStateException(String.format("Spreadsheet '%s' is not for %s.", spreadsheetTitle, country.name()));
        }
    }

    private class Identity implements Predicate<String> {
        private final String accSid;
        private final Country country;
        private final boolean onlySelf;

        private Identity(String accSid, Country country) {
            this(accSid, country, false);
        }

        private Identity(String accSid, Country country, boolean onlySelf) {
            this.accSid = accSid;
            this.country = country;
            this.onlySelf = onlySelf;
        }

        @Override
        public boolean evaluate(String spreadTitle) {
            String result = validateSpreadName(spreadTitle, accSid, country, onlySelf);
            return StringUtils.isBlank(result);
        }

        @Override
        public String toString() {
            return accSid + country.name();
        }
    }


    private static class Historical implements Predicate<String> {
        private boolean historical;

        private Historical(boolean historical) {
            this.historical = historical;
        }

        @Override
        public boolean evaluate(String s) {
            return historical == Strings.containsAnyIgnoreCase(s, DataSource.HISTORY, "Histroy");
        }

        @Override
        public String toString() {
            return historical ? DataSource.HISTORY : StringUtils.EMPTY;
        }
    }

    private boolean evaluate(String spreadsheetTitle, List<Predicate<String>> predicates) {
        for (Predicate<String> predicate : predicates) {
            if (!predicate.evaluate(spreadsheetTitle)) {
                return false;
            }
        }
        return true;
    }


    public int rowNoFromRange(String range) {
        String[] aparts = StringUtils.split(range, ":");
        String[] bparts = StringUtils.split(aparts[ArrayUtils.getLength(aparts) - 1], "!");
        return IntegerUtils.parseInt(bparts[ArrayUtils.getLength(bparts) - 1].replaceAll("[^0-9]", ""), 0);
    }


    //todo check if template sheet has correct format
    protected SheetProperties createNewSheetIfNotExisted(String spreadsheetId, String sheetName, String templateSheetName) {
        long start = System.currentTimeMillis();

        //check if existed
        try {
            SheetProperties sheetProperties = getSheetProperties(spreadsheetId, sheetName);
            LOGGER.info("Sheet {} already created.", sheetName);
            return sheetProperties;
        } catch (Exception e) {
            //LOGGER.error("", e);
        }

        int templateSheetId;
        try {
            templateSheetId = getSheetProperties(spreadsheetId, templateSheetName).getSheetId();
        } catch (Exception e) {
            LOGGER.error("Error loading template sheet {} from {}", templateSheetName, spreadsheetId, e);
            throw new BusinessException(e);
        }

        try {
            SheetProperties sheetProperties = duplicateSheet(spreadsheetId, templateSheetId, sheetName);
            LOGGER.info("Sheet {} created successfully, took {}.", sheetName, Strings.formatElapsedTime(start));
            return sheetProperties;
        } catch (Exception e) {
            LOGGER.error("Fail to copy template sheet  {} {} {}", spreadsheetId, templateSheetName, sheetName, e);
            throw new BusinessException(e);
        }
    }

    public static void main(String[] args) {
        SheetAPI sheetAPI = ApplicationContext.getBean(SheetAPI.class);
        List<File> sheets = sheetAPI.getAllOrderSheets("709", Country.US);
    }
}
