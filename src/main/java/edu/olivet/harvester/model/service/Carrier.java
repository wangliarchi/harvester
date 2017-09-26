package edu.olivet.harvester.model.service;

import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.model.OrderEnums;

@Singleton
public class Carrier {

    /**
     * get default carrier code for an order
     * US book/product : USPS
     * EU book: Royal Mail
     * EU Product: DHL Global Mail
     * Ca Book： Canada Post
     * Ca Product：DHL Global Mail
     * Mx product/book： DHL Global Mail
     *
     * @param
     * @return
     */
    public String getCarrierCodeByCountryAndType(Country country, OrderEnums.OrderItemType orderItemType) {
//        System.out.println(country);
//        System.out.println(orderItemType);
        //US book/product : USPS
        if (country == Country.US) {
            return "USPS";
        }

        //All non-US products:  DHL Global Mail
        if (orderItemType == OrderEnums.OrderItemType.PRODUCT) {
            return "DHL Global Mail";
        }

        //Mx product/book： DHL Global Mail
        if (country == Country.MX) {
            return "DHL Global Mail";
        }

        //EU book: Royal Mail
        if (country.europe() && orderItemType == OrderEnums.OrderItemType.BOOK) {
            return "Royal Mail";
        }

        //Ca Book： Canada Post
        if (country == Country.CA && orderItemType == OrderEnums.OrderItemType.BOOK) {
            return "Canada Post";
        }

        //default to USPS

        return "USPS";

    }
}
