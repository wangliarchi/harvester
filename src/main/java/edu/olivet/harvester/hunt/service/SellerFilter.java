package edu.olivet.harvester.hunt.service;

import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 7:32 PM
 */
public class SellerFilter {

    public void filterSllers(List<Seller> sellers, Order order) {
        //remove intl seller
        sellers.removeIf(it -> it.getShipFromCountry() != OrderCountryUtils.getMarketplaceCountry(order));

        //remove low rating sellers
    }
}
