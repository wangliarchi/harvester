package edu.olivet.harvester.export.service;

import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.service.OrderItemTypeHelper;
import edu.olivet.harvester.spreadsheet.service.AppScript;
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
    private static final String TEMPLATE_SHEET_NAME = "Template";
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetService.class);

    @Inject
    private OrderItemTypeHelper orderItemTypeHelper;
    @Inject
    Now now;

    @Inject AppScript appScript;


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
        fillOrders(spreadsheetId, orders, 0);
    }

    public void fillOrders(String spreadsheetId, List<Order> orders, int repeatTime) {

        if (CollectionUtils.isEmpty(orders)) {
            return;
        }


        if (repeatTime >= Constants.MAX_REPEAT_TIMES) {
            return;
        }

        Date date = now.get();
        SheetProperties sheetProperties = createOrGetOrderSheet(spreadsheetId, date);
        String sheetName = sheetProperties.getTitle();

        //lock sheet
        int protectedId = lockSheet(spreadsheetId, sheetProperties.getSheetId(), "Order exporting process is running.");
        int lastRow = getLastRow(spreadsheetId, sheetName);
        try {
            List<List<Object>> values = convertOrdersToRangeValues(orders, spreadsheetId, sheetName);
            this.spreadsheetValuesAppend(spreadsheetId, sheetName, new ValueRange().setValues(values));
        } catch (Exception e) {
            throw new BusinessException(e);
        } finally {
            unlockSheet(spreadsheetId, protectedId);
        }

        List<Order> missedOrders = checkMissedOrders(orders, lastRow, spreadsheetId, sheetProperties);
        if (CollectionUtils.isNotEmpty(missedOrders)) {
            fillOrders(spreadsheetId, missedOrders, repeatTime + 1);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public int getLastRow(String spreadsheetId, String sheetName) {
        //read current orders
        List<Order> currentOrders;
        try {
            currentOrders = appScript.readOrders(spreadsheetId, sheetName);
        } catch (Exception e) {
            currentOrders = new ArrayList<>();
        }
        return CollectionUtils.isEmpty(currentOrders) ? 2 : currentOrders.stream().mapToInt(Order::getRow).max().getAsInt();
    }

    public List<Order> checkMissedOrders(List<Order> orders, int lastRow, String spreadsheetId, SheetProperties sheetProperties) {
        //read date from order sheet, double check if entered correctly
        String sheetName = sheetProperties.getTitle();
        List<Order> ordersOnSheet = appScript.readOrders(spreadsheetId, sheetName);
        ordersOnSheet.removeIf(it -> it.row <= lastRow);
        Map<String, List<Order>> orderMap = new HashMap<>();
        ordersOnSheet.forEach(it -> {
            List<Order> os = orderMap.getOrDefault(it.order_id, new ArrayList<>());
            os.add(it);
            orderMap.put(it.order_id, os);
        });

        //LOGGER.info("{} , {}", orders, orderMap);

        List<Order> missedOrders = new ArrayList<>();
        for (Order order : orders) {
            if (!orderMap.containsKey(order.order_id)) {
                LOGGER.info("Cant find order " + order.order_id + " on sheet " + sheetName);
                missedOrders.add(order);
                continue;
            }

            List<Order> os = orderMap.get(order.order_id);
            Order found = null;
            for (Order o : os) {
                if (order.equals(o)) {
                    found = o;
                    break;
                }
            }

            if (found == null) {
                LOGGER.info("Cant find order " + order.order_id + " on sheet " + sheetName);
                missedOrders.add(order);
            } else {
                os.remove(found);
                orderMap.put(order.order_id, os);
            }
        }
        //check and delete rest records in orderMap.
        deleteBrokenRows(orderMap, spreadsheetId, sheetProperties);

        return missedOrders;
    }


    private void deleteBrokenRows(Map<String, List<Order>> orderMap, String spreadsheetId, SheetProperties sheetProperties) {

        orderMap.forEach((k, os) -> {
            if (CollectionUtils.isNotEmpty(os)) {
                os.forEach(o -> {
                    LOGGER.info("deleting row {} from {} {}", o.row, spreadsheetId, sheetProperties.getTitle());
                    try {
                        deleteRow(spreadsheetId, sheetProperties.getSheetId(), o.row);
                    } catch (Exception e) {
                        LOGGER.error("", e);
                    }
                });
            }
        });
    }

    private static final Map<String, Field> ORDER_FIELDS_CACHE = new HashMap<>();

    public List<List<Object>> convertOrdersToRangeValues(List<Order> orders, String destSpreadId, String sheetName) {
        List<List<Object>> values = new ArrayList<>();
        orders.forEach(order -> {
            Object[] row = new String[OrderEnums.OrderColumn.values().length];
            for (OrderEnums.OrderColumn orderColumn : OrderEnums.OrderColumn.values()) {
                int index = orderColumn.index();
                try {

                    String fieldName = orderColumn.name().toLowerCase();
                    Field filed = ORDER_FIELDS_CACHE.computeIfAbsent(fieldName, s -> {
                        try {
                            return Order.class.getDeclaredField(s);

                        } catch (Exception e) {
                            return null;
                        }
                    });

                    try {
                        row[index] = filed.get(order);

                    } catch (Exception e) {
                        row[index] = StringUtils.EMPTY;
                    }

                } catch (Exception e) {
                    row[index] = StringUtils.EMPTY;
                }
            }

            values.add(Arrays.asList(row));
        });

        return values;
    }


    public SheetProperties createOrGetOrderSheet(String spreadsheetId, Date date) {
        return createNewSheetIfNotExisted(spreadsheetId, SheetUtils.getSheetNameByDate(date), TEMPLATE_SHEET_NAME);
    }

    //todo check if template sheet has correct format
    public SheetProperties createNewSheetIfNotExisted(String spreadsheetId, String sheetName, String templateSheetName) {
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

}
