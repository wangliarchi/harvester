package edu.olivet.harvester.logger;

import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.service.SellerHuntingLogger;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SellerHuntingLoggerTest {
    @Test
    public void getFilePath() {
        SellerHuntingLogger logger = SellerHuntingLogger.getInstance();

        Order order = new Order();
        order.sheetName = "01/25";
        order.order_id = "114-7000378-4180241";
        order.isbn = "B00K1KZVJY";
        order.original_condition = "New";
        assertEquals(logger.getFilePath(order),"app-data/logs/hunt/01-25/114-7000378-4180241-B00K1KZVJY-New.log");
    }

}