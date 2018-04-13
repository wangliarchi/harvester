package edu.olivet.harvester.spreadsheet.service;

import com.google.inject.Inject;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/11/17 2:38 PM
 */
@Guice(modules = {MockDBModule.class, MockDateModule.class})
public class SheetAPITest {

    @Inject
    private SheetAPI sheetAPI;

    @Inject
    private AppScript appScript;

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

        sheetAPI.markBuyerCancelOrders(orders, worksheet);
    }

    @Test
    public void testSpreadsheetName() {
        String str = "!!!TEST ONLY -  ACC 710US Book Order_Update 704";
        String regex = "([^0-9]%s|^%s)($|[^0-9])";

        String accSid = "710";
        String sidRegex = String.format(regex, accSid, accSid);
        assertTrue(RegexUtils.containsRegex(str, sidRegex));

        accSid = "704";
        sidRegex = String.format(regex, accSid, accSid);
        assertTrue(RegexUtils.containsRegex(str, sidRegex));

        accSid = "71";
        sidRegex = String.format(regex, accSid, accSid);
        assertFalse(RegexUtils.containsRegex(str, sidRegex));

        accSid = "10";
        sidRegex = String.format(regex, accSid, accSid);
        assertFalse(RegexUtils.containsRegex(str, sidRegex));

        accSid = "70";
        sidRegex = String.format(regex, accSid, accSid);
        assertFalse(RegexUtils.containsRegex(str, sidRegex));

        accSid = "04";
        sidRegex = String.format(regex, accSid, accSid);
        assertFalse(RegexUtils.containsRegex(str, sidRegex));
    }

}