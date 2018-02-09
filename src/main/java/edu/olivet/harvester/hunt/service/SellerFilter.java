package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Now;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.HuntStandardSettings;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 7:32 PM
 */
@Singleton
public class SellerFilter {
    private static final SellerHuntingLogger LOGGER = SellerHuntingLogger.getLogger(SellerFilter.class);
    @Inject ForbiddenSellerService forbiddenSellerService;
    @Inject HuntVariableService huntVariableService;
    @Inject OrderItemTypeHelper orderItemTypeHelper;
    @Inject Now now;

    public boolean isQualified(Seller seller, Order order) {
        return isPreliminaryQualified(seller, order) &&
                sellerRatingQualified(seller, order) &&
                conditionQualified(seller, order);
    }

    public boolean isPreliminaryQualified(Seller seller, Order order) {

        if (!shipFromCountryValid(seller, order)) {
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

    /**
     * <pre>
     * 欧洲书单：
     *     非本国发货，只能选uk发货seller，而且seller价格要在50以内。
     *     【对于de fr es it 四个国家的国内单，销售价在50以内的，可以选择ship from uk 的
     * 注：
     * 无论是哪个卖场产生的订单，
     * 无论是哪个国家的收件人，
     * 无论是在哪个amazon卖场做单，
     * 只要是uk发货，seller销售价50以内的都可以选。
     * Uk 卖场没有50限制。
     * 】
     * </pre>
     */
    public boolean shipFromCountryValid(Seller seller, Order order) {
        Country saleChannelCountry = OrderCountryUtils.getMarketplaceCountry(order);

        //同一个国家的，直接退出
        if (seller.getShipFromCountry() == seller.getOfferListingCountry()) {
            return true;
        }


        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);

        if (orderItemType == OrderItemType.BOOK) {
            // CA 书
            if (saleChannelCountry == Country.CA) {
                return true;
            }

            //欧洲书
            if (saleChannelCountry.europe()) {
                if (seller.getShipFromCountry() == Country.UK && seller.getTotalPrice() <= 50) {
                    return true;
                }
            }
        }

        LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its shipped from {}, full info {}",
                seller.getName(), seller.getShipFromCountry(), seller);

        return false;
    }

    public boolean notOutOfStock(Seller seller, Order order) {
        if (!seller.isInStock()) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its out of stock, full info {}",
                    seller.getName(), seller);
            return false;
        }
        return true;
    }

    public boolean notForbiddenSeller(Seller seller, Order order) {
        if (seller.isPt() && forbiddenSellerService.isForbidden(seller)) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its forbidden seller, full info {}",
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
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as profit {} over max loss {}, full info {}",
                    seller.getName(), profit, maxLoss, seller);
        }
        return result;
    }

    /**
     * for direct orders, seller edd must before order edd;
     * for exporting orders, seller edd must before 10 days from order edd
     */
    public boolean eddQualified(Seller seller, Order order) {
        int days = seller.canDirectShip(order.ship_country, orderItemTypeHelper.getItemType(order)) ? 0 : 10;
        Date latestAllowableEdd = DateUtils.addDays(order.latestEdd(), -days);
        boolean result = seller.getLatestDeliveryDate() == null ||
                seller.getLatestDeliveryDate().before(latestAllowableEdd);

        if (!result) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its edd {} too long, full info {}",
                    seller.getName(), seller.getLatestDeliveryDate(), seller);
        }

        return result;
    }

    public boolean sellerOverallRatingQualified(Seller seller, Order order) {
        //HuntStandard huntStandard = HuntStandardSettings.load().getHuntStandard(order);
        HuntStandard huntStandard = huntVariableService.getHuntStandard(seller, order);

        if (seller.getRating() < huntStandard.getYearlyRating().getPositive()) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its yearly rating {}% is lower than standard {}%, full info {}",
                    seller.getName(), seller.getRating(), huntStandard.getYearlyRating().getPositive(), seller);
            return false;
        }

        if (seller.getRatingCount() < huntStandard.getYearlyRating().getCount()) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its overall rating count {} is lower than standard {}, full info {}",
                    seller.getName(), seller.getRatingCount(), huntStandard.getYearlyRating().getCount(), seller);
            return false;
        }

        return true;
    }

    public boolean sellerRatingQualified(Seller seller, Order order) {
        HuntStandard huntStandard = HuntStandardSettings.load().getHuntStandard(order);

        if (!huntStandard.monthlyRatingQualified(seller.getRatingByType(RatingType.Last30Days))) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its monthly rating {} is lower than standard {}, full info {}",
                    seller.getName(), seller.getRatingByType(RatingType.Last30Days), huntStandard.getMonthlyRating(), seller);
            return false;
        }

        if (!huntStandard.yearlyRatingQualified(seller.getRatingByType(RatingType.Last12Month))) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its yearly rating {} is lower than standard {}, full info {}",
                    seller.getName(), seller.getRatingByType(RatingType.Last12Month), huntStandard.getYearlyRating(), seller);
            return false;
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
            if (order.getOrderTotalPrice().getAmount().floatValue() > 40) {
                LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its price {} is higher than {} for acceptable condition - {}",
                        seller.getName(), order.getOrderTotalPrice().usdText(), "$40", seller);
                return false;
            }

            if (Strings.containsAnyIgnoreCase(seller.getConditionDetail(), "water", "damage", "heavy", "loose", "ink", "stain")) {
                LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its condition is not good for acceptable condition",
                        seller.getName());
                return false;
            }

            if (seller.getRatingByType(RatingType.Last30Days) == null ||
                    seller.getRatingByType(RatingType.Last30Days).getPositive() < 95 ||
                    seller.getRatingByType(RatingType.Last30Days).getCount() < 10) {
                LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its monthly rating {} is lower than {} for acceptable condition, {}",
                        seller.getName(), seller.getRatingByType(RatingType.Last30Days), "[95,10]", seller);

                return false;

            }

            return true;
        }

        boolean result = ConditionUtils.goodToGo(order.originalCondition(), seller.getCondition());

        if (!result) {
            LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its condition {} is too low for order condition {} - {}",
                    seller.getName(), seller.getCondition(), order.original_condition, seller);
        }

        return result;
    }

    public boolean typeAllowed(Seller seller, Order order, Set<SellerFullType> allowedTypes) {
        List<SellerFullType> types = seller.supportedFullTypes(order.ship_country, orderItemTypeHelper.getItemType(order));
        for (SellerFullType type : types) {
            if (allowedTypes.contains(type)) {
                seller.setFullType(type);
                return true;
            }
        }

        LOGGER.setOrder(order).getLogger().info("Seller [{}] not qualified as its type is not allowed, supported {}, allowed {} - {}",
                seller.getName(), types, allowedTypes, order.original_condition, seller);

        return false;
    }
}
