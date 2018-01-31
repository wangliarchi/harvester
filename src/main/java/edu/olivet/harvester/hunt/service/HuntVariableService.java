package edu.olivet.harvester.hunt.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.common.service.OrderItemTypeHelper;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.hunt.model.Rating;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/22/2018 3:43 PM
 */
public class HuntVariableService extends AppScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuntVariableService.class);

    private static final String APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbz9HNzArF0rA5jxXlrBfc4CYm7Vy-iU0RSsi9nmgaSrQLfQKKY/exec";
    private Map<String, JSONObject> VARIABLE_MAP = new HashMap<>();
    private static final Float AP_TAX = 0.05f;

    public enum Type {
        Seller,
        Rating,
        Shipping,
        MinRating
    }

    @Inject OrderItemTypeHelper orderItemTypeHelper;

    public void setHuntingVariable(Seller seller, Order order) {
        //seller variable
        setSellerVariable(seller, order);

        //set rating variable
        setRatingVariable(seller, order);

        //set shipping variable
        if (seller.isIntlSeller(order)) {
            setIntlShippingVariable(seller, order);
        }

        //if condition is lower
        if (seller.getCondition().score() < order.originalCondition().score()) {
            seller.setSellerVariable(seller.getSellerVariable() + 10);
        }
    }


    public HuntStandard getHuntStandard(Seller seller, Order order) {
        Country country = seller.getOfferListingCountry();
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        Condition condition = seller.getCondition();
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

    public JSONObject getVariables(Type type, Country country, OrderItemType orderItemType) {
        String key = getKey(type, country, orderItemType);
        return VARIABLE_MAP.computeIfAbsent(key, k -> getVariablesOnline(type, country, orderItemType));
    }


    public JSONObject getVariablesOnline(Type type, Country country, OrderItemType orderItemType) {
        Map<String, String> params = new HashMap<>();
        params.put("method", type.name().toLowerCase());
        params.put("country", country.name());
        params.put("t", orderItemType.name());

        String json = this.processResult(this.get(params));

        return JSON.parseObject(json);


    }

    private String getKey(Type type, Country country, OrderItemType orderItemType) {
        return type.name() + country.name() + orderItemType.name();
    }

    protected String get(Map<String, String> params) {
        String params4Url = this.params2Url(params);
        String url = APP_SCRIPT_URL + params4Url;
        try {
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }


    private void setSellerVariable(Seller seller, Order order) {
        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        float orderPrice = order.getOrderTotalPrice().toUSDAmount().floatValue();

        //seller variable
        JSONObject sellerVariables = getVariables(Type.Seller, orderCountry, orderItemType);
        String key = seller.getOfferListingCountry().name() + " " + seller.getType().abbrev();
        if (seller.getOfferListingCountry() != Country.US && !seller.isIntlSeller(order)) {
            key = "Local " + seller.getType().abbrev();
        }

        JSONObject sellerVariablesByPrices = null;
        for (Map.Entry<String, Object> entry : sellerVariables.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                sellerVariablesByPrices = (JSONObject) entry.getValue();
                break;
            }
        }

        //JSONObject sellerVariablesByPrices = sellerVariables.getJSONObject(key);
        if (sellerVariablesByPrices == null) {
            LOGGER.error("Fail to find seller variables for {}", key);
            throw new BusinessException("Fail to find seller variables for " + key);
        }

        List<String> keys = sellerVariablesByPrices.keySet().stream().collect(Collectors.toList());
        keys.sort((m1, m2) -> Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1);

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

        keys = sellerVariablesByPrice.keySet().stream().collect(Collectors.toList());
        keys.sort((m1, m2) -> Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1);

        float sellerVariable = 0;
        for (String price : keys) {
            if (seller.profit(order) > Float.parseFloat(price)) {
                try {
                    sellerVariable = sellerVariablesByPrice.getFloat(price);
                    break;
                } catch (Exception e) {
                    //LOGGER.info("{} {}",sellerVariablesByPrice,price);
                }
            }
        }

        seller.setSellerVariable(sellerVariable);

        //AP TAX
        if (seller.isAP() && seller.getOfferListingCountry() == Country.US) {
            seller.setSellerVariable(seller.getSellerVariable() + seller.getTotalPriceInUSD() * AP_TAX);
        }
    }

    private void setRatingVariable(Seller seller, Order order) {
        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        float orderPrice = order.getOrderTotalPrice().toUSDAmount().floatValue();

        //seller variable
        JSONObject sellerVariables = getVariables(Type.Rating, orderCountry, orderItemType);

        List<String> keys = sellerVariables.keySet().stream().collect(Collectors.toList());
        keys.sort((m1, m2) -> Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1);

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

        keys = sellerVariablesByPrice.keySet().stream().collect(Collectors.toList());
        keys.sort((m1, m2) -> Float.parseFloat(m1) > Float.parseFloat(m2) ? -1 : 1);

        float ratingVariable = 0;
        for (String rating : keys) {
            if (seller.getRating() >= Integer.parseInt(rating)) {
                ratingVariable = sellerVariablesByPrice.getFloat(rating);
                break;
            }
        }

        seller.setRatingVariable(ratingVariable);
    }

    private void setIntlShippingVariable(Seller seller, Order order) {
        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        //shipping variable
        JSONObject sellerVariables = getVariables(Type.Shipping, orderCountry, orderItemType);


        List<SellerFullType> types = seller.supportedFullTypes(order.ship_country);
        List<SellerFullType> allowedTypes = SellerHuntUtils.countriesToHunt(order).getOrDefault(seller.getOfferListingCountry(), null);
        for (SellerFullType type : types) {
            if (allowedTypes.contains(type)) {
                //US AP Direct
                String key = seller.getOfferListingCountry().name() + " " + type.desc();
                try {
                    Float variable = sellerVariables.getFloat(key);
                    seller.setShippingVariable(variable);
                    seller.setFullType(type);
                    return;
                } catch (Exception e) {
                    LOGGER.error("No shipping setting for {} found", key);
                }
            }
        }

        throw new BusinessException("Fail to find shipping variables for " + seller);
    }


    public Map<Country, List<SellerFullType>> supportedIntlTypes(Order order) {
        Country orderCountry = OrderCountryUtils.getMarketplaceCountry(order);
        OrderItemType orderItemType = orderItemTypeHelper.getItemType(order);
        //shipping variable
        JSONObject shippingVariables = getVariables(Type.Shipping, orderCountry, orderItemType);
        Map<Country, List<SellerFullType>> supportedTypes = new HashMap<>();
        shippingVariables.forEach((k, v) -> {
            //US AP Direct
            try {
                String[] parts = StringUtils.split(k, " ");
                SellerFullType type = SellerFullType.fromType(SellerType.getByCharacter(parts[1]), "Direct".equalsIgnoreCase(parts[2]) ? true : false);
                SellerHuntUtils.addCountry(supportedTypes, Country.fromCode(parts[0]), type);
            } catch (Exception e) {
                LOGGER.error("Fail to find seller variables for {}", e);
            }
        });

        return supportedTypes;
    }

    public static void main(String[] args) {
        HuntVariableService huntVariableService = ApplicationContext.getBean(HuntVariableService.class);
        //huntVariableService.getVariables(Type.Seller, Country.US, OrderItemType.BOOK);

        Seller seller = new Seller();
        seller.setShipFromCountry(Country.US);
        seller.setOfferListingCountry(Country.US);
        seller.setCondition(Condition.Used);
        seller.setType(SellerType.Pt);
        seller.setRating(91);
        seller.setPrice(new Money(15, Country.US));
        seller.setShippingFee(new Money(2.99f, Country.US));

        Order order = new Order();
        order.sales_chanel = "Amazon.com";
        order.ship_country = "United States";
        order.sku = "BKXXX";
        order.price = "51.00";
        order.shipping_fee = "2.99";

        huntVariableService.setHuntingVariable(seller, order);
    }

}
