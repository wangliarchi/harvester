package edu.olivet.harvester.utils;

import com.google.common.collect.Lists;
import edu.olivet.foundations.utils.Configs;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/20/2018 10:28 AM
 */
public class TranslationUtils {
    private static TranslationUtils instance = null;
    private Map<String, String> translations = new HashMap<>();
    private Map<String, List<String>> translationKeys = new HashMap<>();

    public static TranslationUtils getInstance() {
        if (instance == null) {
            instance = new TranslationUtils();
        }
        return instance;
    }

    private TranslationUtils() {
        translations = Configs.load(Config.Translation.fileName());
        translations.forEach((key, enKey) -> {
            List<String> keys = translationKeys.getOrDefault(enKey.toLowerCase(), Lists.newArrayList(enKey.toLowerCase()));
            if (!keys.contains(key.toLowerCase())) {
                keys.add(key.toLowerCase());
            }
            translationKeys.put(enKey.toLowerCase(), keys);
        });
    }

    public String get(String text) {
        return translations.getOrDefault(text, text);
    }

    public String[] fromEn(String englishText) {
        List<String> keys = translationKeys.getOrDefault(englishText, new ArrayList<>());

        return keys.toArray(new String[keys.size()]);
    }
}
