package edu.olivet.harvester.fulfill.service.addressvalidator;

import edu.olivet.harvester.fulfill.model.Address;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 1:03 PM
 */
public interface AddressValidator {
    boolean verify(Address old, Address entered);
}
