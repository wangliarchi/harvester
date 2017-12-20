package edu.olivet.harvester.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.model.OrderEnums;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Guice
public class CarrierTest {
    @Inject private Carrier carrier;

    @Test
    public void testGetCarrierCodeByCountryAndType() throws Exception {

        //us product
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.US, OrderEnums.OrderItemType.BOOK),"USPS");

        //us book
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.US, OrderEnums.OrderItemType.PRODUCT),"USPS");

        //ca product
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.CA, OrderEnums.OrderItemType.PRODUCT),"DHL Global Mail");

        //ca book
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.CA, OrderEnums.OrderItemType.BOOK),"Canada Post");

        //uk product
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.UK, OrderEnums.OrderItemType.PRODUCT),"DHL Global Mail");

        //uk book
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.UK, OrderEnums.OrderItemType.BOOK),"Royal Mail");

        //de product
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.DE, OrderEnums.OrderItemType.PRODUCT),"DHL Global Mail");

        //de book
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.DE, OrderEnums.OrderItemType.BOOK),"Royal Mail");


        //MX product
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.MX, OrderEnums.OrderItemType.PRODUCT),"DHL Global Mail");

        //MX book
        assertEquals(carrier.getCarrierCodeByCountryAndType(Country.MX, OrderEnums.OrderItemType.BOOK),"DHL Global Mail");
    }


}