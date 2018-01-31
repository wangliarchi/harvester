package edu.olivet.harvester.hunt.utils;

import com.google.common.collect.Lists;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import edu.olivet.harvester.hunt.service.HuntVariableService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 6:00 PM
 */
public class SellerHuntUtils {

    public static Map<Country, List<SellerFullType>> countriesToHunt(Order order) {
        Map<Country, List<SellerFullType>> countries = new HashMap<>();

        //order sales channel country
        if (order.isDomesticOrder()) {
            //国内单， 默认国内直寄
            countries.put(OrderCountryUtils.getMarketplaceCountry(order),
                    Lists.newArrayList(SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect));
        } else {


            HuntVariableService huntVariableService = ApplicationContext.getBean(HuntVariableService.class);
            countries.putAll(huntVariableService.supportedIntlTypes(order));

            //国际单
            //如果 ship to country 有 Amazon，在当地国家找
            try {
                Country country = Country.fromCode(CountryStateUtils.getInstance().getCountryCode(order.ship_country));
                if (country != Country.JP) {
                    addCountry(countries, country, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
                }
            } catch (Exception e) {
                //
            }
        }

        return countries;
    }

    public static void addCountry(Map<Country, List<SellerFullType>> countries, Country country, SellerFullType... types) {
        List<SellerFullType> currentTypes = countries.getOrDefault(country, new ArrayList<>());
        currentTypes.addAll(Lists.newArrayList(types));
        countries.put(country, currentTypes.stream().distinct().collect(Collectors.toList()));
    }

    public static String determineRemarkAppendix(Seller seller, Order order) {
        Country salesChannelCountry = OrderCountryUtils.getMarketplaceCountry(order);
        switch (salesChannelCountry) {
            case US:
                if (seller.getOfferListingCountry() != salesChannelCountry || order.isIntlOrder()) {
                    return seller.getRemarkAppendix(order);
                }
                break;
        }


        return "";
    }
}
