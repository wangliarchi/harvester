package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.HuntStandardSettings;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.logger.SellerHuntingLogger;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 7:32 PM
 */
@Singleton
public class SellerFilter {
    @Inject ForbiddenSellerService forbiddenSellerService;
    @Inject Now now;

    public boolean isQualified(Seller seller, Order order) {
        return isPreliminaryQualified(seller, order) &&
                sellerRatingQualified(seller, order) &&
                conditionQualified(seller, order);
    }

    public boolean isPreliminaryQualified(Seller seller, Order order) {
        if (seller.getShipFromCountry() != OrderCountryUtils.getMarketplaceCountry(order)) {
            SellerHuntingLogger.info("Seller {} not qualified as its shipped from {}", seller, seller.getShipFromCountry());

            return false;
        }

        if (!sellerOverallRatingQualified(seller, order)) {
            return false;
        }

        if (seller.isPt() && forbiddenSellerService.isForbidden(seller)) {
            SellerHuntingLogger.info("Seller {} not qualified as its forbidden seller", seller);
            return false;
        }

        return profitQualified(seller, order) && eddQualified(seller);

    }


    public boolean profitQualified(Seller seller, Order order) {
        Float maxLoss = HuntStandardSettings.load().getMaxProfitLoss();
        boolean result = order.getOrderTotalPrice().toUSDAmount().floatValue() - seller.getTotalPriceInUSD() > maxLoss;

        if (result == false) {
            SellerHuntingLogger.info("Seller {} not qualified as profit over max loss", seller);
        }
        return result;
    }

    public boolean eddQualified(Seller seller) {
        boolean result = seller.getLatestDeliveryDate() == null ||
                seller.getLatestDeliveryDate().before(DateUtils.addDays(now.get(), 14));

        if (!result) {
            SellerHuntingLogger.info("Seller {} not qualified as its edd {} too long", seller, seller.getLatestDeliveryDate());
        }

        return result;
    }

    public boolean sellerOverallRatingQualified(Seller seller, Order order) {
        HuntStandard huntStandard = HuntStandardSettings.load().getHuntStandard(order);

        if (seller.getRating() < huntStandard.getYearlyRating().getPositive()) {
            SellerHuntingLogger.info("Seller {} not qualified as its yearly rating {}% is too low", seller, seller.getRating());
            return false;
        }

        if (seller.getRatingCount() < huntStandard.getYearlyRating().getCount()) {
            SellerHuntingLogger.info("Seller {} not qualified as its overall rating count {} is too low", seller, seller.getRatingCount());
            return false;
        }

        return true;
    }

    public boolean sellerRatingQualified(Seller seller, Order order) {
        HuntStandard huntStandard = HuntStandardSettings.load().getHuntStandard(order);

        if (!huntStandard.monthlyRatingQualified(seller.getRatingByType(RatingType.Last30Days))) {
            SellerHuntingLogger.info("Seller {} not qualified as its monthly rating {} is too low", seller, seller.getRatingByType(RatingType.Last30Days));
            return false;
        }

        if (!huntStandard.yearlyRatingQualified(seller.getRatingByType(RatingType.Last12Month))) {
            SellerHuntingLogger.info("Seller {} not qualified as its yearly rating {} is too low", seller, seller.getRatingByType(RatingType.Last12Month));
            return false;
        }


        if (seller.getCondition().acceptable()) {
            if (seller.getRatingByType(RatingType.Last30Days).getPositive() < 95 ||
                    seller.getRatingByType(RatingType.Last30Days).getCount() < 10) {

                SellerHuntingLogger.info("Seller {} not qualified as its monthly rating {} is too low for acceptable condition", seller, seller.getRatingByType(RatingType.Last30Days));

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

        if (seller.getCondition().acceptable()) {
            if (order.getOrderTotalPrice().toUSDAmount().floatValue() > 40) {
                SellerHuntingLogger.info("Seller {} not qualified as its price {} is too high for acceptable condition", seller, order.getOrderTotalPrice().usdText());

                return false;
            }

            if (Strings.containsAnyIgnoreCase(seller.getConditionDetail(), "water", "damage", "heavy", "loose", "ink", "stain")) {
                SellerHuntingLogger.info("Seller {} not qualified as its condition is not good for acceptable condition", seller);
                return false;
            }
        }

        boolean result = ConditionUtils.goodToGo(seller.getCondition(), order.originalCondition());

        if (!result) {
            SellerHuntingLogger.info("Seller {} not qualified as its condition {} is too low for order condition {}", seller, seller.getCondition(), order.original_condition);
        }

        return true;
    }
}
