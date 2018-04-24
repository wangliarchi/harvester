package edu.olivet.harvester.finance;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.deploy.DropboxAssistant;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.OrderService;
import edu.olivet.harvester.finance.model.Refund;
import edu.olivet.harvester.finance.model.UnfulfilledOrder;
import edu.olivet.harvester.finance.service.FinanceSheetService;
import edu.olivet.harvester.finance.service.InvoiceDownloaderService;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.service.OrderFulfillmentRecordService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
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
    private static java.io.File orderRecordsFile;
    private final Map<String, Date> ORDER_RECORDS = new HashMap<>();
    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    @Inject
    public void init() {
        dropboxAssistant = new DropboxAssistant("HarvesterFinanceApp", DROPBOX_TOKEN, false, "/");
        orderRecordsFile = new java.io.File(Directory.Log.folder() + "/order-records.log");
        if (orderRecordsFile.exists()) {
            for (String line : Tools.readLines(orderRecordsFile)) {
                try {
                    String[] parts = line.split(",");
                    ORDER_RECORDS.put(parts[0], Dates.parseDate(parts[2]));
                } catch (Exception e) {
                    //
                }
            }
        }
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
        allOrders = allOrders.stream().filter(distinctByKey(it -> it.order_id + "-" + it.sku + "-" + it.order_number)).collect(Collectors.toList());
        allOrders.sort(Comparator.comparing(Order::getPurchaseDate));
        messagePanel.displayMsg(String.format("Totally %d orders found from %s to %s, took %s", allOrders.size(), fromDate, toDate, Strings.formatElapsedTime(start)), LOGGER);

        //fill
        fillRecordsToSheet(allOrders);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Inject FinanceSheetService financeSheetService;

    public void fillRecordsToSheet(List<Order> allOrders) {
        long start = System.currentTimeMillis();
        messagePanel.displayMsg("Filling data to google sheet...");
        List<UnfulfilledOrder> unfulfilledOrders = new ArrayList<>();
        for (Order order : allOrders) {
            UnfulfilledOrder unfulfilledOrder = UnfulfilledOrder.init(order, REFUND_MAP.getOrDefault(order.order_id, new ArrayList<>()));
            unfulfilledOrders.add(unfulfilledOrder);
            REFUND_MAP.remove(order.order_id);
        }

        financeSheetService.fillInfoToSheet(unfulfilledOrders);
        messagePanel.displayMsg(String.format("Done filling data to google sheet, took %s", Strings.formatElapsedTime(start)));
    }

    public static final String[] DATE_PATTERNS = {"MMM d, YYYY", "d MMM YYYY", "d-MMM-YY"};

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
                        LOGGER.error("", e);
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
        } else if (ORDERMAN_LOG.containsKey(order.order_id)) {
            Date date = ORDERMAN_LOG.get(order.order_id);
            if (date.after(toDate)) {
                order.fulfilledDate = edu.olivet.harvester.utils.common.DateFormat.FULL_DATE.format(date);
                return true;
            }
        } else if (ORDER_RECORDS.containsKey(order.order_id)) {
            Date date = ORDER_RECORDS.get(order.order_id);
            if (date.after(toDate)) {
                order.fulfilledDate = edu.olivet.harvester.utils.common.DateFormat.FULL_DATE.format(date);
                return true;
            }
        } else if (order.fulfilled() && order.getPurchaseDate().after(DateUtils.addDays(toDate, -30))) {
            try {
                Date date = getOrderFulfillmentDateFromBuyerAccount(order);
                if (date != null && date.after(toDate)) {
                    order.fulfilledDate = edu.olivet.harvester.utils.common.DateFormat.FULL_DATE.format(date);
                    return true;
                }
            }catch (Exception e) {
                //
            }
        }

        //https://www.amazon.com/gp/css/summary/print.html/ref=od_aui_print_invoice?ie=UTF8&orderID=113-4493552-8669018
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

    public Date getOrderFulfillmentDateFromBuyerAccount(Order order) {
        Account buyer;
        try {
            buyer = BuyerAccountSettingUtils.load().getByEmail(order.account).getBuyerAccount();
        } catch (Exception e) {
            return null;
        }

        Country country = OrderCountryUtils.getFulfillmentCountry(order);
        BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
        TabbedBuyerPanel.getInstance().setRunningIcon(buyerPanel);

        try {
            Browser browser = buyerPanel.getBrowserView().getBrowser();
            String url = country.baseUrl() + "/gp/css/summary/print.html?ie=UTF8&orderID=" + order.order_number;
            JXBrowserHelper.loadPage(browser, url);

            for (int i = 0; i < 3; i++) {
                if (LoginPage.needLoggedIn(browser)) {
                    LoginPage loginPage = new LoginPage(buyerPanel);
                    loginPage.login();
                    WaitTime.Shortest.execute();
                } else {
                    if (i > 0) {
                        JXBrowserHelper.loadPage(browser, url);
                        WaitTime.Shortest.execute();
                    }
                    break;
                }
            }

            if (LoginPage.needLoggedIn(browser)) {
                return null;
            }

            //WaitTime.Shortest.execute();

            for (DOMElement td : JXBrowserHelper.selectElementsByCssSelector(browser, "td")) {
                try {
                    String dateString = JXBrowserHelper.textFromElement(td);
                    String[] parts = dateString.split(":");
                    if (parts.length == 2) {
                        Date date = InvoiceDownloaderService.parseOrderDate(parts[1].trim(), country);
                        if (date != null) {
                            String ds = edu.olivet.harvester.utils.common.DateFormat.FULL_DATE.format(date);
                            try {
                                FileUtils.writeStringToFile(orderRecordsFile, order.order_id + "," + order.order_number + "," + ds + "\n", StandardCharsets.UTF_8, true);
                            } catch (IOException e) {
                                throw new BusinessException(e);
                            }
                            return date;
                        }
                    }
                } catch (Exception e) {
                    //
                }
            }


        } finally {
            TabbedBuyerPanel.getInstance().setNormalIcon(buyerPanel);
        }

        return null;

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
        loadRefunds(sid);
        loadOrderManLogs();
        List<Country> marketplaces = Arrays.asList(Country.US, Country.CA, Country.UK, Country.JP, Country.MX);
        for (Country country : marketplaces) {
            unfilledOrders(sid, country, null, Dates.parseDate("2017-12-31"));
        }
    }

    public void execute() {
        PSEventListener.reset(SimpleOrderSubmissionRuntimePanel.getInstance());
        PSEventListener.start();
        try {
            //delete existed
            String sid = Settings.load().getSid();
            execute(sid);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        PSEventListener.end();
    }

    public static void main(String[] args) {

        OrderStats orderStats = ApplicationContext.getBean(OrderStats.class);

        String sid = "30";

        //orderStats.loadOrderManLogs();
        orderStats.loadRefundsFromFile(sid);

        orderStats.unfilledOrders(sid, Country.US, null, Dates.parseDate("2017-12-31"));
    }
}
