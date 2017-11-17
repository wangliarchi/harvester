package edu.olivet.harvester.fulfill.utils;

import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 10:40 AM
 */
public class ConditionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionUtils.class);

    private static Map<String, String> conditionI18N;


    public enum Condition {
        New("New"),
        Collectible("Collectible"),
        OpenBox("OpenBox"),
        Refurbished("Refurbished"),
        Used("Used"),
        UsedLikeNew("Used - Like New"),
        UsedVeryGood("Used - Very Good"),
        UsedGood("Used - Good"),
        UsedAcceptable("Used - Acceptable");

        private String text;

        public String text() {
            return text;
        }

        Condition(String text) {
            this.text = text;
        }

        /**
         * 根据baseCondition判定是否为Used
         *
         * @param baseCondition
         */
        public static boolean used(String baseCondition) {
            return Condition.Used.name().equalsIgnoreCase(baseCondition) ||
                    Condition.OpenBox.name().equalsIgnoreCase(baseCondition) ||
                    Condition.Refurbished.name().equalsIgnoreCase(baseCondition);
        }
    }

    public static String translateCondition(String str) {
        conditionI18N = Configs.load("conditions.properties", Configs.KeyCase.LowerCase);

        String translated = conditionI18N.get(str.toLowerCase());
        if (translated == null) {
            translated = conditionI18N.get(str.toLowerCase().replace(StringUtils.SPACE, StringUtils.EMPTY));
            if (translated == null) {
                LOGGER.info("No translation info found for {}", str);
                return str;
                //throw new IllegalArgumentException(UIText.message("error.condition.invalid", str));
            }
        }
        return translated;
    }

    private static Map<String, String> baseConditionCache = new HashMap<>();

    /**
     * 获取Condition的基础Condition，基础condition就是 aaa-bbb横线前面那部分
     * 基础condition只有两种，new和used
     * 比如Used - Very Good的基础Condition为Used
     * new -> new
     * Used - Good -> used
     *
     * @param condition 订单产品的condition字符串
     */
    public static String getMasterCondtion(String condition) {

        if (StringUtils.isBlank(condition)) {
            throw new IllegalArgumentException(UIText.message("error.condition.missing"));
        }

        String result = baseConditionCache.get(condition);
        if (StringUtils.isNotBlank(result)) {
            return result;
        }

        String[] arr = StringUtils.split(condition, Constants.HYPHEN);
        result = translateCondition(arr[0].toLowerCase().trim());
        baseConditionCache.put(condition, result);
        return result;
    }

    public static int getConditionLevel(String cond) {
        String str = cond.replace("-", StringUtils.EMPTY).replace(StringUtils.SPACE, StringUtils.EMPTY).toLowerCase();
        Map<String, String> conditionLvls = Configs.load(Config.ConditionLevel.fileName());
        String lvl = conditionLvls.get(str);
        if (lvl == null) {
            throw new IllegalArgumentException(UIText.message("error.condition.invalid", cond));
        }
        return IntegerUtils.parseInt(lvl, 50);
    }
}
