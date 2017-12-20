package edu.olivet.harvester.spreadsheet;

import com.google.inject.Inject;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Guice
public class OrderHelperTest {

    @Inject private OrderHelper orderHelper;

    @Test public void correctUSState() {
        assertEquals(orderHelper.correctUSState("Ks."), "KS");
        assertEquals(orderHelper.correctUSState("Pa."), "PA");
        assertEquals(orderHelper.correctUSState("NY."),  "NY");

        assertEquals(orderHelper.correctUSState("New York."),  "New York.");
    }

    @Test
    public void getCountryName() {
        assertEquals(orderHelper.getCountryName("CA"), "Canada");
        assertEquals(orderHelper.getCountryName("CN"), "China Mainland");
        assertEquals(orderHelper.getCountryName("HK"), "Hong Kong");
        assertEquals(orderHelper.getCountryName("CAD"), "CAD");
    }

    @Test
    public void getCountryCode() {
        assertEquals(orderHelper.getCountryCode("VI"), "VI");
        assertEquals(orderHelper.getCountryCode("vi"), "VI");
        assertEquals(orderHelper.getCountryCode("Virgin Islands, British"), "VG");
        assertEquals(orderHelper.getCountryCode("Virgin Islands, U.S."), "VI");
        assertEquals(orderHelper.getCountryCode("Ireland, Republic of"), "IE");

        assertEquals(orderHelper.getCountryCode("United States"), "US");
        assertEquals(orderHelper.getCountryCode("united states"), "US");
        assertEquals(orderHelper.getCountryCode("Afghanistan"), "AF");
        assertEquals(orderHelper.getCountryCode("Aland Islands"), "AX");
        assertEquals(orderHelper.getCountryCode("Albania"), "AL");
        assertEquals(orderHelper.getCountryCode("Algeria"), "DZ");
        assertEquals(orderHelper.getCountryCode("American Samoa"), "AS");

        assertEquals(orderHelper.getCountryCode("denmark"), "DK");
    }

}