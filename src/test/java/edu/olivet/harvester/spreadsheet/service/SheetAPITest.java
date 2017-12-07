package edu.olivet.harvester.spreadsheet.service;

import com.google.inject.Inject;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/11/17 2:38 PM
 */
@Guice(modules = {MockDBModule.class,MockDateModule.class})
public class SheetAPITest {

    @Inject
    private SheetAPI sheetAPI;

    @Inject
    private  AppScript appScript;

    @Test
    public void testGetSpreadsheet() throws Exception {
    }

    @Test
    public void testBatchGetSpreadsheetValues() throws Exception {
    }

    @Test
    public void testBatchUpdate() throws Exception {
    }

    @Test
    public void testBatchUpdateValues() throws Exception {
    }

    @Test
    public void testSpreadsheetValuesAppend() throws Exception {
    }

    @Test
    public void testGetSheetProperties() throws Exception {
    }

    @Test
    public void testMarkGrayOrders() throws Exception {
    }

    @Test
    public void testMarkBuyerCancelOrders() throws Exception {
        String spreadsheetId = "1JTotAIBXQGWFkT0lnMZ_Rrbmr5Nn5zrF9VxLlLKDG94";
        String sheetName = "10/11";

        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        Worksheet worksheet = new Worksheet(spreadsheet, sheetName);

        List<Order> orders = appScript.readOrders(spreadsheetId, sheetName);

        //only mark row 3
        orders.removeIf(order -> order.row != 3);

        sheetAPI.markBuyerCancelOrders(orders,worksheet);
    }

}