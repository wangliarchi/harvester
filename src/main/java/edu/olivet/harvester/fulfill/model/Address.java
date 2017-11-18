package edu.olivet.harvester.fulfill.model;

import com.google.common.base.Objects;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.model.Order;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
        Set<String> addressSet = addressSet();
        Set<String> oAddressSet = address.addressSet();

        boolean sameAddressLines = false;
        for (String a : addressSet) {
            if (oAddressSet.contains(a)) {
                sameAddressLines = true;
                break;
            }
        }

        //todo State full and abbr
        return sameAddressLines &&
                //StringUtils.equalsIgnoreCase(city, address.getCity()) &&
                StringUtils.equalsIgnoreCase(state, address.getState()) &&
                StringUtils.equalsIgnoreCase(country, address.getCountry()) &&
                StringUtils.equalsIgnoreCase(getZip5(), address.getZip5());
    }

    public Set<String> addressSet() {
        String a1 = cleanAddress(address1);
        String a2 = cleanAddress(address2);
        return new HashSet<>(Arrays.asList(a1 + " " + a2, a2 + " " + a1));
    }


    public String cleanAddress(String addr) {
        String a1 = addr.replaceAll(RegexUtils.Regex.PUNCTUATION.val(), StringUtils.EMPTY);
        a1 = a1.replaceAll(" ","");
        return a1.toUpperCase();
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(city, state, city, getZip(), address1, address2);
    }

}
