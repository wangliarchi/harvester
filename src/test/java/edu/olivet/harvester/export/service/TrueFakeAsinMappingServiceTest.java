package edu.olivet.harvester.export.service;

import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class TrueFakeAsinMappingServiceTest extends BaseTest {
    @Test
    public void getISBN() {
        final String SPREADSHEET_ID = "1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI";
        List<Order> orders = appScript.readOrders(SPREADSHEET_ID, "11/29");
        for (Order order : orders) {
            String asin = RegexUtils.getMatched(order.getSku_address(), RegexUtils.Regex.ASIN);
            assertEquals(order.isbn, TrueFakeAsinMappingService.getISBN(order.sku, asin));
        }
    }

}