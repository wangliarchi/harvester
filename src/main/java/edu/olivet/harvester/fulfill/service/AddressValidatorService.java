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
import edu.olivet.harvester.utils.Settings;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/25/2017 8:54 AM
 */
public class AddressValidatorService implements AddressValidator {
    @Inject private GoogleAddressValidator googleAddressValidator;
    @Inject private USPSAddressValidator uspsAddressValidator;
    @Inject private OrderManAddressValidator orderManAddressValidator;
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressValidatorService.class);
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
        //todo remove runtime settings
        String sid = Settings.load().getSid() + RuntimeSettings.load().getMarketplaceName();
        String url = FAILED_LOG_APP_SCRIPT_URL + "?s=" + Strings.encode(sid) + "&o=" + Strings.encode(original) +
                "&e=" + Strings.encode(entered) + "&u=" + Strings.encode(uspsReturned);
        try {
            Jsoup.connect(url).ignoreContentType(true).timeout(12000).execute();
        } catch (IOException e) {
            LOGGER.error("{} - ", url, e);
            //throw new BusinessException(e);
            //e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        logFailed("a", "b", "c");
    }

}
