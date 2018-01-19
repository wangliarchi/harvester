package edu.olivet.harvester.feeds.helper;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.Assert.assertEquals;


public class ShipDateUtilsTest extends BaseTest {
    @Inject private ShipDateUtils shipDateUtils;

    private Spreadsheet spreadsheet;

    @BeforeClass
    public void init() {
        String spreadsheetId = "1qxcCkAPvvBaR3KHa2MZv1V39m2E1IMytVDn1yXDaVEM";
        spreadsheet = appScript.getSpreadsheet(spreadsheetId);
        spreadsheet.setSpreadsheetCountry(Country.US);
    }

    @Test
    public void testGetSheetNameByDate() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        long date;

        date = formatter.parse("09/28/2017").getTime();
        assertEquals(shipDateUtils.getSheetNameByDate(date), "09/28");

        date = formatter.parse("09/08/2017").getTime();
        assertEquals(shipDateUtils.getSheetNameByDate(date), "09/08");

        date = formatter.parse("10/28/2017").getTime();
        assertEquals(shipDateUtils.getSheetNameByDate(date), "10/28");

        date = formatter.parse("10/08/2017").getTime();
        assertEquals(shipDateUtils.getSheetNameByDate(date), "10/08");
    }

    @Inject private Now now;

    @Test
    public void testGetShipDateString() {
        Order order = prepareOrder();
        now.set(new Date());

        order.purchase_date = "2018-01-07T18:41:02+00:00";
        order.expected_ship_date = "2018-01-11 2018-01-13";
        order.sheetName = "01/08";
        Worksheet worksheet = new Worksheet(spreadsheet, order.sheetName);
        String defaultDate = worksheet.getOrderConfirmationDate();
        assertEquals(shipDateUtils.getShipDateString(order, Dates.parseDate(defaultDate)),"2018-01-08T05:00:00Z");

    }
    @Test
    public void testGetShipDate() {

        Order order = prepareOrder();
        now.set(new Date());

        order.purchase_date = "2018-01-07T18:41:02+00:00";
        order.expected_ship_date = "2018-01-11 2018-01-13";
        order.sheetName = "01/08";
        Worksheet worksheet = new Worksheet(spreadsheet, order.sheetName);
        String defaultDate = worksheet.getOrderConfirmationDate();

        assertEquals(shipDateUtils.getShipDate(order,Dates.parseDate(defaultDate)),Dates.parseDate("01/08/2018"));



        order.expected_ship_date = "2017-11-22 2017-11-25";
        order.purchase_date = "2017-11-20T03:47:42+00:00";
        order.sheetName = "11/21";
        worksheet = new Worksheet(spreadsheet, order.sheetName);
        defaultDate = worksheet.getOrderConfirmationDate();

        assertEquals(shipDateUtils.getShipDate(order,Dates.parseDate(defaultDate)),Dates.parseDate("11/21/2017"));

        //如果sheet date 比latest expected shipping date 晚，使用 earliest expected shipping date
        order.sheetName = "11/25";
        order.expected_ship_date = "2017-11-22 2017-11-25";
        worksheet = new Worksheet(spreadsheet, order.sheetName);
        defaultDate = worksheet.getOrderConfirmationDate();
        assertEquals(shipDateUtils.getShipDate(order,Dates.parseDate(defaultDate)),Dates.parseDate("11/22/2017"));

        //如果purchase date 比sheet date 晚， 使用purchase date
        order.sheetName = "11/21";
        order.expected_ship_date = "2017-11-22 2017-11-25";
        order.purchase_date = "2017-11-22T08:47:42+00:00";
        worksheet = new Worksheet(spreadsheet, order.sheetName);
        defaultDate = worksheet.getOrderConfirmationDate();
        assertEquals(shipDateUtils.getShipDate(order,Dates.parseDate(defaultDate)),Dates.parseDate("11/22/2017"));
    }
}