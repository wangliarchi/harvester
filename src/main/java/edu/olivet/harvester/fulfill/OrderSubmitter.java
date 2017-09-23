package edu.olivet.harvester.fulfill;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.teamdev.jxbrowser.chromium.SavePageType;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Spreadsheet;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order station prototype entry
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Singleton
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);
    private static final String SPREAD_ID = "1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8";

    @Inject private AppsScript appsScript;

    private void execute() {
        Settings settings = Settings.load();
        if (settings == null || CollectionUtils.isEmpty(settings.getConfigs())) {
            return;
        }

        Spreadsheet spreadsheet = appsScript.getSpreadsheet(SPREAD_ID);
        List<String> sheetNames = spreadsheet.getSheetNames();
        sheetNames.removeIf(it -> !Regex.COMMON_ORDER_SHEET_NAME.isMatched(it));
        sheetNames.add(0, StringUtils.EMPTY);

        String[] array = sheetNames.toArray(new String[sheetNames.size()]);
        SelectOrderRangeDialog dialog = UITools.setDialogAttr(new SelectOrderRangeDialog(array));
        List<OrderRange> ranges = dialog.getOrderRanges();
        if (CollectionUtils.isEmpty(ranges)) {
            return;
        }

        Configuration config = settings.getConfigs().get(0);
        Account prime = config.getPrimeBuyer(), pt = config.getBuyer();

        Map<String, BrowserFrame> frameMap = new HashMap<>();
        frameMap.computeIfAbsent(prime.key(), k -> {
            BrowserFrame frame = new BrowserFrame(prime);
            frame.loginMobilePage();
            return frame;
        });
        frameMap.computeIfAbsent(pt.key(), k -> {
            BrowserFrame frame = new BrowserFrame(pt);
            frame.loginMobilePage();
            return frame;
        });

        for (OrderRange range : ranges) {
            String sheetName = range.getSheetName();
            List<Order> orders = appsScript.readOrders(SPREAD_ID, sheetName);
            int i = 0;
            for (Order order : orders) {
                if (range.beyond(order.row)) {
                    break;
                }
                Account buyer = i++ % 4 == 0 ? prime : pt;
                if (!range.contains(order.row)) {
                    LOGGER.warn("{} does not contains {}", range, order.row);
                    continue;
                } else if (order.colorIsGray() || order.buyerCanceled()) {
                    LOGGER.warn("Row {}({}) is not eligible to fulfill.", order.row, order.order_id);
                    continue;
                }

                BrowserFrame frame = frameMap.get(buyer.key());
                final long start = System.currentTimeMillis();
                try {
                    frame.submit(order, buyer);
                    LOGGER.info("Submit order {} at row {} of sheet {} finished in {}.", order.order_id, order.row,
                        sheetName, Strings.formatElapsedTime(start));
                } catch (Exception e) {
                    LOGGER.error("Failed to submit order {} at row {} of sheet {}:", order.order_id, order.row, sheetName, e);
                } finally {
                    String filePath = Directory.Tmp.path() + File.separator + order.order_id + ".html";
                    String dirPath = Directory.Tmp.path() + File.separator + order.order_id + "";
                    frame.getBrowser().saveWebPage(filePath, dirPath, SavePageType.ONLY_HTML);
                    frame.clearShoppingCart();
                    LOGGER.info("Process order {} at row {} of sheet {} in {}.", order.order_id, order.row,
                        sheetName, Strings.formatElapsedTime(start));
                }
            }
        }
    }

    public static void main(String[] args) {
        UITools.setTheme();
        ApplicationContext.getBean(OrderSubmitter.class).execute();
    }

}
