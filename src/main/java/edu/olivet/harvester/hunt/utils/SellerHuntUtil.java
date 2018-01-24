package edu.olivet.harvester.hunt.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 6:00 PM
 */
public class SellerHuntUtil {

    public static List<Country> countriesToHunt(Order order) {
        List<Country> countries = new ArrayList<>();

        //order sales channel country
        countries.add(OrderCountryUtils.getMarketplaceCountry(order));

        if (order.isIntlOrder()) {
            try {
                Country country = Country.fromCode(CountryStateUtils.getInstance().getCountryCode(order.ship_country));
                countries.add(country);
            } catch (Exception e) {
                //
            }

            switch (OrderCountryUtils.getMarketplaceCountry(order)) {
                case US:
                    countries.add(Country.UK);
                    countries.add(Country.CA);
                    break;
                default:
                    countries.add(Country.US);
            }
        }

        return countries.stream().distinct().collect(Collectors.toList());
    }
}
