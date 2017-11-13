package edu.olivet.harvester.fulfill.model;

import com.google.common.base.Objects;
import edu.olivet.harvester.model.Order;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 8:54 AM
 */
@Data
public class Address {
    private String address1;
    private String address2 = "";
    private String city;
    private String state;
    private String zip = "";
    private String zip5 = "";
    private String zip4 = "";
    private String country;


    public void setZip(String zip) {
        this.zip = zip;
        String[] parts = StringUtils.split(zip, "-");
        if (parts.length > 1) {
            zip5 = parts[0];
            zip4 = parts[1];
        } else {
            zip5 = zip;
        }
    }

    public String getZip() {
        if (StringUtils.isNotBlank(zip4)) {
            return zip5 + "-" + zip4;
        }
        return zip5;
    }

    public static Address loadFromOrder(Order order) {
        Address address = new Address();
        address.setCountry(order.ship_country);
        address.setState(order.ship_state);
        address.setCity(order.ship_city);
        address.setAddress1(order.ship_address_1);
        address.setAddress2(order.ship_address_2);
        address.setZip(order.ship_zip);
        return address;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        Set<String> addressSet = new HashSet<>(Arrays.asList(address1, address2));
        Set<String> oAddressSet = new HashSet<>(Arrays.asList(address.getAddress1(), address.getAddress2()));
        return Objects.equal(city, address.getCity()) &&
                Objects.equal(state, address.getState()) &&
                Objects.equal(country, address.getCountry()) &&
                Objects.equal(getZip(), address.getZip()) &&
                Objects.equal(addressSet, oAddressSet);
    }

}
