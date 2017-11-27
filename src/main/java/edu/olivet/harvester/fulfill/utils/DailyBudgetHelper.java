package edu.olivet.harvester.fulfill.utils;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.ui.RuntimeSettingsPanel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 11:33 AM
 */
@Singleton
public class DailyBudgetHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyBudgetHelper.class);

    @Inject
    SheetService sheetService;


    public Float getRemainingBudget(String spreadsheetId, String dateString) {
        Date date = Dates.parseDate(dateString);
        return getRemainingBudget(spreadsheetId, date);
    }

    private static final Map<String, Integer> BUDGET_ROW_CACHE = new HashMap<>();

    public Float getRemainingBudget(String spreadsheetId, Date date) {
        Map<String, Float> budgetData = getData(spreadsheetId, date);
        Float remaining = budgetData.get("budget") - budgetData.get("cost");

        if (budgetData.get("budget") <= 0) {
            throw new BusinessException("Today's budget has not been entered yet. Please fill in 'Daily Cost' sheet.");
        }

        if (remaining <= 0) {
            throw new BusinessException("You have exceed today's budget limit. ");
        }

        return remaining;
    }


    public Map<String, Float> getData(String spreadsheetId, Date date) {
        List<String> ranges = com.google.common.collect.Lists.newArrayList("Daily Cost");
        List<ValueRange> valueRanges = sheetService.bactchGetSpreadsheetValues(spreadsheetId, ranges);
        int rowNo = 2;

        Map<String, Float> budgetData = new HashMap<>();
        budgetData.put("budget", 0f);
        budgetData.put("cost", 0f);
        for (ValueRange valueRange : valueRanges) {
            List<List<Object>> data = com.google.common.collect.Lists.reverse(valueRange.getValues());
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

                                budgetData.put("budget", budget);
                                budgetData.put("cost", totalCost);
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
        //throw new BusinessException("Today's budget has not been entered yet. Please fill in 'Daily Cost' sheet.");
    }

    public Float getCost(String spreadsheetId, String date) {
        return getCost(spreadsheetId, Dates.parseDate(date));
    }


    public Float getBudget(String spreadsheetId, Date date) {


        int row = BUDGET_ROW_CACHE.getOrDefault(spreadsheetId + dateToGoogleSheetName(date), 2);

        List<String> ranges = com.google.common.collect.Lists.newArrayList("Daily Cost!C" + row);

        try {
            List<ValueRange> valueRanges = sheetService.bactchGetSpreadsheetValues(spreadsheetId, ranges);
            return FloatUtils.parseFloat(valueRanges.get(0).getValues().get(0).get(0).toString(), 0);
        } catch (Exception e) {
            return 0f;
        }
    }


    public Float getCost(String spreadsheetId, Date date) {

        int row = BUDGET_ROW_CACHE.getOrDefault(spreadsheetId + dateToGoogleSheetName(date), 2);

        List<String> ranges = com.google.common.collect.Lists.newArrayList("Daily Cost!B" + row);

        try {
            List<ValueRange> valueRanges = sheetService.bactchGetSpreadsheetValues(spreadsheetId, ranges);
            return FloatUtils.parseFloat(valueRanges.get(0).getValues().get(0).get(0).toString(), 0);
        } catch (Exception e) {
            return 0f;
        }
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

    public void addSpending(String spreadsheetId, Date date, float spending) {
        float cost = getCost(spreadsheetId, date);
        cost += spending;


        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);


        RuntimeSettingsPanel.getInstance().todayUsedTextField.setText(df.format(cost));

        int row = BUDGET_ROW_CACHE.getOrDefault(spreadsheetId + dateToGoogleSheetName(date), 2);

        List<ValueRange> dateToUpdate = new ArrayList<>();

        ValueRange valueRange = new ValueRange().setValues(Collections.singletonList(Lists.newArrayList(df.format(cost))))
                .setRange(String.format("%s!B%d", "Daily Cost", row));
        dateToUpdate.add(valueRange);

        try {
            sheetService.batchUpdateValues(spreadsheetId, dateToUpdate);
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


    //Budget($)

    public static String dateToGoogleSheetName(Date date) {
        return Dates.format(date, "MM/dd");
    }

    public static void main(String[] args) {
        String spreadsheetId = "1Kn8PgppidUuOwYfayR5lc2lPtQihB1a-If_YaKU1Ofs";
        DailyBudgetHelper dailyBudgetHelper = ApplicationContext.getBean(DailyBudgetHelper.class);
        float remainingBudget = dailyBudgetHelper.getRemainingBudget(spreadsheetId, "08/10/2017");
        System.out.println(remainingBudget);

        remainingBudget = dailyBudgetHelper.getRemainingBudget(spreadsheetId, "11/10");
        System.out.println(remainingBudget);

        float cost = dailyBudgetHelper.getCost(spreadsheetId, "11/10");
        System.out.println(cost);

        dailyBudgetHelper.addSpending(spreadsheetId, "11/10", 50);
        cost = dailyBudgetHelper.getCost(spreadsheetId, "11/10");
        System.out.println(cost);

        cost = dailyBudgetHelper.getCost(spreadsheetId, "11/02");
        System.out.println(cost);
    }

}
