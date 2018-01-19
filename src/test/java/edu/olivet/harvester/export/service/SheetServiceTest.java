package edu.olivet.harvester.export.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import org.apache.commons.lang3.time.DateUtils;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertEquals;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/19/17 8:45 PM
 */
public class SheetServiceTest extends BaseTest {
    @Inject private
    SheetService sheetService;

    @Inject private
    ExportOrderService exportOrderService;

    @Inject private
    Now now;

    private String spreadsheetId = "1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI";


    @Test
    public void testFillOrders() throws Exception {
        now.set(Dates.parseDate("11/30/2017"));
        Date lastExportedDate = DateUtils.addDays(new Date(), -1);

        List<Order> orders = exportOrderService.listUnexportedOrders(lastExportedDate, now.get(), Country.US);

        sheetService.fillOrders(spreadsheetId, orders);
    }

    //
    @Test
    public void testGetLastRow() throws Exception {

        assertEquals(sheetService.getLastRow(spreadsheetId,"11/29"),106);
    }
    @Test
    public void testCreateOrGetOrderSheet() throws Exception {
        now.set(Dates.parseDate("11/30/2017"));

        sheetService.createOrGetOrderSheet(spreadsheetId, now.get());
    }


}