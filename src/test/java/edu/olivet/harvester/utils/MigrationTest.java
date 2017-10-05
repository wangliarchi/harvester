package edu.olivet.harvester.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 9/30/17 9:48 AM
 */
public class MigrationTest extends BaseTest {
    @Test
    public void testLoadFromOrderManConfigFile() throws Exception {
        String testConfigFilePath = basePath + "conf" + File.separator + "accounts.js";
        Settings settings = Migration.loadFromOrderManConfigFile(new File(testConfigFilePath));

        assertEquals(settings.getSid(), "714");
        assertEquals(settings.getConfigByCountry(Country.US).getStoreName(), "Mike Pro");

        assertEquals(settings.getConfigByCountry(Country.US).getMwsCredential().getSellerId(),"A3BEPQLI451F6I");
        assertEquals(settings.getConfigByCountry(Country.US).getMwsCredential().getAccessKey(), "AKIAI73MGXM4FSWB6TIQ");
        assertEquals(settings.getConfigByCountry(Country.US).getMwsCredential().getSecretKey(), "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");

        assertEquals(settings.getConfigByCountry(Country.US).getSeller().getEmail(),"usseller@gmail.com");
        assertEquals(settings.getConfigByCountry(Country.US).getSeller().getPassword(),"BarrP78Fly");

        assertEquals(settings.getConfigByCountry(Country.US).getBuyer().getEmail(),"buyerus@gmail.com");
        assertEquals(settings.getConfigByCountry(Country.US).getBuyer().getPassword(),"buyeruspw");

        assertEquals(settings.getConfigByCountry(Country.US).getProdBuyer(), null);


        assertEquals(settings.getConfigByCountry(Country.US).getPrimeBuyer().getEmail(),"pbuyerus@gmail.com");
        assertEquals(settings.getConfigByCountry(Country.US).getPrimeBuyer().getPassword(),"pbuyeruspw");

        assertEquals(settings.getConfigByCountry(Country.US).getBookDataSourceUrl(), "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY");
        assertEquals(settings.getConfigByCountry(Country.US).getProductDataSourceUrl(), "");


        assertEquals(settings.getConfigByCountry(Country.MX).getBuyer(),null);
        assertEquals(settings.getConfigByCountry(Country.MX).getProdBuyer().getEmail(),"mxprobuyer@gmail.com");



        assertEquals(settings.getConfigByCountry(Country.CA).getMwsCredential().getSellerId(),"CAA3BEPQLI451F6I");
        assertEquals(settings.getConfigByCountry(Country.CA).getMwsCredential().getAccessKey(), "CAAKIAI73MGXM4FSWB6TIQ");
        assertEquals(settings.getConfigByCountry(Country.CA).getMwsCredential().getSecretKey(), "CAW5oIE4cbQ1MTG3YO5h4JU93J7FGjDX1/OLDjADBc");


        assertEquals(settings.getConfigByCountry(Country.MX).getMwsCredential().getSellerId(),"CAA3BEPQLI451F6I");
        assertEquals(settings.getConfigByCountry(Country.MX).getMwsCredential().getAccessKey(), "CAAKIAI73MGXM4FSWB6TIQ");
        assertEquals(settings.getConfigByCountry(Country.MX).getMwsCredential().getSecretKey(), "CAW5oIE4cbQ1MTG3YO5h4JU93J7FGjDX1/OLDjADBc");



    }

}