package edu.olivet.harvester.fulfill.utils;

import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
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

    /**
     * https://www.amazon.com/gp/help/customer/display.html/ref=olp_cg_pop?ie=UTF8&nodeId=200143590&pop-up=1
     */
    public enum Condition {
        New("New", 100),

        UsedLikeNew("Used - Like New", 90),
        UsedVeryGood("Used - Very Good", 85),
        UsedGood("Used - Good", 80),
        UsedAcceptable("Used - Acceptable", 50),
        Used("Used", 80),

        CollectibleLikeNew("Collectible - Like New", 120),
        CollectibleVeryGood("Collectible - Very Good", 110),
        CollectibleGood("Collectible - Good", 100),
        CollectibleAcceptable("Collectible - Acceptable", 50),
        Collectible("Collectible", 50),

        Refurbished("Refurbished", 50),

        OpenBoxLikeNew("OpenBox - Like New", 90),
        OpenBoxGood("OpenBox - Good", 80),
        OpenBoxAcceptable("OpenBox - Acceptable", 50),
        OpenBox("OpenBox", 50);

        public static int NEW_SCORE = 100;

        public static Condition parseFromText(String conditionText) {
            String str = conditionText.replace("-", StringUtils.EMPTY).replace(StringUtils.SPACE, StringUtils.EMPTY).toLowerCase();
            if (StringUtils.startsWithIgnoreCase(str, "new")) {
                return New;
            }
            String translatedConditionText = ConditionUtils.translateCondition(str);
            for (Condition condition : Condition.values()) {
                if (condition.name().equalsIgnoreCase(translatedConditionText)) {
                    return condition;
                }
            }

            throw new BusinessException("Condition " + conditionText + " not recognized");
        }


        private String text;
        private int score;

        public int score() {
            return score;
        }

        public String text() {
            return text;
        }

        Condition(String text, int score) {
            this.text = text;
            this.score = score;
        }

        /**
         * 根据baseCondition判定是否为Used
         */
        public boolean used() {
            return score < NEW_SCORE;
        }
    }

    public static String translateCondition(String str) {
        Map<String, String> conditionI18N = Configs.load("conditions.properties", Configs.KeyCase.LowerCase);

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
    public static String getMasterCondition(String condition) {

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

    public static boolean goodToGo(Condition sellerCondition, Condition actualCondition) {
        return actualCondition.score() - sellerCondition.score() >= -10;
    }

    public static boolean goodToGo(String sellerCondition, String actualCondition) {
        return goodToGo(Condition.parseFromText(sellerCondition), Condition.parseFromText(actualCondition));
    }

    public static int getConditionLevel(String cond) {
        String str = cond.replace("-", StringUtils.EMPTY).replace(StringUtils.SPACE, StringUtils.EMPTY).toLowerCase();
        Map<String, String> conditionLevels = Configs.load(Config.ConditionLevel.fileName());
        String lvl = conditionLevels.get(str);
        if (lvl == null) {
            throw new BusinessException(UIText.message("error.condition.invalid", cond));
        }
        return IntegerUtils.parseInt(lvl, 50);
    }
}
