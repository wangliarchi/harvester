package edu.olivet.harvester.feeds;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.FeedUploader;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.feeds.helper.FeedGenerator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.Assert.*;

@Guice
public class ConfirmShipmentsTest extends  BaseTest {

    @Inject private ConfirmShipments confirmShipments;

    String spreadsheetId = "1qxcCkAPvvBaR3KHa2MZv1V39m2E1IMytVDn1yXDaVEM";

    Spreadsheet spreadsheet;

    @BeforeClass
    public  void  init(){
        confirmShipments.setAppScript(appScript);
        spreadsheet = appScript.getSpreadsheet(spreadsheetId);
    }


    @Test
    public void testGetOrdersFromWorksheet() throws Exception {


        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);

        assertEquals(orders.size(),8);

        assertEquals(orders.get(0).sku,"JiuXMZLCustextbkAug22-2017-C726348");
        assertEquals(orders.get(7).status,"n");


    }

    @Test(expectedExceptions = NoWorksheetFoundException.class)
    public void testGetOrdersFromWorksheetInValidSheetName() throws Exception {

        Worksheet worksheet = new Worksheet(spreadsheet, "09/222");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);
    }

    @Test(expectedExceptions = NoOrdersFoundInWorksheetException.class)
    public void testGetOrdersFromWorksheetNoData() throws Exception {
        String spreadsheetId = "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY";
        Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);


        Worksheet worksheet = new Worksheet(spreadsheet, "template");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);
    }


    @Test
    public  void testGetLastOrderRow() {
        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");


        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);


        assertEquals(confirmShipments.getLastOrderRow(orders),10);

    }

    @Test
    public void testWriteLogToWorksheet() {
        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");
        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);

        String submissionResult = "Feed Processing Summary:\n" +
                "\tNumber of records processed\t\t7\n" +
                "\tNumber of records successful\t\t7";

        //write to test googlesheet
        confirmShipments.setLastOrderRowNo(confirmShipments.getLastOrderRow(orders));
        confirmShipments.writeLogToWorksheet(worksheet,submissionResult);
    }

    @Test
    public void testGetSheetNameByDate() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        long date;

        date = formatter.parse("09/28/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date),"09/28");

        date = formatter.parse("09/08/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date),"09/08");

        date = formatter.parse("10/28/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date),"10/28");

        date = formatter.parse("10/08/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date),"10/08");
    }

    @Inject private FeedUploader feedUploader;

    @Test
    public void testSubmitFeed() {

        Settings.Configuration config = Settings.load(basePath+File.separator+"conf/harvester-test-invalidmws.json").getConfigByCountry(Country.US);

        MarketWebServiceIdentity credential = config.getMwsCredential();

        File feedFile = new File(TEST_DATA_ROOT + File.separator + "feed-US_BOO_confirm_shipment-invalid.txt");
        String result = feedUploader.execute(feedFile, FeedGenerator.BatchFileType.ShippingConfirmation.feedType(), credential, 1);

    }

    @Test
    public  void  testInsertToLocalDbLog() {

        String submissionResult = "Feed Processing Summary:\n" +
                "\tNumber of records processed\t\t7\n" +
                "\tNumber of records successful\t\t7";

        File feedFile = new File(TEST_DATA_ROOT+File.separator+"feed-US_BOOK_confirm_shipment_2017-9-28_120813.txt");

        confirmShipments.insertToLocalDbLog(feedFile, Country.US, submissionResult);
    }
    @Test
    public void testGenerateFeedFile() throws Exception {
    }


}