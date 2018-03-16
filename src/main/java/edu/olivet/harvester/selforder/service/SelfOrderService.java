package edu.olivet.harvester.selforder.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.utils.SelfOrderHelper;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/2/2018 6:04 AM
 */
public class SelfOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfOrderService.class);

    @Inject SheetService sheetService;
    @Inject SelfOrderHelper selfOrderHelper;


    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, String sheetName) {
        return fetchSelfOrders(spreadsheetId, Lists.newArrayList(sheetName + "!A1:AZ"));
    }

    public List<SelfOrder> fetchSelfOrders(String spreadsheetId, List<String> ranges) {

        List<ValueRange> valueRanges;
        try {
            valueRanges = sheetService.batchGetSpreadsheetValues(spreadsheetId, ranges);
        } catch (BusinessException e) {
            LOGGER.error("error get batch sheet values for {} ranges {}. {}", spreadsheetId, ranges, e);
            throw e;
        }

        List<SelfOrder> selfOrders = new ArrayList<>();
        String sid = Settings.load().getSid();
        //loop by sheet
        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> rows = valueRange.getValues();
            String a1Notation = valueRange.getRange();
            LOGGER.warn("{}->{}", spreadsheetId, StringUtils.defaultString(a1Notation, "!No A1notation!"));

            if (CollectionUtils.isEmpty(rows)) {
                LOGGER.warn("{}->{}读取不到任何有效数据", spreadsheetId, StringUtils.defaultString(a1Notation, "!No A1notation!"));
                continue;
            }

            int start = a1Notation.contains("'") ? 1 : 0;
            String sheetName = a1Notation.substring(start, a1Notation.indexOf("!") - start);
            int starRow = 1;
            List<SelfOrder> ordersForSheet = new ArrayList<>();
            int row = 0;
            for (List<Object> objects : rows) {
                row++;
                if (row <= starRow) {
                    continue;
                }
                if (CollectionUtils.isEmpty(objects)) {
                    //LOGGER.info("Row {} is empty for {} {}.", row, spreadTitle, sheetName);
                    continue;
                }
                if (StringUtils.isBlank(objects.get(0).toString())) {
                    continue;
                }
                List<String> columns = new ArrayList<>();
                objects.forEach(value -> columns.add(value.toString()));

                SelfOrder selfOrder = new SelfOrder();
                int maxCol = columns.size();
                for (int j = 1; j <= maxCol; j++) {
                    try {
                        selfOrderHelper.setColumnValue(j, columns.get(j - 1), selfOrder);
                    } catch (Exception e) {
                        //
                    }
                }

                if (!sid.equalsIgnoreCase(selfOrder.buyerAccountCode)) {
                    continue;
                }

                selfOrder.setRow(row);
                selfOrder.setSpreadsheetId(spreadsheetId);
                selfOrder.setSheetName(sheetName);
                ordersForSheet.add(selfOrder);
            }

            LOGGER.info("{}->{} found {} orders", spreadsheetId, sheetName, ordersForSheet.size());

            selfOrders.addAll(ordersForSheet);
        }

        return selfOrders;
    }

    public static void main(String[] args) {
        String spreadsheetId = SystemSettings.reload().getSelfOrderSpreadsheetId();
        SelfOrderService selfOrderService = ApplicationContext.getBean(SelfOrderService.class);
        List<SelfOrder> selfOrders = selfOrderService.fetchSelfOrders(spreadsheetId, "03/01");
        System.out.println(selfOrders);
    }
}
