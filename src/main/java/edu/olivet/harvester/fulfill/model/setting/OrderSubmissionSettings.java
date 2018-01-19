package edu.olivet.harvester.fulfill.model.setting;

import com.alibaba.fastjson.JSON;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.ui.Harvester;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/15/2018 3:32 PM
 */
@Data
@Singleton
public class OrderSubmissionSettings {
    public static final String RUNTIME_SETTINGS_FILE_PATH = Directory.Customize.path() + "/order-submission-settings.json";
    public static final String TEST_RUNTIME_SETTINGS_FILE_PATH = "src/test/resources/conf/order-submission-settings.json";

    private String marketplaceName;
    private String spreadsheetId;
    private String spreadsheetName;
    private String sheetName;

    public void save() {
        File file = new File(getConfigPath());
        Tools.writeStringToFile(file, JSON.toJSONString(this, true));

        //clear cache
        instance = null;
    }

    private static OrderSubmissionSettings instance;

    public static OrderSubmissionSettings load() {
        if (instance == null) {
            File file = new File(getConfigPath());
            if (file.exists() && file.isFile()) {
                instance = JSON.parseObject(Tools.readFileToString(file), OrderSubmissionSettings.class);
            } else {
                instance = new OrderSubmissionSettings();
            }
        }

        return instance;
    }

    private static String getConfigPath() {
        if (Harvester.debugFlag) {
            return TEST_RUNTIME_SETTINGS_FILE_PATH;
        }

        return RUNTIME_SETTINGS_FILE_PATH;
    }


    public Country getCurrentCountry() {
        if (StringUtils.isBlank(marketplaceName)) {
            return null;
        }
        return Country.valueOf(marketplaceName);
    }

}
