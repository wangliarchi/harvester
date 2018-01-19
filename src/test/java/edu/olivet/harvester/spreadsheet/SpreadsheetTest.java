package edu.olivet.harvester.spreadsheet;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Guice(modules = {MockDateModule.class, MockDBModule.class})
public class SpreadsheetTest extends BaseTest {


    @Test
    public void testGetSpreadsheetType() throws Exception {

        //us book
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);

        spreadsheet.setSpreadsheetId(spreadsheetId);
        assertEquals(spreadsheet.getSpreadsheetType(), OrderEnums.OrderItemType.BOOK);

        //ca product
        assertEquals(appScript.getSpreadsheet("17k9ohj5RTCeMKKbpEbBb7azB4u3yZ3aHs1FfYTPaAMo").getSpreadsheetType(),
                OrderEnums.OrderItemType.PRODUCT);

    }

    @Test
    public void testGetSpreadsheetCountry() throws Exception {
        //us book
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        assertEquals(spreadsheet.getSpreadsheetCountry(), Country.US);

        //ca product
        assertEquals(appScript.getSpreadsheet("17k9ohj5RTCeMKKbpEbBb7azB4u3yZ3aHs1FfYTPaAMo").getSpreadsheetCountry(), Country.CA);

    }

    @Test
    public void testGetSheetNames() throws Exception {

        //us book
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        assertTrue(spreadsheet.getSheetNames().containsAll(Arrays.asList("Daily Cost", "confirm", "Individual Orders", "09/21", "09/14")));
    }

}