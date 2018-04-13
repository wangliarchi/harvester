package edu.olivet.harvester.feeds.helper;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.common.DateFormat;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.testng.Assert.assertEquals;


public class ShipDateUtilsTest extends BaseTest {
    @Inject private ShipDateUtils shipDateUtils;

    //private Spreadsheet spreadsheet;

    @BeforeClass
    public void init() {
//        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
//        spreadsheet = appScript.getSpreadsheet(spreadsheetId);
//        spreadsheet.setSpreadsheetCountry(Country.US);
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
        Worksheet worksheet = new Worksheet(null, order.sheetName);
        String defaultDate = worksheet.getOrderConfirmationDate();
        assertEquals(shipDateUtils.getShipDateString(order, Dates.parseDate(defaultDate)), "2018-01-08T05:00:00Z");

    }

    @Test
    public void testGetShipDate() {

        Order order = prepareOrder();
        now.set(new Date());

        order.purchase_date = "2018-01-07T18:41:02+00:00";
        order.expected_ship_date = "2018-01-11 2018-01-13";
        order.sheetName = "01/08";
        Worksheet worksheet = new Worksheet(null, order.sheetName);
        String defaultDate = worksheet.getOrderConfirmationDate();

        assertEquals(shipDateUtils.getShipDate(order, Dates.parseDate(defaultDate)), Dates.parseDate("01/08/2018"));


        order.expected_ship_date = "2017-11-22 2017-11-25";
        order.purchase_date = "2017-11-20T03:47:42+00:00";
        order.sheetName = "11/21";
        worksheet = new Worksheet(null, order.sheetName);
        defaultDate = worksheet.getOrderConfirmationDate();

        assertEquals(shipDateUtils.getShipDate(order, Dates.parseDate(defaultDate)), Dates.parseDate("11/21/2017"));

        //如果sheet date 比latest expected shipping date 晚，使用 earliest expected shipping date
        order.sheetName = "11/25";
        order.expected_ship_date = "2017-11-22 2017-11-25";
        worksheet = new Worksheet(null, order.sheetName);
        defaultDate = worksheet.getOrderConfirmationDate();
        assertEquals(shipDateUtils.getShipDate(order, Dates.parseDate(defaultDate)), Dates.parseDate("11/22/2017"));

        //如果purchase date 比sheet date 晚， 使用purchase date
        order.sheetName = "11/21";
        order.expected_ship_date = "2017-11-22 2017-11-25";
        order.purchase_date = "2017-11-22T08:47:42+00:00";
        worksheet = new Worksheet(null, order.sheetName);
        defaultDate = worksheet.getOrderConfirmationDate();
        assertEquals(shipDateUtils.getShipDate(order, Dates.parseDate(defaultDate)), Dates.parseDate("11/22/2017"));
    }

    @Test
    public void testGetShipDatePST() {
        Order order = prepareOrder();
        now.set(new Date());

        order.purchase_date = "2018-02-19T18:15:33+00:00";
        order.expected_ship_date = "2018-02-20 2018-02-26";
        order.sheetName = "02/19";

        Worksheet worksheet = new Worksheet(null, order.sheetName);
        String defaultDate = worksheet.getOrderConfirmationDate();

        shipDateUtils.getShipDate(order, Dates.parseDate(defaultDate));
    }

    @Test
    public void testGetShipDateKST() {
        //April 6, 2018 1:27:04 PM KST"
        Order order = prepareOrder();
        now.set(Dates.parseDate("2018-04-06_00:33:22"));

        order.purchase_date = "2018-04-04_17:33:22";
        order.expected_ship_date = "2018-04-05 2018-04-06";
        order.sheetName = "04/06";

        Worksheet worksheet = new Worksheet(null, order.sheetName);
        String defaultDate = worksheet.getOrderConfirmationDate();
        Date nowDate = now.get();
        Date shipDate = shipDateUtils.getShipDate(order, Dates.parseDate(defaultDate));
        String dateString = shipDateUtils.getShipDateString(order, Dates.parseDate(defaultDate));
        System.out.println(nowDate);
        System.out.println(shipDate);
        System.out.println(dateString);
        //2018-04-04T15:00:00.000Z
    }
    //2018-04-04_17:33:22
}