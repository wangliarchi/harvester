package edu.olivet.harvester.fulfill.model;

import com.alibaba.fastjson.JSON;
import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Tools;
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
    private String noInvoiceText = "{No Invoice}";
    private String finderCode = "";

    public void save() {
        File file = new File(Harvester.RUNTIME_SETTINGS_FILE_PATH);
        Tools.writeStringToFile(file, JSON.toJSONString(this, true));
    }

    public static RuntimeSettings load() {
        File file = new File(Harvester.RUNTIME_SETTINGS_FILE_PATH);
        RuntimeSettings settings;
        if (file.exists() && file.isFile()) {
            settings = JSON.parseObject(Tools.readFileToString(file), RuntimeSettings.class);
        } else {
            settings = new RuntimeSettings();
        }

        return settings;
    }

    public String context() {
        return this.sid + marketplaceName;
    }

    public String toString() {
        return context() + " " + spreadsheetName + " " + sheetName + " " + advancedSubmitSetting.toString();
    }
}
