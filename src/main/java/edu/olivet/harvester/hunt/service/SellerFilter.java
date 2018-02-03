package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.HuntStandardSettings;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import org.apache.commons.lang3.time.DateUtils;

import java.util.List;
import java.util.Set;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 7:32 PM
 */
@Singleton
public class SellerFilter {
    private static final SellerHuntingLogger LOGGER = SellerHuntingLogger.getInstance();
    @Inject ForbiddenSellerService forbiddenSellerService;
    @Inject HuntVariableService huntVariableService;
    @Inject Now now;

    public boolean isQualified(Seller seller, Order order) {
        return isPreliminaryQualified(seller, order) &&
                sellerRatingQualified(seller, order) &&
                conditionQualified(seller, order);
    }

    public boolean isPreliminaryQualified(Seller seller, Order order) {
        if (seller.getShipFromCountry() != seller.getOfferListingCountry()) {
            LOGGER.info(order, "Seller [{}] not qualified as its shipped from {}, full info {}",
                    seller.getName(), seller.getShipFromCountry(), seller);
            return false;
        }

        if (!sellerOverallRatingQualified(seller, order)) {
            return false;
        }
        if (!notOutOfStock(seller, order)) {
            return false;
        }

        return checkAddon(seller, order) && notForbiddenSeller(seller, order) && profitQualified(seller, order) && eddQualified(seller, order);

    }

    public boolean notOutOfStock(Seller seller, Order order) {
        if (!seller.isInStock()) {
            LOGGER.info(order, "Seller [{}] not qualified as its out of stock, full info {}",
                    seller.getName(), seller);
            return false;
        }
        return true;
    }

    public boolean notForbiddenSeller(Seller seller, Order order) {
        if (seller.isPt() && forbiddenSellerService.isForbidden(seller)) {
            LOGGER.info(order, "Seller [{}] not qualified as its forbidden seller, full info {}",
                    seller.getName(), seller);
            return false;
        }

        return true;
    }

    public boolean checkAddon(Seller seller, Order order) {
        //if (seller.isAddOn() && order.isDomesticOrder()) {
        //    LOGGER.info(order, "Seller [{}] not qualified as it's addon, not allowed for domestic orders, full info {}",
        //            seller.getName(), seller);
        //    return false;
        //}

        return true;
    }

    public boolean profitQualified(Seller seller, Order order) {
        Float maxLoss = HuntStandardSettings.load().getMaxProfitLoss();
        Float profit = seller.profit(order);
        boolean result = profit > maxLoss;

        if (!result) {
            LOGGER.info(order, "Seller [{}] not qualified as profit {} over max loss {}, full info {}",
                    seller.getName(), profit, maxLoss, seller);
        }
        return result;
    }

    public boolean eddQualified(Seller seller, Order order) {
        boolean result = seller.getLatestDeliveryDate() == null ||
                seller.getLatestDeliveryDate().before(DateUtils.addDays(now.get(), 14));

        if (!result) {
            LOGGER.info(order, "Seller [{}] not qualified as its edd {} too long, full info {}",
                    seller.getName(), seller.getLatestDeliveryDate(), seller);
        }

        return result;
    }

    public boolean sellerOverallRatingQualified(Seller seller, Order order) {
        //HuntStandard huntStandard = HuntStandardSettings.load().getHuntStandard(order);
        HuntStandard huntStandard = huntVariableService.getHuntStandard(seller, order);

        if (seller.getRating() < huntStandard.getYearlyRating().getPositive()) {
            LOGGER.info(order, "Seller [{}] not qualified as its yearly rating {}% is lower than standard {}%, full info {}",
                    seller.getName(), seller.getRating(), huntStandard.getYearlyRating().getPositive(), seller);
            return false;
        }

        if (seller.getRatingCount() < huntStandard.getYearlyRating().getCount()) {
            LOGGER.info(order, "Seller [{}] not qualified as its overall rating count {} is lower than standard {}, full info {}",
                    seller.getName(), seller.getRatingCount(), huntStandard.getYearlyRating().getCount(), seller);
            return false;
        }

        return true;
    }

    public boolean sellerRatingQualified(Seller seller, Order order) {
        HuntStandard huntStandard = HuntStandardSettings.load().getHuntStandard(order);

        if (!huntStandard.monthlyRatingQualified(seller.getRatingByType(RatingType.Last30Days))) {
            LOGGER.info(order, "Seller [{}] not qualified as its monthly rating {} is lower than standard {}, full info {}",
                    seller.getName(), seller.getRatingByType(RatingType.Last30Days), huntStandard.getMonthlyRating(), seller);
            return false;
        }

        if (!huntStandard.yearlyRatingQualified(seller.getRatingByType(RatingType.Last12Month))) {
            LOGGER.info(order, "Seller [{}] not qualified as its yearly rating {} is lower than standard {}, full info {}",
                    seller.getName(), seller.getRatingByType(RatingType.Last12Month), huntStandard.getYearlyRating(), seller);
            return false;
        }


        if (seller.getCondition().acceptable()) {
            if (seller.getRatingByType(RatingType.Last30Days).getPositive() < 95 ||
                    seller.getRatingByType(RatingType.Last30Days).getCount() < 10) {

                LOGGER.info(order, "Seller [{}] not qualified as its monthly rating {} is lower than {} for acceptable condition, {}",
                        seller.getName(), seller.getRatingByType(RatingType.Last30Days), "[95,10]", seller);

                return false;

            }
        }
        return true;
    }

    /**
     * <pre>
     * New：new；
     * Good：销售价在40美元或40美元以下的选acceptable 以上的condition，
     * 销售价在40美元或40美元以上的不选acceptable，只选good以上的condition；
     *
     * acceptable：acceptable 以上的condition
     * 针对acc的选择标准：
     * a，1年标注等同正常找单规则；
     * 1个月的rating 需要在95%，rating数是10个以上。
     * b，凡是包含water，damage，heavy，loose，ink，stain等字样，都不选；
     * </pre>
     */
    public boolean conditionQualified(Seller seller, Order order) {

        if (order.originalCondition().used() && seller.getCondition().acceptable()) {
            if (order.getOrderTotalPrice().toUSDAmount().floatValue() > 40) {
                LOGGER.info("Seller [{}] not qualified as its price {} is higher than {} for acceptable condition - {}",
                        seller.getName(), order.getOrderTotalPrice().usdText(), "$40", seller);

                return false;
            }

            if (Strings.containsAnyIgnoreCase(seller.getConditionDetail(), "water", "damage", "heavy", "loose", "ink", "stain")) {
                LOGGER.info("Seller [{}] not qualified as its condition is not good for acceptable condition",
                        seller.getName());
                return false;
            }

            return true;
        }

        boolean result = ConditionUtils.goodToGo(order.originalCondition(), seller.getCondition());

        if (!result) {
            LOGGER.info("Seller [{}] not qualified as its condition {} is too low for order condition {} - {}",
                    seller.getName(), seller.getCondition(), order.original_condition, seller);
        }

        return result;
    }

    public boolean typeAllowed(Seller seller, Order order, Set<SellerFullType> allowedTypes) {

        List<SellerFullType> types = seller.supportedFullTypes(order.ship_country);
        for (SellerFullType type : types) {
            if (allowedTypes.contains(type)) {
                seller.setFullType(type);
                return true;
            }
        }

        LOGGER.info("Seller [{}] not qualified as its type is not allowed, supported {}, allowed {} - {}",
                seller.getName(), types, allowedTypes, order.original_condition, seller);

        return false;
    }
}
