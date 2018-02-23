package edu.olivet.harvester.utils.order;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PageUtilsTest {
    @Test
    public void getSellerUUID() {
        assertEquals(PageUtils.getSellerUUID("https://www.amazon.com/shops/A2L77EE7U53NWQ/ref=olp_merch_name_4"), "A2L77EE7U53NWQ");
        assertEquals(PageUtils.getSellerUUID("http://www.amazon.com/shops/A2L77EE7U53NWQ/ref=olp_merch_name_4"), "A2L77EE7U53NWQ");
        assertEquals(PageUtils.getSellerUUID("/gp/aag/main/ref=olp_merch_name_2?ie=UTF8&asin=159562015X&isAmazonFulfilled=1&seller=A1RS5IKH655ZAB"),
                "A1RS5IKH655ZAB");
    }

}