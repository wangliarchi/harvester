package edu.olivet.harvester.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.OrderEnums;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Guice(modules = {MockDateModule.class, MockDBModule.class})
public class SettingsTest extends BaseTest {

    private Settings settings;

    @BeforeClass
    public void initialize() {
        settings = Settings.load(testConfigFilePath);
    }

    @Test
    public void testGetConfigByCountry() throws Exception {
        Settings.Configuration config = settings.getConfigByCountry(Country.US);
        //System.out.println(config);

        assertEquals(config.getCountry(), Country.US);
        assertEquals(config.getBookDataSourceUrl(), "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY");
        assertEquals(config.getStoreName(), "Mike Pro");
    }

    @Test
    public void testListAllSpreadsheets() throws Exception {
        List<String> spreadsheetIds = settings.listAllSpreadsheets();
        String[] expectedIds = {
                "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY",
                "1qxcCkAPvvBaR3KHa2MZv1V39m2E1IMytVDn1yXDaVEM",
                "1VIar2m0_78mUk3wcmfiqLWQOBB34NBsac94R4EYgcOU",
                "17k9ohj5RTCeMKKbpEbBb7azB4u3yZ3aHs1FfYTPaAMo"};
        assertEquals(spreadsheetIds, Arrays.asList(expectedIds));
    }

    @Test
    public void testgetSpreadIdByType() throws Exception {
        Settings.Configuration config = settings.getConfigByCountry(Country.US);

        assertEquals(config.getSpreadId(OrderEnums.OrderItemType.BOOK), "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY");

        assertEquals(config.getSpreadId(OrderEnums.OrderItemType.PRODUCT), "1qxcCkAPvvBaR3KHa2MZv1V39m2E1IMytVDn1yXDaVEM");
    }

    @Test
    public void testGetMWSCredential() throws Exception {
        Settings.Configuration config = settings.getConfigByCountry(Country.US);
        assertEquals(config.getValidMwsCredential(), new MarketWebServiceIdentity(
                "A3BEPQLI451F6I",
                "AKIAI73MGXM4FSWB6TIQ",
                "W5oIE4cbQ1MTG3YO5h4JU93J7FGjDX1/OLDjADBc",
                "ATVPDKIKX0DER"

        ));


    }

    @Test
    public void testConfigValidation() {
        String testConfigFilePath = basePath + "conf" + File.separator + "harvester-test-nomws.json";
        Settings.Configuration config = Settings.load(testConfigFilePath).getConfigByCountry(Country.US);

        assertTrue(config.validate().contains("MWS API credential not provided or invalid"));

    }

    @Test
    public void testConfigStoreName() {
        String testConfigFilePath = basePath + "conf" + File.separator + "harvester-test-nomws.json";
        Settings.Configuration config = Settings.load(testConfigFilePath).getConfigByCountry(Country.US);

        System.out.println(config.getStoreNameFromWeb());
    }


}