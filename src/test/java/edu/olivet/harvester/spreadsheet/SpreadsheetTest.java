package edu.olivet.harvester.spreadsheet;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import lombok.Getter;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

@Guice
public class SpreadsheetTest {

    @Inject
    private AppScript appsScript;

    @Test
    public void testGetSpreadsheetType() throws Exception {

        //us book
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appsScript.getSpreadsheet(spreadsheetId);

        spreadsheet.setSpreadsheetId(spreadsheetId);
        assertEquals(spreadsheet.getSpreadsheetType(), OrderEnums.OrderItemType.BOOK);

        //ca product
        spreadsheet.setSpreadsheetId("17k9ohj5RTCeMKKbpEbBb7azB4u3yZ3aHs1FfYTPaAMo");
        assertEquals(spreadsheet.getSpreadsheetType(), OrderEnums.OrderItemType.PRODUCT);

    }

    @Test
    public void testGetSpreadsheetCountry() throws Exception {
        //us book
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appsScript.getSpreadsheet(spreadsheetId);
        assertEquals(spreadsheet.getSpreadsheetCountry(), Country.US);

        //ca product
        spreadsheet.setSpreadsheetId("17k9ohj5RTCeMKKbpEbBb7azB4u3yZ3aHs1FfYTPaAMo");
        assertEquals(spreadsheet.getSpreadsheetCountry(), Country.CA);

    }

    @Test
    public void testGetSheetNames() throws Exception {

        //us book
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appsScript.getSpreadsheet(spreadsheetId);
        assertTrue(spreadsheet.getSheetNames().containsAll(Arrays.asList("Daily Cost", "confirm", "Individual Orders", "09/21", "09/14")));
    }

}