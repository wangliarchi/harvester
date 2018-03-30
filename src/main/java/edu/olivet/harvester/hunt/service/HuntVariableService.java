package edu.olivet.harvester.hunt.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.Rating;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.common.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/22/2018 3:43 PM
 */
@Singleton
public class HuntVariableService extends AppScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuntVariableService.class);

    private static final String APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbz9HNzArF0rA5jxXlrBfc4CYm7Vy-iU0RSsi9nmgaSrQLfQKKY/exec";
    private Map<String, JSONObject> VARIABLE_MAP = new HashMap<>();
    private static final Float AP_TAX = 0.0225f;
    @Inject OrderItemTypeHelper orderItemTypeHelper;
    @Inject SellerHuntUtils sellerHuntUtils;

    public enum Type {
        Seller,
        Rating,
        Shipping,
        IntlShipping,
        MinRating
    }


    public void setHuntingVariable(Seller seller, Order order) {
        //seller variable
        setSellerVariable(seller, order);

        //rating variable
        setRatingVariable(seller, order);

        setTaxVariable(seller, order);

        //shipping variable
        setIntlShippingVariable(seller, order);


        //if condition is lower
        if (seller.getCondition().score() < order.originalCondition().score()) {
            seller.setSellerVariable(seller.getSellerVariable() + 10);
        }
    }

    public HuntStandard getHuntStandard(Seller seller, Order order) {
        Country country = seller.getOfferListingCountry();
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        Condition condition = seller.getCondition();
        return getHuntStandard(country, orderItemType, condition);
    }

    public HuntStandard getHuntStandard(Country country, OrderItemType orderItemType, Condition condition) {
        JSONObject minRatingVariables = getVariables(Type.MinRating, country, orderItemType);

        HuntStandard.Type type = HuntStandard.Type.init(orderItemType, condition);
        String key = type.getDesc();
        HuntStandard standard = HuntStandard.getByType(type);

        JSONObject minRatings = null;
        for (Map.Entry<String, Object> entry : minRatingVariables.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                minRatings = (JSONObject) entry.getValue();
                break;
            }
        }

        if (minRatings == null) {
            return standard;
        }

        try {
            int positive;
            try {
                positive = (int) (minRatings.getBigDecimal("yearlyPositive").floatValue() * 100);
            } catch (Exception e) {
                positive = Integer.parseInt(minRatings.getString("yearlyPositive").replace("%s", ""));
            }
            standard.setYearlyRating(new Rating(positive, minRatings.getInteger("yearlyRating"), RatingType.Last12Month));
        } catch (Exception e) {
            //
        }

        try {
            int positive;
            try {
                positive = (int) (minRatings.getBigDecimal("monthlyPositive").floatValue() * 100);
            } catch (Exception e) {
                positive = Integer.parseInt(minRatings.getString("monthlyPositive").replace("%s", ""));
            }
            standard.setMonthlyRating(new Rating(positive, minRatings.getInteger("monthlyRating"), RatingType.Last12Month));
        } catch (Exception e) {
            //
        }
        return standard;
    }

    private String getKey(Type type, Country country, OrderItemType orderItemType) {
        return type.name() + country.name() + orderItemType.name();
    }

    public JSONObject getVariables(Type type, Country country, OrderItemType orderItemType) {
        String key = getKey(type, country, orderItemType);
        return VARIABLE_MAP.computeIfAbsent(key, k -> getVariablesOnline(type, country, orderItemType));
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public JSONObject getVariablesOnline(Type type, Country country, OrderItemType orderItemType) {
        Map<String, String> params = new HashMap<>();
        params.put("method", type.name().toLowerCase());
        params.put("country", country.name());
        params.put("t", orderItemType.name());
        try {
            String json = this.processResult(this.get(params));
            return JSON.parseObject(json);
        } catch (Exception e) {
            LOGGER.error("Failed to get variables for {} {} {}", type, country, orderItemType);
            throw new BusinessException(e);
        }
    }


    @Repeat(expectedExceptions = BusinessException.class)
    protected String get(Map<String, String> params) {
        String params4Url = this.params2Url(params);
        String url = APP_SCRIPT_URL + params4Url;
        try {
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }


    public void setSellerVariable(Seller seller, Order order) {
        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        float orderPrice = order.getOrderTotalPrice().getAmount().floatValue();

        //seller variable
        JSONObject sellerVariables = getVariables(Type.Seller, orderCountry, orderItemType);
        String key = seller.getOfferListingCountry().name() + " " + seller.getType().abbrev();
        String altKey = key;
        if (seller.getOfferListingCountry() != Country.US && (!seller.isIntlSeller(order)) || orderCountry == seller.getOfferListingCountry()) {
            altKey = "Local " + seller.getType().abbrev();
        }

        JSONObject sellerVariablesByPrices = null;
        for (Map.Entry<String, Object> entry : sellerVariables.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey()) || altKey.equalsIgnoreCase(entry.getKey())) {
                sellerVariablesByPrices = (JSONObject) entry.getValue();
                break;
            }
        }

        //JSONObject sellerVariablesByPrices = sellerVariables.getJSONObject(key);
        if (sellerVariablesByPrices == null) {
            LOGGER.error("Fail to find seller variables for {} {} {} {}", key, order.order_id, orderCountry, order.type());
            throw new BusinessException("Fail to find seller variables for " + key);
        }

        List<String> keys = new ArrayList<>(sellerVariablesByPrices.keySet());
        keys.sort((String m1, String m2) -> {
            if (Float.parseFloat(m1) == Float.parseFloat(m2)) {
                return 0;
            }
            return Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1;
        });

        JSONObject sellerVariablesByPrice = null;
        for (String price : keys) {
            if (orderPrice > Float.parseFloat(price)) {
                sellerVariablesByPrice = sellerVariablesByPrices.getJSONObject(price);
                break;
            }
        }

        if (sellerVariablesByPrice == null) {
            throw new BusinessException("No seller variables found for " + key + " " + orderPrice);
        }

        keys = new ArrayList<>(sellerVariablesByPrice.keySet());
        keys.sort((String m1, String m2) -> {
            if (Float.parseFloat(m1) == Float.parseFloat(m2)) {
                return 0;
            }
            return Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1;
        });

        float sellerVariable = 0;
        float profit = seller.profit(order);
        for (String price : keys) {
            if (profit > Float.parseFloat(price)) {
                try {
                    sellerVariable = sellerVariablesByPrice.getFloat(price);
                    break;
                } catch (Exception e) {
                    //LOGGER.info("{} {}",sellerVariablesByPrice,price);
                }
            }
        }


        //如果ship from 国家和 offer listing 所在国家不一样，加一个国家卖家的 权重系数
        if (seller.getShipFromCountry() != seller.getOfferListingCountry()) {
            sellerVariable += seller.getTotalPrice() * 0.1;
        }

        seller.setSellerVariable(sellerVariable);
    }

    /**
     * US AP tax
     */
    public void setTaxVariable(Seller seller, Order order) {

        if (!seller.isAP() || seller.getOfferListingCountry() != Country.US ||
                (!"US".equalsIgnoreCase(CountryStateUtils.getInstance().getCountryCode(order.ship_country)) && seller.isDirectShip())) {
            seller.setTaxVariable(0);
            return;
        }

        float taxRate = AP_TAX;
        //try {
        //    String stateName = order.ship_state;
        //    if (!"US".equalsIgnoreCase(CountryStateUtils.getInstance().getCountryCode(order.ship_country))) {
        //        stateName = FwdAddressUtils.getUSFwdAddress().getState();
        //    }
        //    State state = State.parse(stateName);
        //    taxRate = (float) state.getTaxRate();
        //} catch (Exception e) {
        //
        //}

        seller.setTaxVariable(seller.getTotalPriceInUSD() * taxRate);

    }

    public void setRatingVariable(Seller seller, Order order) {
        if (seller.isAP()) {
            seller.setRatingVariable(0);
            return;
        }

        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        float orderPrice = order.getOrderTotalPrice().getAmount().floatValue();

        //seller variable

        JSONObject sellerVariables = getVariables(Type.Rating, orderCountry, orderItemType);

        //sort by selling price
        List<String> keys = new ArrayList<>(sellerVariables.keySet());
        keys.sort((String m1, String m2) -> {
            if (Float.parseFloat(m1) == Float.parseFloat(m2)) {
                return 0;
            }
            return Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1;
        });

        JSONObject sellerVariablesByPrice = null;
        for (String price : keys) {
            if (orderPrice > Float.parseFloat(price)) {
                sellerVariablesByPrice = sellerVariables.getJSONObject(price);
                break;
            }
        }

        if (sellerVariablesByPrice == null) {
            throw new BusinessException("No rating variables found for" + orderPrice);
        }

        //sort by rating
        keys = new ArrayList<>(sellerVariablesByPrice.keySet());
        keys.sort((String m1, String m2) -> {
            if (Float.parseFloat(m1) == Float.parseFloat(m2)) {
                return 0;
            }
            return Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1;
        });

        //
        float ratingVariable = 0;
        for (String rating : keys) {
            if (seller.getRatingByType(RatingType.Last30Days).getPositive() >= Integer.parseInt(rating)) {
                ratingVariable = sellerVariablesByPrice.getFloat(rating);
                break;
            }
        }

        seller.setRatingVariable(ratingVariable);
    }


    public void setIntlShippingVariable(Seller seller, Order order) {
        if (!seller.isIntlSeller(order)) {
            setShippingVariableForDomestic(seller);
            return;
        }

        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        //shipping variable
        Map<String, Map<String, Map<String, Float>>> shippingMap = getIntlShippingVariable(orderCountry, orderItemType);
        String countryType = (seller.getShipFromCountry().name() + " " + orderItemType.name()).toLowerCase();
        if (!shippingMap.containsKey(countryType)) {
            throw new BusinessException("Fail to find shipping variables for " + countryType);
        }

        Map<String, Map<String, Float>> shippingMapForCountryType = shippingMap.get(countryType);
        String shipToCountry = CountryStateUtils.getInstance().getCountryCode(order.ship_country).toLowerCase();
        if ("gb".equalsIgnoreCase(shipToCountry)) {
            shipToCountry = "uk";
        }
        List<String> keys = Lists.newArrayList(shipToCountry);
        if (CountryStateUtils.getInstance().isEUCountry(shipToCountry)) {
            keys.add("eu");
        }
        keys.add("default");

        if (!MapUtils.containsAnyKey(shippingMapForCountryType, keys.toArray())) {
            throw new BusinessException("Fail to find shipping variables for " + countryType + " " + shipToCountry);
        }

        Map<String, Float> shippingMapForCountryTypeForCountry = MapUtils.getValue(shippingMapForCountryType, keys.toArray());

        List<SellerFullType> types = seller.supportedFullTypes(order.ship_country, orderItemTypeHelper.getItemType(order));
        Set<SellerFullType> allowedTypes = sellerHuntUtils.countriesToHunt(order).getOrDefault(seller.getOfferListingCountry(), null);
        for (SellerFullType type : types) {
            if (allowedTypes.contains(type)) {
                //US AP Direct
                String key = type.isDirectShip() ? type.desc() : "export";
                key = key.toLowerCase();
                //noinspection ConstantConditions
                if (shippingMapForCountryTypeForCountry.containsKey(key)) {
                    seller.setShippingVariable(shippingMapForCountryTypeForCountry.get(key));
                    seller.setFullType(type);
                    return;
                }
            }
        }

        throw new BusinessException("Fail to find shipping variables for " + countryType + " to " + shipToCountry + " via " + seller.getFullType(order));
    }


    public void setShippingVariableForDomestic(Seller seller) {
        if (seller.isPrime()) {
            //IT AP/Prime & price less than 29 euro,  need to add 2.7 euro shipping fee
            if (seller.getOfferListingCountry() == Country.IT && seller.getPrice().getAmount().floatValue() < 29) {
                seller.setShippingVariable(new Money(2.7f, Country.IT).toUSDAmount().floatValue());
                return;
            }
        }

        seller.setShippingVariable(0);
    }

    private static final Map<String, Map<String, Map<String, Float>>> SHIPPING_MAP = new HashMap<>();

    public Map<String, Map<String, Map<String, Float>>> getIntlShippingVariable(Country country, OrderItemType orderItemType) {
        if (SHIPPING_MAP.size() > 0) {
            return SHIPPING_MAP;
        }
        JSONObject sellerVariables = getVariables(Type.IntlShipping, country, orderItemType);
        for (Map.Entry<String, Object> entry : sellerVariables.entrySet()) {
            String countryType = entry.getKey().toLowerCase();
            Map<String, Map<String, Float>> countryTypeMap = SHIPPING_MAP.getOrDefault(countryType, new HashMap<>());

            JSONObject countryTypeVariables = sellerVariables.getJSONObject(countryType);
            for (Map.Entry<String, Object> countryEntry : countryTypeVariables.entrySet()) {
                String countryCode = countryEntry.getKey().toLowerCase();
                Map<String, Float> countryEntryMap = countryTypeMap.getOrDefault(countryCode, new HashMap<>());

                JSONObject detailVariable = countryTypeVariables.getJSONObject(countryCode);
                for (Map.Entry<String, Object> countryTypeEntry : detailVariable.entrySet()) {
                    try {
                        countryEntryMap.put(countryTypeEntry.getKey().toLowerCase(), Float.parseFloat(countryTypeEntry.getValue().toString()));
                    } catch (Exception e) {
                        //
                    }
                }

                countryTypeMap.put(countryCode, countryEntryMap);

            }


            SHIPPING_MAP.put(countryType, countryTypeMap);
        }

        return SHIPPING_MAP;
    }

    public Map<Country, Set<SellerFullType>> supportedIntlTypes(Order order) {
        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        //shipping variable
        JSONObject shippingVariables = getVariables(Type.Shipping, orderCountry, orderItemType);
        Map<Country, Set<SellerFullType>> supportedTypes = new HashMap<>();
        shippingVariables.forEach((k, v) -> {
            try {
                String[] parts = StringUtils.split(k, " ");
                SellerFullType type = SellerFullType.fromType(SellerType.getByCharacter(parts[1].trim()), "Direct".equalsIgnoreCase(parts[2]));
                sellerHuntUtils.addCountry(supportedTypes, Country.fromCode(parts[0].trim()), type);
            } catch (Exception e) {
                LOGGER.error("Fail to find seller variables for {}", e);
            }
        });

        return supportedTypes;
    }

    public static void main(String[] args) {
        HuntVariableService huntVariableService = ApplicationContext.getBean(HuntVariableService.class);
        huntVariableService.getIntlShippingVariable(Country.US, OrderItemType.BOOK);
    }

}
