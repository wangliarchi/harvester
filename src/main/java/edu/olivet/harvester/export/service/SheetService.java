package edu.olivet.harvester.export.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.service.OrderItemTypeHelper;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.spreadsheet.utils.SheetUtils;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/19/17 6:20 PM
 */
public class SheetService extends SheetAPI {
    private final static String TEMPLATE_SHEET_NAME = "Template";
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);

    @Inject
    private OrderItemTypeHelper orderItemTypeHelper;
    @Inject
    Now now;


    public void fillOrders(Country country, List<Order> orders, MessagePanel messagePanel) {
        List<String> spreadsheetIds = Settings.load().getConfigByCountry(country).listSpreadsheetIds();

        //US only has BOOK spreadsheet, other marketplaces have book and product
        if (country == Country.US) {
            fillOrders(spreadsheetIds.get(0), orders);
            messagePanel.displayMsg(orders.size() + " order(s) exported.", LOGGER);
        } else {
            //divide orders to book and product group
            List<edu.olivet.harvester.model.Order> bookOrders = new ArrayList<>();
            List<edu.olivet.harvester.model.Order> productOrders = new ArrayList<>();
            for (edu.olivet.harvester.model.Order order : orders) {
                if (orderItemTypeHelper.getItemType(order) == OrderEnums.OrderItemType.PRODUCT) {
                    productOrders.add(order);
                } else {
                    bookOrders.add(order);
                }
            }

            if (CollectionUtils.isNotEmpty(bookOrders)) {
                String bookSpreadsheetId = Settings.load().getConfigByCountry(country).getBookDataSourceUrl();
                if (StringUtils.isBlank(bookSpreadsheetId)) {
                    messagePanel.displayMsg("No book spreadsheet configuration found. Orders will be exported to product sheet", LOGGER, InformationLevel.Negative);
                    bookSpreadsheetId = spreadsheetIds.get(0);
                }

                fillOrders(bookSpreadsheetId, bookOrders);

                messagePanel.displayMsg(bookOrders.size() + " book order(s) exported.", LOGGER);
            }

            if (CollectionUtils.isNotEmpty(productOrders)) {
                String productSpreadsheetId = Settings.load().getConfigByCountry(country).getProductDataSourceUrl();
                if (StringUtils.isBlank(productSpreadsheetId)) {
                    messagePanel.displayMsg("No product spreadsheet configuration found. Orders will be exported to book sheet", LOGGER, InformationLevel.Negative);
                    productSpreadsheetId = spreadsheetIds.get(0);
                }

                fillOrders(productSpreadsheetId, productOrders);
                messagePanel.displayMsg(productOrders.size() + " product order(s) exported.", LOGGER);
            }
        }
    }

    public void fillOrders(String spreadsheetId, List<Order> orders) {

        if (CollectionUtils.isEmpty(orders)) {
            return;
        }

        Date date = now.get();
        String sheetName = createOrGetOrderSheet(spreadsheetId, date);

        List<List<Object>> values = convertOrdersToRangeValues(orders, spreadsheetId, sheetName);

        try {
            this.spreadsheetValuesAppend(spreadsheetId, sheetName, new ValueRange().setValues(values));
        } catch (BusinessException e) {
            throw new BusinessException(e);
        }
    }


    private static final Map<String, Field> ORDER_FIELDS_CACHE = new HashMap<>();

    public List<List<Object>> convertOrdersToRangeValues(List<Order> orders, String destSpreadId, String sheetName) {
        List<List<Object>> values = new ArrayList<>();
        orders.forEach(order -> {
            Object[] row = new String[OrderEnums.OrderColumn.values().length];
            int col = 0;
            for (OrderEnums.OrderColumn orderColumn : OrderEnums.OrderColumn.values()) {
                int index = orderColumn.index();
                try {

                    String fieldName = orderColumn.name().toLowerCase();
                    Field filed = ORDER_FIELDS_CACHE.computeIfAbsent(fieldName, s -> {
                        try {
                            return Order.class.getDeclaredField(s);
                        } catch (NoSuchFieldException e) {
                            throw new BusinessException(e);
                        }
                    });

                    try {
                        row[index] = filed.get(order);
                    } catch (IllegalAccessException e) {
                        throw new BusinessException(e);
                    }

                } catch (Exception e) {
                    row[index] = "";
                    //LOGGER.error("Error getting {} value, Order # {}. {}", colName, order.order_id, e.getMessage());
                }
            }

            values.add(Arrays.asList(row));
        });

        return values;
    }


    public String createOrGetOrderSheet(String spreadsheetId, Date date) {
        return createNewSheetIfNotExisted(spreadsheetId, SheetUtils.getSheetNameByDate(date), TEMPLATE_SHEET_NAME);
    }

    //todo check if template sheet has correct format
    public String createNewSheetIfNotExisted(String spreadsheetId, String sheetName, String templateSheetName) {
        long start = System.currentTimeMillis();

        //check if existed
        try {
            getSheetProperties(spreadsheetId, sheetName);
            LOGGER.info("Sheet {} already created.", sheetName);
            return sheetName;
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
            duplicateSheet(spreadsheetId, templateSheetId, sheetName);
            LOGGER.info("Sheet {} created successfully, took {}.", sheetName, Strings.formatElapsedTime(start));
            return sheetName;
        } catch (Exception e) {
            LOGGER.error("Fail to copy template sheet  {} {} {}", spreadsheetId, templateSheetName, sheetName, e);
            throw new BusinessException(e);
        }
    }
}
