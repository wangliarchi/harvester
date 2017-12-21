package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.addressvalidator.AddressValidator;
import edu.olivet.harvester.fulfill.service.addressvalidator.GoogleAddressValidator;
import edu.olivet.harvester.fulfill.service.addressvalidator.OrderManAddressValidator;
import edu.olivet.harvester.fulfill.service.addressvalidator.USPSAddressValidator;
import org.apache.commons.lang3.SystemUtils;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/25/2017 8:54 AM
 */
public class AddressValidatorService implements AddressValidator {
    @Inject GoogleAddressValidator googleAddressValidator;
    @Inject USPSAddressValidator uspsAddressValidator;
    @Inject OrderManAddressValidator orderManAddressValidator;

    private static final String FAILED_LOG_APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbytuV7WRhq5aWNf0vQ4PIf1iCE4rkdgchlC23GvojlDxJVLZNk/exec";

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean verify(Address old, Address entered) {
        if (old.isUSAddress()) {
            if (uspsAddressValidator.verify(old, entered)) {
                return true;
            }
        }

        if (googleAddressValidator.verify(old, entered)) {
            return true;
        }

        return orderManAddressValidator.verify(old, entered);
    }

    @Repeat
    public static void logFailed(String original, String entered, String uspsReturned) {
        String sid = RuntimeSettings.load().getSid() + RuntimeSettings.load().getMarketplaceName();
        String url = FAILED_LOG_APP_SCRIPT_URL + "?s=" + Strings.encode(sid) + "&o=" + Strings.encode(original) + "&e=" + entered + "&u=" + Strings.encode(uspsReturned);
        try {
            Jsoup.connect(url).ignoreContentType(true).timeout(12000).execute().body().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        logFailed("a", "b", "c");
    }

}
