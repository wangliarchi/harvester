package edu.olivet.harvester.export.service;

import com.amazonservices.mws.orders._2013_09_01.model.Order;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import org.apache.commons.lang3.time.DateUtils;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 12:03 PM
 */
public class ExportOrderServiceTest extends BaseTest {
    @Inject
    ExportOrderService exportOrderService;
    @Inject
    Now now;

    @Test
    public void testRemoveExportedOrders() {
        now.set(Dates.parseDate("11/30/2017"));
        Date lastExportedDate = DateUtils.addDays(new Date(), -1);

        List<Order> orders = exportOrderService.listOrdersFromAmazon(lastExportedDate, now.get(), Country.US);
        assertEquals(orders.size(), 6);


    }

    @Test
    public void testSaveAmazonOrders() {
        now.set(Dates.parseDate("11/30/2017"));
        Date lastExportedDate = DateUtils.addDays(new Date(), -1);
        List<Order> orders = exportOrderService.listOrdersFromAmazon(lastExportedDate,now.get(), Country.US);
        orders = exportOrderService.removeExportedOrders(orders,now.get(), Country.US);
        exportOrderService.convertToAmazonOrders(orders, Country.US);
    }

}