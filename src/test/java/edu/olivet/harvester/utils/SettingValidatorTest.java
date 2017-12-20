package edu.olivet.harvester.utils;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/6/17 10:14 PM
 */

@Guice(modules = {MockDateModule.class, MockDBModule.class})
public class SettingValidatorTest extends BaseTest {

    @Inject
    private SettingValidator settingValidator;


    @Test
    public void testValidate() {
        Settings settings = Settings.load(testConfigFilePath);
        List<String> errors = settingValidator.validate(settings.getSid(),settings.getConfigs());

        System.out.println(errors);
    }

    @Test
    public void testSpreadsheetCountryAndTitleShouldMatch() throws Exception {
        Settings.Configuration config = Settings.load(testConfigFilePath).getConfigByCountry(Country.MX);
        List<String> errors = settingValidator.spreadsheetAccountCountryAndTitleShouldMatch("714",config);

        assertEquals(errors.size(), 1);
    }


    @Test
    public void testSpreadsheetTypeAndTitleShouldMatch() throws Exception {
        Settings.Configuration config = Settings.load(testConfigFilePath).getConfigByCountry(Country.MX);
        List<String> errors = settingValidator.spreadsheetTypeAndTitleShouldMatch(config);

        System.out.println(errors);
        assertEquals(errors.size(), 1);
    }


    @Test
    public void testValidateSpreadsheetIds() throws Exception {
        Settings.Configuration config = Settings.load(testConfigFilePath).getConfigByCountry(Country.CA);
        List<String> errors = settingValidator.validateSpreadsheetIds(config);

        System.out.println(errors);
        assertEquals(errors.size(), 1);
    }

    @Test
    public void testSpreadsheetIdNeedToBeUnique() {
        Settings settings = Settings.load(testConfigFilePath);

        List<String> errors = settingValidator.spreadsheetIdNeedToBeUnique(settings.getConfigs());

        System.out.println(errors);

    }
}