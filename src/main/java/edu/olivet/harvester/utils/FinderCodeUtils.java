package edu.olivet.harvester.utils;

import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Random;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/4/17 9:00 PM
 */
public class FinderCodeUtils {
    private static final List<String> names = Configs.readLines(Config.Names.fileName());

    public static String generate() {
        Random rand = new Random();
        int randomIndex = rand.nextInt(names.size());
        return names.get(randomIndex);
    }

    public static boolean validate(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }

        if (Character.isDigit(code.charAt(0))) {
            return false;
        }

        String escaped = code.replaceAll(RegexUtils.Regex.NON_ALPHA_LETTERS.val(), "");
        return StringUtils.length(escaped) >= 3;
    }

    public static void main(String[] args) {
        System.out.println(FinderCodeUtils.generate());
    }

}
