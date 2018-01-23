package edu.olivet.harvester.utils;

import edu.olivet.foundations.amazon.Country;
import org.apache.logging.log4j.util.Strings;

import java.util.Locale;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/20/2018 5:07 PM
 */
public class I18N extends edu.olivet.foundations.utils.I18N {
    public I18N(String... fileBaseNames) {
        super(fileBaseNames);
    }

    public String getText(String key, Country country) {
        return getText(key, country.locale());
    }

    public String getText(String key, Locale locale) {
        this.setLocale(locale);
        key = key.replaceAll(" ", "").toLowerCase();
        return this.getText(key);
    }
}
