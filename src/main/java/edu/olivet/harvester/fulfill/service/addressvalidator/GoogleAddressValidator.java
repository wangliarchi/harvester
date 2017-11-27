package edu.olivet.harvester.fulfill.service.addressvalidator;

import edu.olivet.harvester.fulfill.model.Address;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 9:14 AM
 */
public class GoogleAddressValidator implements AddressValidator {
    @Override
    public boolean verify(Address old, Address entered) {
        return false;
    }
    //https://maps.googleapis.com/maps/api/geocode/xml?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=YOUR_API_KEY
}
