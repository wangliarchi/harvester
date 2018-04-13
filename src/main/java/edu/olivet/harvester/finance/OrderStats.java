package edu.olivet.harvester.finance;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import com.opencsv.CSVReader;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.finance.model.Refund;
import edu.olivet.harvester.finance.model.UnfulfilledOrder;
import edu.olivet.harvester.finance.service.FinanceSheetService;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.service.OrderFulfillmentRecordService;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/11/2018 3:24 PM
 */
public class OrderStats {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderStats.class);

    @Inject SheetAPI sheetAPI;
    @Inject OrderService orderService;
    @Inject OrderFulfillmentRecordService orderFulfillmentRecordService;

    private DropboxAssistant dropboxAssistant;

    private static final String DROPBOX_TOKEN = "ksBvMCycir0AAAAAAABWoK4pOnesC7bGfNsLQxd6FKR_2f3nue4rXZVMpyL8Wz21";
    private static final String DROPBOX_ROOT_DIR = "/WatchDog/Refund/";

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    @Inject
    public void init() {
        dropboxAssistant = new DropboxAssistant("HarvesterFinanceApp", DROPBOX_TOKEN, false, "/");
    }

    public void unfilledOrders(String sid, Country country, Date fromDate, @NotNull Date toDate) {
        long start = System.currentTimeMillis();
        fromDate = (fromDate == null) ? DateUtils.addMonths(toDate, -4) : fromDate;

        Date maxDate = DateUtils.addDays(toDate, 3);
        Date minDate = DateUtils.addDays(fromDate, -3);

        Range<Date> dateRange = Range.between(minDate, maxDate);
        messagePanel.displayMsg(String.format("Reading unfilled orders from %s for  %s to  %s", country, minDate, maxDate), LOGGER);

        List<File> sheets = sheetAPI.getAllOrderSheets(sid, country);
        messagePanel.displayMsg(String.format("%d order update sheets found - %s", sheets.size(),
                sheets.stream().map(it -> it.getName()).collect(Collectors.toList())), LOGGER);

        List<Order> allOrders = new ArrayList<>();
        for (File file : sheets) {
            try {
                Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(file.getId());
                List<Order> orders = orderService.fetchOrders(spreadsheet, dateRange);
                Date finalFromDate = fromDate;
                Date finalToDate = toDate;
                orders.removeIf(order -> !checkOrder(order, finalFromDate, finalToDate));

                messagePanel.displayMsg(String.format("%d orders found from %s", orders.size(), spreadsheet.getProperties().getTitle()), LOGGER);

                allOrders.addAll(orders);
            } catch (Exception e) {
                messagePanel.displayMsg(String.format("Fail to read orders from %s - %s", file.getName(), Strings.getExceptionMsg(e)), LOGGER);
            }
        }

        allOrders.sort(Comparator.comparing(Order::getPurchaseDate));
        messagePanel.displayMsg(String.format("Totally %d orders found from %s to %s, took %s", allOrders.size(), fromDate, toDate, Strings.formatElapsedTime(start)), LOGGER);

        //fill
        fillRecordsToSheet(allOrders);
    }

    @Inject FinanceSheetService financeSheetService;

    public void fillRecordsToSheet(List<Order> allOrders) {
        long start = System.currentTimeMillis();
        messagePanel.displayMsg("Filling data to google sheet...");
        List<UnfulfilledOrder> unfulfilledOrders = allOrders.stream()
                .map(it -> UnfulfilledOrder.init(it, REFUND_MAP.getOrDefault(it.order_id, new ArrayList<>())))
                .collect(Collectors.toList());

        financeSheetService.fillInfoToSheet(unfulfilledOrders);
        messagePanel.displayMsg(String.format("Done filling data to google sheet, took %s", Strings.formatElapsedTime(start)));
    }

    public static final String[] DATE_PATTERNS = {"MMM d, YYYY", "d MMM YYYY"};

    public final Map<String, List<Refund>> REFUND_MAP = new HashMap<>();

    public void loadRefunds(String sid) {
        if (REFUND_MAP.size() > 0) {
            return;
        }

        downloadRefunds(sid);

        loadRefundsFromFile(sid);
    }

    public void loadRefundsFromFile(String sid) {
        messagePanel.displayMsg("Loading refund records from files...", LOGGER);
        long start = System.currentTimeMillis();
        String refundFileFolderPath = Directory.APP_DATA + "/finance/refund/" + DROPBOX_ROOT_DIR + sid;
        java.io.File folder = new java.io.File(refundFileFolderPath);

        int total = 0;
        for (java.io.File file : folder.listFiles()) {
            List<String> lines = Tools.readLines(file);
            try {
                //CSVReader reader = new CSVReader(new FileReader(file), '\t');
                //String[] line;
                Country country = getCountryFromFilename(file.getName());
                //while ((line = reader.readNext()) != null) {
                for (String lineString : lines) {
                    String[] line = lineString.split("\t");
                    try {
                        Refund refund = new Refund();
                        Date date = DateUtils.parseDate(line[0], Locale.US, DATE_PATTERNS);
                        //
                        refund.setDate(date);
                        refund.setOrderId(line[1]);

                        refund.setSku(line[2]);
                        refund.setTransactionType(line[3]);
                        refund.setPaymentType(line[4]);
                        refund.setPaymentDetail(line[5]);
                        float amount = Float.parseFloat(line[6].replaceAll("[^0-9-.,]", ""));
                        refund.setAmount(new Money(amount, country));
                        refund.setQuantity(line[7]);

                        if (StringUtils.equalsAnyIgnoreCase(refund.getPaymentType(), "Product charges", "Shipping")
                                || StringUtils.equalsAnyIgnoreCase(refund.getPaymentDetail(), "Product charges", "Shipping")) {
                            List<Refund> refundList = REFUND_MAP.getOrDefault(refund.getOrderId(), new ArrayList<>());
                            boolean existed = false;
                            for (Refund e : refundList) {
                                if (e.getSku().equalsIgnoreCase(refund.getSku()) &&
                                        e.getOrderId().equalsIgnoreCase(refund.getOrderId()) &&
                                        e.getPaymentType().equalsIgnoreCase(refund.getPaymentType())) {
                                    existed = true;
                                    break;
                                }
                            }
                            if (existed == false) {
                                refundList.add(refund);
                                REFUND_MAP.put(refund.getOrderId(), refundList);
                                total++;
                            }
                        }

                    } catch (Exception e) {
                        //LOGGER.error("", e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        messagePanel.displayMsg(String.format("Loaded %d refund records, took %s", total, Strings.formatElapsedTime(start)), LOGGER);
    }

    public Country getCountryFromFilename(String filename) {
        for (Country country : Country.values()) {
            // EU spreadsheet might be named 'UK' or 'EU'
            String regex = country.name();
            if (RegexUtils.containsRegex(filename, regex)) {
                return country;
            }
        }

        throw new BusinessException("Cant get country info from file name " + filename);
    }


    public void downloadRefunds(String sid) {
        messagePanel.displayMsg("Sync refund files from dropbox.", LOGGER);
        long start = System.currentTimeMillis();
        List<FileMetadata> files;
        try {
            files = dropboxAssistant.getAllFiles(DROPBOX_ROOT_DIR + sid + "/", true);
        } catch (DbxException e) {
            //
            LOGGER.error("", e);
            throw new BusinessException(e);
        }

        String localRootDir = Directory.APP_DATA + "/finance/refund";
        for (FileMetadata file : files) {
            //Directory.APP_DATA + "/finance/refund"
            java.io.File target = new java.io.File(localRootDir, file.getPathDisplay());
            if (target.exists()) {
                continue;
            }
            try {
                dropboxAssistant.downloadFile(file, localRootDir);
            } catch (Exception e) {
                //
            }
        }

        messagePanel.displayMsg(String.format("%d refund files downloaded, took %s", files.size(), Strings.formatElapsedTime(start)));
    }

    public boolean checkOrder(Order order, Date fromDate, Date toDate) {
        //去掉自买单和buyer cancel的单
        if (order.selfBuy() || order.buyerCanceled() || order.canceledBySeller()) {
            return false;
        }

        if (!Regex.AMAZON_ORDER_NUMBER.isMatched(order.order_id)) {
            return false;
        }

        //时间区间
        try {
            if (order.getPurchaseDate().before(fromDate) || order.getPurchaseDate().after(toDate)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        //toDate之后做单 需要提取出来
        OrderFulfillmentRecord record = orderFulfillmentRecordService.getRecord(order);
        if (record != null) {
            if (record.getFulfillDate().after(toDate)) {
                order.fulfilledDate = edu.olivet.harvester.utils.common.DateFormat.FULL_DATE.format(record.getFulfillDate());
                return true;
            }
        }

        if (ORDERMAN_LOG.containsKey(order.order_id)) {
            Date date = ORDERMAN_LOG.get(order.order_id);
            if (date.after(toDate)) {
                order.fulfilledDate = edu.olivet.harvester.utils.common.DateFormat.FULL_DATE.format(date);
                return true;
            }
        }


        //toDate之后refund 需要提取出来。
        if (REFUND_MAP.containsKey(order.order_id)) {
            Refund refund = REFUND_MAP.get(order.order_id).get(0);
            if (refund.getDate().after(toDate)) {
                return true;
            }
        } else {
            //如果没有refund，也没有做单，需要提取出来
            if (!order.fulfilled()) {
                return true;
            }
        }

        //其他情况不要
        return false;
    }

    private final Map<String, Date> ORDERMAN_LOG = new HashMap<>();

    public void loadOrderManLogs() {
        messagePanel.displayMsg("Loading order fulfillment records from OrderMan logs.", LOGGER);
        long start = System.currentTimeMillis();
        String logFolderPath = "C:\\OrderMan\\logs";
        java.io.File folder = new java.io.File(logFolderPath);
        if (!folder.exists()) {
            messagePanel.displayMsg("No orderman log folder found.", InformationLevel.Negative);
            return;
        }
        for (java.io.File file : folder.listFiles()) {
            if (StringUtils.containsIgnoreCase(file.getName(), "success")) {
                //success.2017-10-27.log
                String dateString = file.getName().replace("success.", "").replace(".log", "");


                Date date = new Date();
                try {
                    BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    date = new Date(attr.creationTime().to(TimeUnit.MILLISECONDS));
                } catch (IOException e) {
                    // e.printStackTrace();
                }
                try {
                    date = Dates.parseDate(dateString);
                } catch (Exception e) {
                    //
                }

                try {
                    //CSVReader reader = new CSVReader(new FileReader(file), '\t');
                    //String[] line;
                    List<String> lines = Tools.readLines(file);
                    //while ((line = reader.readNext()) != null) {
                    for (String lineString : lines) {
                        String[] line = lineString.split("\t");
                        if (!Regex.AMAZON_ORDER_NUMBER.isMatched(line[2])) {
                            continue;
                        }
                        try {
                            date = Dates.parseDate(line[12]);
                        } catch (Exception e) {
                            //
                        }
                        ORDERMAN_LOG.put(line[2], date);
                        //第三列是订单原单号
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        messagePanel.displayMsg(String.format("%d records found from Orderman logs, took %s", ORDERMAN_LOG.size(), Strings.formatElapsedTime(start)));
    }

    public void execute(String sid) {
        REFUND_MAP.clear();
        try {
            sheetAPI.deleteSheet(sid, FinanceSheetService.SPREADSHEET_ID);
        } catch (Exception e) {
            //
        }
        loadRefunds(sid);
        loadOrderManLogs();
        List<Country> marketplaces = Arrays.asList(Country.US, Country.CA, Country.UK, Country.JP, Country.MX);
        for (Country country : marketplaces) {
            unfilledOrders(sid, country, null, Dates.parseDate("2017-12-31"));
        }
    }

    public void execute() {

        //delete existed
        String sid = Settings.load().getSid();
        execute(sid);
    }

    public static void main(String[] args) {
        OrderStats orderStats = ApplicationContext.getBean(OrderStats.class);
        String sid = "30";

        orderStats.loadOrderManLogs();
        orderStats.loadRefunds(sid);

        orderStats.unfilledOrders(sid, Country.US, null, Dates.parseDate("2017-12-31"));
    }
}
