package edu.olivet.harvester.feeds;

import com.amazonservices.mws.orders._2013_09_01.model.ListOrdersResult;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.*;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.feeds.helper.ConfirmShipmentEmailSender;
import edu.olivet.harvester.feeds.helper.FeedGenerator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.mws.OrderClient;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Guice(modules = {MockDateModule.class, MockDBModule.class})
public class ConfirmShipmentsTest extends BaseTest {

    @Inject
    private ConfirmShipments confirmShipments;

    private Spreadsheet spreadsheet;

    @BeforeClass
    public void init() {
        confirmShipments.setAppScript(appScript);
        String spreadsheetId = "1qxcCkAPvvBaR3KHa2MZv1V39m2E1IMytVDn1yXDaVEM";
        spreadsheet = appScript.getSpreadsheet(spreadsheetId);

    }


    @Test
    public void testGetOrdersFromWorksheet() throws Exception {


        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);

        assertEquals(orders.size(), 8);

        assertEquals(orders.get(0).sku, "JiuXMZLCustextbkAug22-2017-C726348");
        assertEquals(orders.get(7).status, "n");


    }


    @Test
    public void testGetOrdersFromWorksheetIT() throws Exception {


        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");

        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);


        assertEquals(orders.get(0).sku, "JiuXMZLCustextbkAug22-2017-C726348");
        assertEquals(orders.get(7).status, "n");


    }

    @Test
    public void testGetLastOrderRow() {
        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");


        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);


        assertEquals(confirmShipments.getLastOrderRow(orders), 10);

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
        confirmShipments.writeLogToWorksheet(worksheet, submissionResult,"Total 7; ");
    }

    @Test
    public void testGetSheetNameByDate() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        long date;

        date = formatter.parse("09/28/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date), "09/28");

        date = formatter.parse("09/08/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date), "09/08");

        date = formatter.parse("10/28/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date), "10/28");

        date = formatter.parse("10/08/2017").getTime();
        assertEquals(confirmShipments.getSheetNameByDate(date), "10/08");
    }

    @Inject
    private FeedUploader feedUploader;

    @Test(expectedExceptions = BusinessException.class)
    public void testSubmitFeed() {

        String configFilePath = basePath + File.separator + "conf/harvester-test-invalidmws.json";
        Settings.Configuration config = Settings.load(configFilePath).getConfigByCountry(Country.US);

        MarketWebServiceIdentity credential = config.getMwsCredential();

        File feedFile = new File(TEST_DATA_ROOT + File.separator + "feed-US_BOO_confirm_shipment-invalid.txt");
        feedUploader.execute(feedFile, FeedGenerator.BatchFileType.ShippingConfirmation.feedType(), credential, 1);

    }

    @Test
    public void testInsertToLocalDbLog() {

        String submissionResult = "Feed Processing Summary:\n" +
                "\tNumber of records processed\t\t7\n" +
                "\tNumber of records successful\t\t7";

        File feedFile = new File(TEST_DATA_ROOT + File.separator + "feed-US_BOOK_confirm_shipment_2017-9-28_120813.txt");

        confirmShipments.insertToLocalDbLog(feedFile, Country.US, submissionResult);
    }


    @Test
    public void testNotConfirmedOrderNotification() throws ParseException {

        OrderClient orderClient = new OrderClient() {
            @Override
            public List<com.amazonservices.mws.orders._2013_09_01.model.Order> listOrders(Country country, @Nullable Map<OrderFetcher.DateRangeType, Date> dateMap, String... statuses) {

                try {
                    File localOrderXMLFile = new File(TEST_DATA_ROOT + File.separator + "list-orders-" + country.name() + ".xml");
                    String xmlFragment = Tools.readFileToString(localOrderXMLFile);
                    ListOrdersResult listOrdersResult = MWSUtils.buildMwsObject(xmlFragment, ListOrdersResult.class);

                    return listOrdersResult.getOrders();
                } catch (Exception e) {
                    return new ArrayList<>();
                }

            }

        };
        ConfirmShipmentEmailSender confirmShipmentEmailSender = confirmShipments.getConfirmShipmentEmailSender();
        confirmShipmentEmailSender.setTestMode(true);
        confirmShipments.setConfirmShipmentEmailSender(confirmShipmentEmailSender);
        confirmShipments.setMwsOrderClient(orderClient);
        confirmShipments.notConfirmedOrderNotification();
    }

    @Test
    public void testGetOrderFinderEmail() {
        Worksheet worksheet = new Worksheet(spreadsheet, "09/22");
        List<Order> orders = confirmShipments.getOrdersFromWorksheet(worksheet);
        assertEquals(confirmShipments.getOrderFinderEmail(orders),"johnnyxiang2017@gmail.com");
    }

}