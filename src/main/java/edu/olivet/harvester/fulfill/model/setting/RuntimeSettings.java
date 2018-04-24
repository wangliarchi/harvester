package edu.olivet.harvester.fulfill.model.setting;

import com.alibaba.fastjson.JSON;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.ui.Harvester;
import lombok.Data;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/7/17 1:34 PM
 */
@Data
@Singleton
public class RuntimeSettings {
    private String sid;
    private String marketplaceName;
    private String spreadsheetId;
    private String spreadsheetName;
    private String sheetName;
    private AdvancedSubmitSetting advancedSubmitSetting = new AdvancedSubmitSetting();
    private String lostLimit = "5";
    private String priceLimit = "3";
    private String eddLimit = "7";
    private String expeditedEddLimit = "3";
    private String noInvoiceText = "{No Invoice}";
    private String finderCode = "";
    private String buyerEmail;
    private String primeBuyerEmail;
    private OrderValidator.SkipValidation skipValidation = OrderValidator.SkipValidation.None;

    public static final String RUNTIME_SETTINGS_FILE_PATH = Directory.Customize.path() + "/runtime-settings.json";
    public static final String TEST_RUNTIME_SETTINGS_FILE_PATH = "src/test/resources/conf/runtime-settings.json";

    public void save() {
        File file = new File(getConfigPath());
        Tools.writeStringToFile(file, JSON.toJSONString(this, true));

        //clear cache
        instance = null;
    }

    private static RuntimeSettings instance;

    public static RuntimeSettings load() {
        if (instance == null) {
            File file = new File(getConfigPath());
            if (file.exists() && file.isFile()) {
                instance = JSON.parseObject(Tools.readFileToString(file), RuntimeSettings.class);
            } else {
                instance = new RuntimeSettings();
            }
        }

        if (instance == null) {
            instance = new RuntimeSettings();
        }
        return instance;
    }

    private static String getConfigPath() {
        if (Harvester.debugFlag) {
            return TEST_RUNTIME_SETTINGS_FILE_PATH;
        }

        return RUNTIME_SETTINGS_FILE_PATH;
    }

    public String context() {
        return this.sid + marketplaceName;
    }

    public Country getCurrentCountry() {
        return Country.valueOf(marketplaceName);
    }

    public OrderItemType getCurrentType() {
        return Strings.containsAnyIgnoreCase(spreadsheetName, "product", "prod") ? OrderItemType.PRODUCT : OrderItemType.BOOK;
    }

    public String getPrimeBuyerAccount() {
        return primeBuyerEmail;
    }

    public String getBuyerAccount() {
        return buyerEmail;
    }

    public String toString() {
        return context() + " " + spreadsheetName + " " + sheetName + " " + advancedSubmitSetting.toString();
    }
}
