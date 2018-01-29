package edu.olivet.harvester.hunt.utils;

import com.google.common.collect.Lists;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 6:00 PM
 */
public class SellerHuntUtil {

    public static Map<Country, List<SellerFullType>> countriesToHunt(Order order) {
        Map<Country, List<SellerFullType>> countries = new HashMap<>();

        //order sales channel country
        if (order.isDomesticOrder()) {
            //国内单， 默认国内直寄
            countries.put(OrderCountryUtils.getMarketplaceCountry(order),
                    Lists.newArrayList(SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect));
        } else {
            //国际单
            //如果 ship to country 有 Amazon，在当地国家找
            try {
                Country country = Country.fromCode(CountryStateUtils.getInstance().getCountryCode(order.ship_country));
                addCountry(countries, country, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
            } catch (Exception e) {
                //
            }

            //add US
            addCountry(countries, Country.US, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect,
                    SellerFullType.APExport, SellerFullType.PrimeExport, SellerFullType.PtExport);

            switch (OrderCountryUtils.getMarketplaceCountry(order)) {
                case US:
                    addCountry(countries, Country.UK, SellerFullType.APDirect);
                    addCountry(countries, Country.CA, SellerFullType.APDirect);
                    break;
                default:
                    break;
            }
        }

        return countries;
    }

    public static void addCountry(Map<Country, List<SellerFullType>> countries, Country country, SellerFullType... types) {
        List<SellerFullType> currentTypes = countries.getOrDefault(country, new ArrayList<>());
        currentTypes.addAll(Lists.newArrayList(types));
        countries.put(country, currentTypes);
    }

    public static String determineRemarkAppendix(Seller seller, Order order) {

        return "";
    }
}
