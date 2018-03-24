package edu.olivet.harvester.fulfill.service.addressvalidator;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.Address;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OrderManAddressValidatorTest  extends BaseTest {
    @Inject private
    OrderManAddressValidator addressValidator;

    @Test
    public void testGetCorrectedAddress() throws Exception {
    }

    @Test
    public void testVerify() throws Exception {

        Address oldAddress = new Address();
        Address newAddress = new Address();

        oldAddress.setName("MaryWKennedy");
        oldAddress.setCountry("United States");
        newAddress.setName("MaryWKennedy");
        newAddress.setCountry("United States");

        oldAddress.setAddress1("613 Rolling Hills Cirle");
        oldAddress.setAddress2("Box 190");
        oldAddress.setCity("Honey Brook");
        oldAddress.setState("PA");
        oldAddress.setZip("19344");

        newAddress.setAddress1("PO BOX 190");
        newAddress.setAddress2("613 ROLLING HILLS CIRLE");
        newAddress.setCity("HONEY BROOK");
        newAddress.setState("PA");
        newAddress.setZip("19344-0190");

        Assert.assertEquals(addressValidator.verify(oldAddress, newAddress), true);
    }

}