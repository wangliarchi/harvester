package edu.olivet.harvester.fulfill;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.RepeatModule;
import edu.olivet.foundations.utils.DateModule;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums.OrderColor;
import edu.olivet.harvester.model.Spreadsheet;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

@Guice(modules = {RepeatModule.class, DateModule.class})
public class AppsScriptTest {
    @Inject private AppsScript appsScript;
    private static final String SAMPLE_SPREAD_ID = "1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8";

    @Test
    public void getSpreadsheet() {
        Spreadsheet sp = appsScript.getSpreadsheet(SAMPLE_SPREAD_ID);
        assertEquals(sp.getTitle(), "Harvester Prototype Sheet 2017");
        assertTrue(sp.getSheetNames().containsAll(Arrays.asList("Daily Cost", "confirm", "Individual Orders", "09/21", "09/14")));
    }

    @Test
    public void getSpreadId() {
        assertEquals(appsScript.getSpreadId("https://docs.google.com/spreadsheets/d/1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8/edit#gid=1792115927"),
            "1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8");
        assertEquals(appsScript.getSpreadId("https://drive.google.com/open?id=1v8wpwfuc1jYF-ERqhDMC-YATUM8MxD-L8ftKQEl_BIM"),
            "1v8wpwfuc1jYF-ERqhDMC-YATUM8MxD-L8ftKQEl_BIM");
    }

    @Test
    public void markColor() {
        assertTrue(appsScript.markColor(SAMPLE_SPREAD_ID, "09/20", 4, OrderColor.Finished));
        assertFalse(appsScript.readOrders(SAMPLE_SPREAD_ID, "09/20").get(1).colorIsGray());

        assertTrue(appsScript.markColor(SAMPLE_SPREAD_ID, "09/20", 4, OrderColor.Gray));
        assertTrue(appsScript.readOrders(SAMPLE_SPREAD_ID, "09/20").get(1).colorIsGray());

        assertTrue(appsScript.markColor(SAMPLE_SPREAD_ID, "09/14", 3, OrderColor.HighPriority));
    }

    @Test
    public void readOrders() {
        List<Order> orders = appsScript.readOrders(SAMPLE_SPREAD_ID, "09/21");
        assertEquals(orders.size(), 5);
        assertTrue(orders.get(3).colorIsGray() && orders.get(4).colorIsGray());
        assertEquals(orders.get(1).status, "fi09月/13日已移表");
    }

}