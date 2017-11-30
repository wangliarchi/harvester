package edu.olivet.harvester.fulfill.service.addressvalidator;

import edu.olivet.harvester.fulfill.model.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 9:14 AM
 */
public class GoogleAddressValidator implements AddressValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAddressValidator.class);
    private static final String ENDPOINT = "https://maps.googleapis.com/maps/api/geocode/xml?address=%s&key=AIzaSyAF-QwGpe8EZUoZVNVl8nmNFnSJW-S8AoY";

    @Override
    public boolean verify(Address old, Address entered) {
        String url = String.format(ENDPOINT, entered.withouName());
        LOGGER.debug("{}", url);
        return false;
    }

}
