package edu.olivet.harvester.service;

import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 12:03 PM
 */
public class OrderServiceTest extends BaseTest {

    @Inject OrderService orderService;
    @Inject
    SheetAPI sheetAPI;
    @Test
    public void testFindDuplicates() throws Exception {
        String spreadsheetId = "1J6CqNKoSfw3ERNWTLXVYh3R37a11kB4nBEzySIypV68";
        Spreadsheet spreadsheet = sheetAPI.getSpreadsheet(spreadsheetId);
        List<Order> orders = orderService.getOrders(spreadsheet, Dates.parseDate("10/20/2017"));
        List<Order> dOrders = orderService.findDuplicates(orders);
        System.out.println(dOrders);
    }

}