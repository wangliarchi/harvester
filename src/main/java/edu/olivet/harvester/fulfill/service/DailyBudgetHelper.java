package edu.olivet.harvester.fulfill.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.fulfill.exception.Exceptions.NoBudgetException;
import edu.olivet.harvester.fulfill.exception.Exceptions.OutOfBudgetException;
import edu.olivet.harvester.utils.common.NumberUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 11:33 AM
 */
@Singleton
public class DailyBudgetHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyBudgetHelper.class);

    @Inject
    private SheetService sheetService;
    @Inject private
    Now now;

    private Map<String, Float> budgetMap = new ConcurrentHashMap<>();
    private Map<String, Float> spendingMap = new ConcurrentHashMap<>();

    private Map<String, List<RuntimePanelObserver>> runtimePanelObservers = new ConcurrentHashMap<>();
    private Map<String, AtomicDouble> costBuffer = new ConcurrentHashMap<>();

    public void addRuntimePanelObserver(String spreadsheetId, RuntimePanelObserver runtimePanelObserver) {
        List<RuntimePanelObserver> observers = runtimePanelObservers.getOrDefault(spreadsheetId, new ArrayList<>());
        observers.add(runtimePanelObserver);
        runtimePanelObservers.put(spreadsheetId, observers);

        runtimePanelObserver.updateBudget(Float.toString(NumberUtils.round(budgetMap.getOrDefault(spreadsheetId, 0.0f), 2)));
        runtimePanelObserver.updateSpending(Float.toString(NumberUtils.round(spendingMap.getOrDefault(spreadsheetId, 0.0f), 2)));

    }

    private static final Map<String, Integer> BUDGET_ROW_CACHE = new HashMap<>();

    public Float checkBudget(String spreadsheetId) {
        return getRemainingBudget(spreadsheetId, now.get(), 0);
    }

    public synchronized Float getRemainingBudget(String spreadsheetId, Date date, float costToSpend) {
        //Map<String, Float> budgetData = getData(spreadsheetId, date);
        Map<DataType, Float> budgetData = getData(spreadsheetId, date);

        if (budgetData.get(DataType.Budget) <= 0) {
            throw new NoBudgetException("Today's budget has not been entered yet. Please fill in 'Daily Cost' sheet.");
        }

        float costInBuffer = 0;
        if (costBuffer.containsKey(spreadsheetId)) {
            costInBuffer = costBuffer.get(spreadsheetId).floatValue();
        }

        Float remaining = budgetData.get(DataType.Budget) - budgetData.get(DataType.Cost) - costInBuffer;


        if (remaining <= 0) {
            throw new OutOfBudgetException("You have exceed today's budget limit. ");
        }

        if (remaining < costToSpend) {
            throw new OutOfBudgetException("You don't have enough fund to process this order. Need $" +
                    costToSpend + ", only have $" + String.format("%.2f", remaining));
        }

        costBuffer.put(spreadsheetId, new AtomicDouble(costToSpend + costInBuffer));
        return remaining;
    }


    public synchronized Map<DataType, Float> getData(String spreadsheetId, Date date) {
        List<String> ranges = Lists.newArrayList("Daily Cost");
        List<ValueRange> valueRanges = sheetService.batchGetSpreadsheetValues(spreadsheetId, ranges);
        int rowNo = 2;

        Map<DataType, Float> budgetData = new HashMap<>();
        budgetData.put(DataType.Budget, 0f);
        budgetData.put(DataType.Cost, 0f);
        for (ValueRange valueRange : valueRanges) {
            if (valueRange.getValues() == null) {
                continue;
            }
            List<List<Object>> data = Lists.reverse(valueRange.getValues());
            rowNo = data.size();
            for (List<Object> rowData : data) {
                if (CollectionUtils.isNotEmpty(rowData) && rowData.size() > 0) {
                    String dateStr = rowData.get(0).toString();
                    if (StringUtils.isNotBlank(dateStr) && !"Date".equalsIgnoreCase(dateStr)) {
                        try {
                            Date rowDate = Dates.parseDate(rowData.get(0).toString());
                            if (dateToGoogleSheetName(date).equals(dateToGoogleSheetName(rowDate))) {
                                BUDGET_ROW_CACHE.put(spreadsheetId + dateToGoogleSheetName(date), rowNo);
                                float totalCost = FloatUtils.parseFloat(rowData.get(1).toString(), 0);
                                float budget = FloatUtils.parseFloat(rowData.get(2).toString(), 0);
                                budgetData.put(DataType.Budget, budget);
                                budgetData.put(DataType.Cost, totalCost);
                                budgetMap.put(spreadsheetId, budget);
                                spendingMap.put(spreadsheetId, totalCost);

                                if (runtimePanelObservers.containsKey(spreadsheetId)) {
                                    runtimePanelObservers.get(spreadsheetId).forEach(runtimePanelObserver -> {
                                        if (runtimePanelObserver != null) {
                                            runtimePanelObserver.updateSpending(Float.toString(NumberUtils.round(totalCost, 2)));
                                            runtimePanelObserver.updateBudget(Float.toString(NumberUtils.round(budget, 2)));
                                        }
                                    });
                                }
                                return budgetData;
                            }
                        } catch (Exception e) {
                            LOGGER.error("", e);
                        }
                    }
                }
                rowNo--;
            }
            rowNo = data.size();
        }

        //not found. create today's record
        createBudgetRow(spreadsheetId, date);
        BUDGET_ROW_CACHE.put(spreadsheetId + dateToGoogleSheetName(date), rowNo);
        return budgetData;
    }

    public Float getCost(String spreadsheetId, String date) {
        return getCost(spreadsheetId, Dates.parseDate(date));
    }

    public enum DataType {
        Cost,
        Budget
    }

    public Float getCost(String spreadsheetId, Date date) {
        Map<DataType, Float> budgetData = getData(spreadsheetId, date);
        return budgetData.get(DataType.Cost);
    }


    public void createBudgetRow(String spreadsheetId, Date date) {
        String dateString = dateToGoogleSheetName(date);
        String sheetName = "Daily Cost";

        Object[] row = new String[3];
        row[0] = dateString;
        row[1] = "0";
        row[2] = "0";
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList(row));

        try {
            sheetService.spreadsheetValuesAppend(spreadsheetId, sheetName, new ValueRange().setValues(values));
        } catch (BusinessException e) {
            throw new BusinessException(e);
        }
    }


    public void addSpending(String spreadsheetId, String date, float spending) {
        addSpending(spreadsheetId, Dates.parseDate(date), spending);
    }


    //@Inject OrderFulfillmentRecordService orderFulfillmentRecordService;

    public synchronized void addSpending(String spreadsheetId, Date date, float spending) {
        float cost = getCost(spreadsheetId, date);
        cost += spending;

        //float localTotalCost = orderFulfillmentRecordService.totalCost(spreadsheetId, date);

        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        if (runtimePanelObservers.containsKey(spreadsheetId)) {
            float finalCost = cost;
            runtimePanelObservers.get(spreadsheetId).forEach(runtimePanelObserver -> {
                if (runtimePanelObserver != null) {
                    runtimePanelObserver.updateSpending(df.format(finalCost));
                }
            });
        }

        int row = BUDGET_ROW_CACHE.getOrDefault(spreadsheetId + dateToGoogleSheetName(date), 2);

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange valueRange = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(df.format(cost))))
                .setRange(String.format("%s!B%d", "Daily Cost", row));
        dateToUpdate.add(valueRange);

        try {
            sheetService.batchUpdateValues(spreadsheetId, dateToUpdate);
            LOGGER.info("Successfully updated spending {}， now total spent {}", df.format(spending), df.format(cost));
            AtomicDouble costInBuffer = costBuffer.getOrDefault(spreadsheetId, null);
            if (costInBuffer != null) {
                costBuffer.put(spreadsheetId, new AtomicDouble(costInBuffer.floatValue() - spending));
            }
        } catch (BusinessException e) {
            LOGGER.error("Fail to update cost error msg {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }


    public void updateBudget(String spreadsheetId, Date date, float budget) {

        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);

        int row = BUDGET_ROW_CACHE.getOrDefault(spreadsheetId + dateToGoogleSheetName(date), 2);

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange valueRange = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(df.format(budget))))
                .setRange(String.format("%s!C%d", "Daily Cost", row));
        dateToUpdate.add(valueRange);

        try {
            sheetService.batchUpdateValues(spreadsheetId, dateToUpdate);
        } catch (BusinessException e) {
            LOGGER.error("Fail to update budget error msg {} - {}", spreadsheetId, e);
            throw new BusinessException(e);
        }
    }

    public static String dateToGoogleSheetName(Date date) {
        return Dates.format(date, "MM/dd");
    }

    public static void main(String[] args) {
        DailyBudgetHelper dailyBudgetHelper = ApplicationContext.getBean(DailyBudgetHelper.class);

        String[] sheetIds = new String[] {
                "1wRtULwAq5sNRzPI9Zi-eEJnlLHpsbK2i2uvpCiDg010",
                "1JTotAIBXQGWFkT0lnMZ_Rrbmr5Nn5zrF9VxLlLKDG94",
                "1QQxg-h_Z7y7JFrA1jqgt96INMyFLdXQ9bnxteE3Rd5w",
                "1J6CqNKoSfw3ERNWTLXVYh3R37a11kB4nBEzySIypV68",
                "1i-3FSFOFZ5mNP87Lz4YX4W3QIUILQNLguc0itldR0kc",
                "1ra4jDYLcvedRqA-oP0sStVvQm9dwDrMXts3z0FfGgmA",
                "1tP6pGFZLB5Q1PtpBokxXB8gQdd8T8Z4Z-PKnbeMfT78",
                "1Kn8PgppidUuOwYfayR5lc2lPtQihB1a-If_YaKU1Ofs",
                "1wRtULwAq5sNRzPI9Zi-eEJnlLHpsbK2i2uvpCiDg010",
                "1JTotAIBXQGWFkT0lnMZ_Rrbmr5Nn5zrF9VxLlLKDG94",
                "1QQxg-h_Z7y7JFrA1jqgt96INMyFLdXQ9bnxteE3Rd5w",
                "1J6CqNKoSfw3ERNWTLXVYh3R37a11kB4nBEzySIypV68",
                "1i-3FSFOFZ5mNP87Lz4YX4W3QIUILQNLguc0itldR0kc",
                "1ra4jDYLcvedRqA-oP0sStVvQm9dwDrMXts3z0FfGgmA",
                "1tP6pGFZLB5Q1PtpBokxXB8gQdd8T8Z4Z-PKnbeMfT78",
                "1Kn8PgppidUuOwYfayR5lc2lPtQihB1a-If_YaKU1Ofs",
                "1wRtULwAq5sNRzPI9Zi-eEJnlLHpsbK2i2uvpCiDg010",
                "1JTotAIBXQGWFkT0lnMZ_Rrbmr5Nn5zrF9VxLlLKDG94",
                "1QQxg-h_Z7y7JFrA1jqgt96INMyFLdXQ9bnxteE3Rd5w",
                "1J6CqNKoSfw3ERNWTLXVYh3R37a11kB4nBEzySIypV68",
                "1i-3FSFOFZ5mNP87Lz4YX4W3QIUILQNLguc0itldR0kc",
                "1ra4jDYLcvedRqA-oP0sStVvQm9dwDrMXts3z0FfGgmA",
                "1tP6pGFZLB5Q1PtpBokxXB8gQdd8T8Z4Z-PKnbeMfT78",
                "1Kn8PgppidUuOwYfayR5lc2lPtQihB1a-If_YaKU1Ofs",
                "1wRtULwAq5sNRzPI9Zi-eEJnlLHpsbK2i2uvpCiDg010",
                "1JTotAIBXQGWFkT0lnMZ_Rrbmr5Nn5zrF9VxLlLKDG94",
                "1QQxg-h_Z7y7JFrA1jqgt96INMyFLdXQ9bnxteE3Rd5w",
                "1J6CqNKoSfw3ERNWTLXVYh3R37a11kB4nBEzySIypV68",
                "1i-3FSFOFZ5mNP87Lz4YX4W3QIUILQNLguc0itldR0kc",
                "1ra4jDYLcvedRqA-oP0sStVvQm9dwDrMXts3z0FfGgmA",
                "1tP6pGFZLB5Q1PtpBokxXB8gQdd8T8Z4Z-PKnbeMfT78",
                "1Kn8PgppidUuOwYfayR5lc2lPtQihB1a-If_YaKU1Ofs"
        };

        Map<String, Float> spendings = new HashMap<>();
        for (String sheetId : sheetIds) {
            spendings.put(sheetId, 0f);
        }
        for (int i = 0; i < 32; i++) {
            int finalI = i;
            new Thread(() -> {
                LOGGER.info("Thread " + finalI + " started");
                String sheetId = sheetIds[finalI];
                while (true) {
                    try {
                        dailyBudgetHelper.checkBudget(sheetId);
                        WaitTime.Shorter.execute();
                        float cost = (float) (10.1f + 0.1 * finalI);
                        float remainingBudget = dailyBudgetHelper.getRemainingBudget(sheetId, new Date(), cost);
                        LOGGER.info("{} remains {}", finalI, remainingBudget);
                        WaitTime.Shorter.execute();
                        if (remainingBudget < cost) {
                            throw new BusinessException("remaining budget not enough " + remainingBudget);
                        }
                        dailyBudgetHelper.addSpending(sheetId, new Date(), cost);
                        spendings.put(sheetId, spendings.get(sheetId) + cost);
                        LOGGER.info("{} total local spendings {}", finalI, spendings.get(sheetId));
                    } catch (Exception e) {
                        LOGGER.error("", e);
                        break;
                    }
                }
            }).start();
        }


//        String spreadsheetId = "1Kn8PgppidUuOwYfayR5lc2lPtQihB1a-If_YaKU1Ofs";
//        DailyBudgetHelper dailyBudgetHelper = ApplicationContext.getBean(DailyBudgetHelper.class);
//        float remainingBudget = dailyBudgetHelper.getRemainingBudget(spreadsheetId, Dates.parseDate("08/10/2017"));
//        System.out.println(remainingBudget);
//
//        remainingBudget = dailyBudgetHelper.getRemainingBudget(spreadsheetId, Dates.parseDate("11/10"));
//        System.out.println(remainingBudget);
//
//        float cost = dailyBudgetHelper.getCost(spreadsheetId, "11/10");
//        System.out.println(cost);
//
//        dailyBudgetHelper.addSpending(spreadsheetId, "11/10", 50);
//        cost = dailyBudgetHelper.getCost(spreadsheetId, "11/10");
//        System.out.println(cost);
//
//        cost = dailyBudgetHelper.getCost(spreadsheetId, "11/02");
//        System.out.println(cost);
//
//        for (int i = 1; i < 20; i++) {
//            int finalI = i;
//            new Thread(() -> {
//                LOGGER.info("Thread " + finalI + " started");
//                dailyBudgetHelper.addSpending(spreadsheetId, new Date(), 10 * finalI);
//            }).start();
//        }
    }

}
