package edu.olivet.harvester.utils.common;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/30/2017 12:49 PM
 */
public class RandomUtils {
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }
}
