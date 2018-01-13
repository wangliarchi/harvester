package edu.olivet.harvester.utils.common;

import edu.olivet.harvester.utils.JXBrowserHelper;

import java.util.regex.Pattern;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/10/18 1:52 PM
 */
public class Strings extends edu.olivet.foundations.utils.Strings {
    public static String parseErrorMsg(String fullMsg) {
        if (edu.olivet.foundations.utils.Strings.containsAnyIgnoreCase(fullMsg, JXBrowserHelper.CHANNEL_CLOSED_MESSAGE)) {
            return "JXBrowser Crashed";
        }

        Pattern pattern = Pattern.compile(Pattern.quote("xception:"));
        String[] parts = pattern.split(fullMsg);
        return parts[parts.length - 1].trim();
    }
}
