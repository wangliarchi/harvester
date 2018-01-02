package edu.olivet.harvester.spreadsheet.model;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Guice(modules = {MockDateModule.class})
public class WorksheetTest {
    @Inject Now now;

    @Test
    public void getOrderConfirmationDate() {
        Spreadsheet spreadsheet = new Spreadsheet();
        spreadsheet.setSpreadsheetCountry(Country.US);
        String sheetName = "12/31";
        now.set(Dates.parseDate("01/01/2018"));
        Worksheet worksheet = new Worksheet(spreadsheet, sheetName);
        assertEquals(worksheet.getOrderConfirmationDate(),"2017-12-31");

        sheetName = "01/01";
        worksheet = new Worksheet(spreadsheet, sheetName);
        assertEquals(worksheet.getOrderConfirmationDate(),"2018-01-01");

    }

}