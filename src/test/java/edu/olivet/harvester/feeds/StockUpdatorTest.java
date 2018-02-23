package edu.olivet.harvester.feeds;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StockUpdatorTest extends BaseTest {
    @Inject StockUpdator stockUpdator;

    @Test
    public void getSyncSkus() {
        System.out.println(stockUpdator.getSyncSkus(Country.UK));
    }

    @Test
    public void getUpdateRecords() {
    }

}