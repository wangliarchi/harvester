package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.service.addressvalidator.AddressValidator;
import edu.olivet.harvester.fulfill.service.addressvalidator.GoogleAddressValidator;
import edu.olivet.harvester.fulfill.service.addressvalidator.OrderManAddressValidator;
import edu.olivet.harvester.fulfill.service.addressvalidator.USPSAddressValidator;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/25/2017 8:54 AM
 */
public class AddressValidatorService implements AddressValidator {
    @Inject GoogleAddressValidator googleAddressValidator;
    @Inject USPSAddressValidator uspsAddressValidator;
    @Inject OrderManAddressValidator orderManAddressValidator;

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

}
