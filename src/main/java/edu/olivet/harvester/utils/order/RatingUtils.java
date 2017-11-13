package edu.olivet.harvester.utils.order;

import edu.olivet.harvester.fulfill.model.Seller;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/1/17 9:23 AM
 */
public class RatingUtils {

    public static final String SELLER_RATING_URL = "%s/gp/aag/main/ref=olp_merch_rating?ie=UTF8&seller=%s";

    public static String sellerRatingUrl(Seller seller) {
        return String.format(seller.getCountry_OfferListing().baseUrl(), seller.getUuid());
    }
}
