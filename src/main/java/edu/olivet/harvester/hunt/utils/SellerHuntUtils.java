package edu.olivet.harvester.hunt.utils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import edu.olivet.harvester.hunt.service.HuntVariableService;
import edu.olivet.harvester.hunt.service.SellerComparator;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.recycler.Recycler.C;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/23/18 6:00 PM
 */
public class SellerHuntUtils {
    @Inject HuntVariableService huntVariableService;
    @Inject Now now;

    public Map<Country, Set<SellerFullType>> countriesToHunt(Order order) {
        Map<Country, Set<SellerFullType>> countries = new HashMap<>();

        Country saleChannelCountry = OrderCountryUtils.getMarketplaceCountry(order);

        //add order marketplace country if supported
        if (countriesToHunt().contains(saleChannelCountry)) {
            addCountry(countries, saleChannelCountry, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
        }

        //国际单
        if (!order.isDomesticOrder()) {
            try {
                Country shipToCountry = Country.fromCode(CountryStateUtils.getInstance().getCountryCode(order.ship_country));
                if (countriesToHunt().contains(shipToCountry)) {
                    addCountry(countries, shipToCountry, SellerFullType.APDirect, SellerFullType.PrimeDirect, SellerFullType.PtDirect);
                }
            } catch (Exception e) {
                //
            }

            huntVariableService.supportedIntlTypes(order).forEach((country, types) -> {
                addCountry(countries, country, types.toArray(new SellerFullType[types.size()]));
            });
        }

        addUKFwd(countries, order);
        addUSFwd(countries, order);

        return countries;
    }

    /**
     * <pre>
     *     当ship to country 不是UK，而且EDD 允许的情况下，找UK FWD 单
     * </pre>
     */
    private void addUKFwd(Map<Country, Set<SellerFullType>> countries, Order order) {
        if (Country.UK.code().equalsIgnoreCase(CountryStateUtils.getInstance().getCountryCode(order.ship_country))) {
            return;
        }

        if (order.eddDays(now.get()) < 2 * 7) {
            return;
        }

        addCountry(countries, Country.UK, SellerFullType.APExport, SellerFullType.PrimeExport, SellerFullType.PtExport);
    }

    /**
     * <pre>
     *     当ship to country 不是US，而且EDD 允许的情况下，找US FWD 单
     * </pre>
     */
    private void addUSFwd(Map<Country, Set<SellerFullType>> countries, Order order) {
        if (Country.US.code().equalsIgnoreCase(CountryStateUtils.getInstance().getCountryCode(order.ship_country))) {
            return;
        }

        if (order.eddDays(now.get()) < 2 * 7) {
            return;
        }

        addCountry(countries, Country.US, SellerFullType.APExport, SellerFullType.PrimeExport, SellerFullType.PtExport);
    }


    public void addCountry(Map<Country, Set<SellerFullType>> countries, Country country, SellerFullType... types) {
        Set<SellerFullType> currentTypes = countries.getOrDefault(country, new HashSet<>());
        currentTypes.addAll(Lists.newArrayList(types));
        countries.put(country, currentTypes);
    }

    /**
     * <pre>
     * US BOOK
     * 国内单不标记
     * US直寄：不标记
     * US转运：US FWD
     * UK直寄 : UK SHIPMENT
     *
     * CA BOOK
     * 订单产生国seller直寄 : 不做标记
     * US直寄：US SHIPMENT
     * US转运：US FWD
     * UK直寄 : UK SHIPMENT
     *
     * EU BOOK
     * remark (不区分国内单还是国际单, 就是seller所在国家名 + SHIPMENT 或者 FWD)
     * 比如：
     * 订单产生国AP直寄 : 本国 SHIPMENT
     * 订单产生国非AP Prime直寄 : 本国 SHIPMENT
     * 订单产生国一般seller直寄 : 本国 SHIPMENT
     * UKAP直寄 ：UK SHIPMENT 目前什么都不标，是默认值
     * 非AP Prime，UK一般seller直寄 : UK SHIPMENT 目前什么都不标，是默认值
     * UK一般seller转运 : UK FWD
     * US AP直寄：US SHIPMENT
     *
     * PRODUCT
     * Us卖场的seller，不做标记。
     * Uk卖场中，如找uk seller，分2种情况：
     * 客人所在地为uk之外：uk fwd
     * 客人所在地为uk：uk shipment
     * </pre>
     */
    public static String determineRemarkAppendix(Seller seller, Order order) {


        SellerFullType type = seller.getFullType(order);
        String remark = seller.getOfferListingCountry().name() + " " + (type.isDirectShip() ? "Shipment" : "FWD");

        String defaultRemark = defaultRemarkAppendix(order);
        if (StringUtils.equalsIgnoreCase(defaultRemark, remark)) {
            return "";
        }

        return remark;

    }

    public static String defaultRemarkAppendix(Order order) {
        Country salesChannelCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType itemType = OrderItemTypeHelper.getItemTypeBySku(order);

        if (salesChannelCountry == Country.US) {
            return "US Shipment";
        }

        if (salesChannelCountry.europe()) {
            if (itemType == OrderItemType.BOOK) {
                return "UK Shipment";
            }

            return "US FWD";
        }

        if (itemType == OrderItemType.BOOK) {
            return salesChannelCountry.name() + " Shipment";
        } else {
            return "US FWD";
        }
    }

    public static List<Country> countriesToHunt() {
        return Lists.newArrayList(Country.US, Country.CA, Country.UK, Country.DE, Country.FR, Country.ES, Country.IT);
    }

    public static void sortSellers(List<Seller> sellers) {
        sellers.sort(SellerComparator.getInstance());
    }
}
