package edu.olivet.harvester.spreadsheet;

import edu.olivet.foundations.aop.RepeatModule;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.utils.DateModule;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderColor;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

@Guice(modules = {RepeatModule.class, DateModule.class, MockDBModule.class})
public class AppScriptTest extends BaseTest {

    private static final String SAMPLE_SPREAD_ID = "1qxcCkAPvvBaR3KHa2MZv1V39m2E1IMytVDn1yXDaVEM";

    @Test
    public void getSpreadsheet() {
        Spreadsheet spreadsheet = appScript.getSpreadsheet(SAMPLE_SPREAD_ID);
        assertEquals(spreadsheet.getTitle(), "!!!TEST ONLY - Copy of ACC 714 US Book Order Update - TEST ONLY!!!!");
        assertTrue(spreadsheet.getSheetNames().containsAll(Arrays.asList("Daily Cost", "confirm", "Individual Orders", "09/21", "09/14")));
    }

    @Test
    public void getSpreadId() {
        assertEquals(AppScript.getSpreadId("https://docs.google.com/spreadsheets/d/1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8/edit#gid=1792115927"),
            "1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8");
        assertEquals(AppScript.getSpreadId("https://drive.google.com/open?id=1v8wpwfuc1jYF-ERqhDMC-YATUM8MxD-L8ftKQEl_BIM"),
            "1v8wpwfuc1jYF-ERqhDMC-YATUM8MxD-L8ftKQEl_BIM");
    }

    @Test
    public void markColor() {
        assertTrue(appScript.markColor(SAMPLE_SPREAD_ID, "09/20", 4, OrderColor.Finished));
        assertFalse(appScript.readOrders(SAMPLE_SPREAD_ID, "09/20").get(1).colorIsGray());

        assertTrue(appScript.markColor(SAMPLE_SPREAD_ID, "09/20", 4, OrderColor.Gray));
        assertTrue(appScript.readOrders(SAMPLE_SPREAD_ID, "09/20").get(1).colorIsGray());

        assertTrue(appScript.markColor(SAMPLE_SPREAD_ID, "09/14", 3, OrderColor.HighPriority));
    }

    @Test
    public void readOrders() {
        List<Order> orders = appScript.readOrders(SAMPLE_SPREAD_ID, "09/21");
        assertEquals(orders.size(), 11);
        assertTrue(orders.get(10).colorIsGray());
        assertEquals(orders.get(1).status, "fi09月/13日已移表");
    }




}