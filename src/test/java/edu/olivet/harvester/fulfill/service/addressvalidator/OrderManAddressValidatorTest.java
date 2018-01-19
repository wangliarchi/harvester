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

        oldAddress.setName("test");
        oldAddress.setCountry("United States");
        newAddress.setName("test{No Invoice}");
        newAddress.setCountry("United States");

        oldAddress.setAddress1("4350 N. 84 Place");
        oldAddress.setAddress2("Apt 3c");
        oldAddress.setCity("Scottsdale");
        oldAddress.setState("AZ");
        oldAddress.setZip("85251");

        newAddress.setAddress1("4350 N 84TH PL");
        newAddress.setAddress2("Apt c3");
        newAddress.setCity(oldAddress.getCity());
        newAddress.setState(oldAddress.getState());
        newAddress.setZip(oldAddress.getZip());

        Assert.assertEquals(addressValidator.verify(oldAddress, newAddress), true);
    }

}