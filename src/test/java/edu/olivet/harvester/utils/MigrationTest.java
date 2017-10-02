package edu.olivet.harvester.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

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
        assertEquals(settings.getConfigByCountry(Country.US).getMwsCredential().getAccessKey(), "AKIAI73MGXM4FSWB6TIQ");

        assertEquals(settings.getConfigByCountry(Country.US).getBookDataSourceUrl(), "1IMbmaLUjqvZ7w8OdPd59fpTuad8U__5PAyKg3yR0DjY");
    }

}