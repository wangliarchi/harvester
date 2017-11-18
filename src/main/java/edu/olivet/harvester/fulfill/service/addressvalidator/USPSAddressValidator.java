package edu.olivet.harvester.fulfill.service.addressvalidator;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 8:45 AM
 */
public class USPSAddressValidator implements AddressValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(USPSAddressValidator.class);
    private static final String USPS_ADDRESS_VRF_ENDPOINT = "http://production.shippingapis.com/ShippingAPI.dll?API=Verify&XML=";
    private static final String USERNAME = "566OLIVE0646";

    @Inject
    CountryStateUtils countryStateUtils;

    public boolean verify(Address old, Address entered) {
        if (StringUtils.isBlank(entered.getCountry())) {
            entered.setCountry(old.getCountry());
        }

        if (old.equals(entered)) {
            return true;
        }
        try {
            Address corrected = getCorrectedAddress(old);
            return corrected.equals(entered);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return false;

    }

    @Repeat
    public Address getCorrectedAddress(Address address) {
        String xmlRequest = addressToXMLRequest(address);
        String endpoint = USPS_ADDRESS_VRF_ENDPOINT + Strings.encode(xmlRequest);
        String response = get(endpoint);
        LOGGER.info(response);
        if (StringUtils.contains(response, "The address you entered was found but more information is needed")) {
            address.setAddress2("APT " + address.getAddress2());
            throw new BusinessException("The address you entered was found but more information is needed. " + response);
        }


        Address correctedAddress = parseResponse(response);
        correctedAddress.setCountry(address.getCountry());


        return correctedAddress;
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public String get(String url) {
        try {
            LOGGER.info(url);
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            LOGGER.error("", e);
            throw Lang.wrapThrow(e);
        }
    }

    public Address parseResponse(String xmlString) {
        LOGGER.error(xmlString);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document doc;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            LOGGER.error("", e);
            throw new BusinessException(e);
        }

        doc.getDocumentElement().normalize();

        NodeList addressList = doc.getElementsByTagName("Address");
        if (addressList.getLength() == 0) {
            throw new BusinessException("No result returned");
        }

        Node addressNode = addressList.item(0);
        Element addressElement = (Element) addressNode;
        if (addressElement.getElementsByTagName("Error").getLength() > 0) {
            throw new BusinessException("Error, " + addressElement.getElementsByTagName("Error").item(0).getTextContent());
        }

        Address address = new Address();
        try {
            address.setAddress1(addressElement.getElementsByTagName("Address1").item(0).getTextContent());
        } catch (Exception e) {
            address.setAddress1("");
        }
        try {
            address.setAddress2(addressElement.getElementsByTagName("Address2").item(0).getTextContent());
        } catch (Exception e) {
            address.setAddress2("");
        }

        address.setCity(addressElement.getElementsByTagName("City").item(0).getTextContent());
        address.setState(addressElement.getElementsByTagName("State").item(0).getTextContent());

        try {
            address.setZip4(addressElement.getElementsByTagName("Zip4").item(0).getTextContent());
        } catch (Exception e) {
            address.setZip4("");
        }

        address.setZip5(addressElement.getElementsByTagName("Zip5").item(0).getTextContent());

        return address;
    }

    public String addressToXMLRequest(Address address) {
        String request = "<AddressValidateRequest USERID=\"%s\"><Address>" +
                "<Address1>%s</Address1>" +
                "<Address2>%s</Address2>" +
                "<City>%s</City>" +
                "<State>%s</State>" +
                "<Zip5>%s</Zip5>" +
                "<Zip4>%s</Zip4>" +
                "</Address>" +
                "</AddressValidateRequest>";

        return String.format(request,
                USERNAME,
                address.getAddress2(), address.getAddress1(), address.getCity(),
                countryStateUtils.getUSStateAbbr(address.getState()), address.getZip5(), address.getZip4());
    }

    public static void main(String[] args) {
        USPSAddressValidator validator = ApplicationContext.getBean(USPSAddressValidator.class);

        // Entered Address(address1=10248 PRINCE PL APT T1, address2=, city=UPPER MARLBORO, state=MD, zip=20774-1220, zip5=20774, zip4=1220, country=United States),
        //origin Address(address1=10248 Prince Place, address2=T1, city=Upper Marlboro, state=MD, zip=20774, zip5=20774, zip4=, country=United States)
        //3142 W George St C3, address2=, city=Chicago, state=IL, zip=60618, zip5=60618, zip4=, country=United States
        Address address = new Address();
        address.setAddress1("3142 W George St C3");
        address.setAddress2("");
        address.setCity("Chicago");
        address.setState("Illinois");
        address.setZip("60618");
        address.setCountry("United States");

        Address enteredAddress = new Address();
        enteredAddress.setAddress1("3142 W GEORGE ST C3");
        enteredAddress.setAddress2("");
        enteredAddress.setCity("CHICAGO");
        enteredAddress.setState("IL");
        enteredAddress.setZip("60618-7626");
        enteredAddress.setCountry("United States");

        System.out.println(validator.verify(address, enteredAddress));

    }
}
