package edu.olivet.harvester.fulfill.model;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Objects;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.utils.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 8:54 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String name = "";
    private String address1;
    private String address2 = "";
    private String city;
    private String state;
    private String zip = "";
    private String country = "";
    private String zip5 = "";
    private String zip4 = "";
    private String phoneNumber = "";


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
        address.setName(order.recipient_name.replaceAll("\"", "").replaceAll("&#34;", ""));
        if ("unlisted".equalsIgnoreCase(order.ship_phone_number)) {
            address.setPhoneNumber("321-123-456");
        } else {
            address.setPhoneNumber(order.ship_phone_number);
        }

        return address;
    }

    public Address copy() {
        Address address = new Address();
        address.setName(name);
        address.setAddress1(address1);
        address.setAddress2(address2);
        address.setCity(city);
        address.setState(state);
        address.setCountry(country);
        address.setZip(getZip());
        address.setPhoneNumber(phoneNumber);

        return address;
    }

    public static Address USFwdAddress() {
        return JSON.parseObject(Configs.read(Config.USForwardAddress.fileName()), Address.class);
    }

    public String getCountryCode() {
        if (StringUtils.isBlank(country)) {
            return "";
        }
        return CountryStateUtils.getInstance().getCountryCode(country);
    }

    public boolean isUSAddress() {
        return "US".equalsIgnoreCase(getCountryCode());
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


        //check recipient name first
        if (!StringUtils.equalsIgnoreCase(getRecipient(), address.getRecipient())) {
            return false;
        }

        Set<String> addressSet = addressSet();
        Set<String> oAddressSet = address.addressSet();

        boolean sameAddressLines = false;
        for (String a : addressSet) {
            if (oAddressSet.contains(a)) {
                sameAddressLines = true;
                break;
            } else {
                for (String b : oAddressSet) {
                    String com = StringUtils.getCommonPrefix(b, a);
                    String ar = a.replace(com, "");
                    String br = b.replace(com, "");

                    if (sameChars(ar, br)) {
                        sameAddressLines = true;
                        break;
                    }

                }
            }

        }

        boolean sameCountry = true;
        if (StringUtils.isNotBlank(country) && StringUtils.isNotBlank(address.getCountry())) {
            if (!getCountryCode().equalsIgnoreCase(address.getCountryCode())) {
                sameCountry = false;
            }
        }
        //todo State full and abbr
        return sameAddressLines &&
                StringUtils.equalsIgnoreCase(city, address.getCity()) &&
                StringUtils.equalsIgnoreCase(state, address.getState()) &&
                sameCountry &&
                StringUtils.equalsIgnoreCase(getZip5(), address.getZip5());
    }

    private boolean sameChars(String firstStr, String secondStr) {
        char[] first = firstStr.toCharArray();
        char[] second = secondStr.toCharArray();
        Arrays.sort(first);
        Arrays.sort(second);
        return Arrays.equals(first, second);
    }

    public String getRecipient() {

        return name.replace(RuntimeSettings.load().getNoInvoiceText(), "")
                .replaceAll(RegexUtils.Regex.PUNCTUATION.val(), "")
                .replaceAll(" ", "");
    }

    public Set<String> addressSet() {
        String a1 = cleanAddress(address1);
        String a2 = cleanAddress(address2);
        return new HashSet<>(Arrays.asList(a1 + "" + a2, a2 + "" + a1));
    }


    public String cleanAddress(String addr) {
        String a1 = addr.replaceAll(RegexUtils.Regex.PUNCTUATION.val(), StringUtils.EMPTY);
        a1 = a1.replaceAll(" ", "");
        return a1.toUpperCase();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, city, state, city, getZip(), address1, address2);
    }

    @Override
    public String toString() {
        return name + (StringUtils.isNotBlank(address1) ? ", " + address1 : "") + (StringUtils.isNotBlank(address2) ? ", " + address2 : "") + ", " + city + ", " + state + " " + getZip() + ", " + country;
    }

    public String withouName() {
        return (StringUtils.isNotBlank(address1) ? ", " + address1 : "") + (StringUtils.isNotBlank(address2) ? ", " + address2 : "") + ", " + city + ", " + state + " " + getZip() + ", " + country;
    }

}
