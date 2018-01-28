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
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.spreadsheet.service.AppScript;
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

    public enum Type {
        Seller,
        Rating,
        Shipping
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
        String key = seller.getOfferListingCountry().name() + " " + seller.getType().abbrev() + " " +
                (seller.canDirectShip(orderCountry) ? "Direct" : "Export");

        JSONObject sellerVariablesByPrices = sellerVariables.getJSONObject(key);
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
            if (orderPrice - seller.getTotalPriceInUSD() > Float.parseFloat(price)) {
                try {
                    sellerVariable = sellerVariablesByPrice.getFloat(price);
                    break;
                } catch (Exception e) {
                    //LOGGER.info("{} {}",sellerVariablesByPrice,price);
                }
            }
        }

        seller.setSellerVariable(sellerVariable);
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
