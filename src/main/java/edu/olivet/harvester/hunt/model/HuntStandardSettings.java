package edu.olivet.harvester.hunt.model;

import com.alibaba.fastjson.JSON;
import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.ui.Harvester;
import lombok.Data;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 11:32 AM
 */
@Data
@Singleton
public class HuntStandardSettings {

    public static final String SETTINGS_FILE_PATH = Directory.Customize.path() + "/hunt-standard-settings.json";
    public static final String TEST_SETTINGS_FILE_PATH = "src/test/resources/conf/hunt-standard-settings.json";

    public enum HuntingMode {
        Strict, Loose
    }

    private HuntingMode huntingMode = HuntingMode.Strict;
    private HuntStandard newBookStandard = HuntStandard.newBookDefault();
    private HuntStandard usedBookStandard = HuntStandard.usedBookDefault();
    private HuntStandard newProductStandard = HuntStandard.newProductDefault();
    private float maxProfitLoss = -5;


    public void save() {
        File file = new File(getConfigPath());
        Tools.writeStringToFile(file, JSON.toJSONString(this, true));

        //clear cache
        instance = null;
    }

    public HuntStandard getHuntStandard(Order order) {
        if (OrderItemTypeHelper.getItemTypeBySku(order) == OrderItemType.PRODUCT) {
            return newProductStandard;
        }

        if (order.originalCondition().used()) {
            return usedBookStandard;
        }

        return newBookStandard;
    }

    private static HuntStandardSettings instance;

    public static HuntStandardSettings load() {
        if (instance == null) {
            File file = new File(getConfigPath());
            if (file.exists() && file.isFile()) {
                instance = JSON.parseObject(Tools.readFileToString(file), HuntStandardSettings.class);
            } else {
                instance = new HuntStandardSettings();
            }
        }

        return instance;
    }

    private static String getConfigPath() {
        if (Harvester.debugFlag) {
            return TEST_SETTINGS_FILE_PATH;
        }

        return SETTINGS_FILE_PATH;
    }

}
