package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class HuntServiceTest extends BaseTest {

    @Inject HuntService huntService;
    @Inject Now now;

    @Test
    public void huntForOrder() {
        now.set(Dates.parseDate("01/24/2018"));

        String spreadsheetId = "1eSECnF7F6hCybrUJWBl9EUzZrw6NnrAWe7h15OyYrjo";
        String sheetName = "01/24";
        List<Order> orders = appScript.readOrders(spreadsheetId, sheetName);
        for (Order order : orders) {
            if (!OrderCountryUtils.getShipToCountry(order).equalsIgnoreCase("US")) {
                continue;
            }
            Seller seller = huntService.huntForOrder(order);
            assertEquals(seller.getName(), order.seller);
            assertEquals(seller.getType().abbrev(), order.character);
        }

    }

}