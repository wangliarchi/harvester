package edu.olivet.harvester.fulfill.service.shipping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingType;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/4/17 9:39 AM
 */
@Singleton
public class FeeLimitChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeeLimitChecker.class);

    private static final HashMap<Country, HashMap<ShippingType, HashMap<ShippingSpeed, Float>>> LIMIT_MAP = new HashMap<>();

    private static FeeLimitChecker instance = null;

    public static FeeLimitChecker getInstance() {
        if (instance == null) {
            instance = new FeeLimitChecker();
        }
        return instance;
    }

    private FeeLimitChecker() {
        String json = Configs.read(Config.ShippingFeeLimit.fileName());
        JSONObject object = JSON.parseObject(json);
        object.forEach((countryCode, c) -> {
            try {
                Country country = Country.fromCode(countryCode.toUpperCase());
                JSONObject configs = object.getJSONObject(countryCode);
                HashMap<ShippingType, HashMap<ShippingSpeed, Float>> countryMap = LIMIT_MAP.getOrDefault(country, new HashMap<>());


                for (ShippingType shippingType : ShippingType.values()) {
                    HashMap<ShippingSpeed, Float> typeMap = countryMap.getOrDefault(shippingType, new HashMap<>());
                    for (ShippingSpeed shippingSpeed : ShippingSpeed.values()) {
                        String key = shippingType.getCode() + shippingSpeed.getCode();
                        try {
                            String limit = configs.get(key.toLowerCase()).toString();
                            typeMap.put(shippingSpeed, Float.valueOf(limit));
                        } catch (Exception e) {
                            typeMap.put(shippingSpeed, 0f);
                        }
                    }

                    countryMap.put(shippingType, typeMap);
                }

                LIMIT_MAP.put(country, countryMap);

            } catch (Exception e) {
                LOGGER.error("", e);
            }

        });
    }

    public Float getLimit(Country fulfillmentCountry, ShippingType shippingType, ShippingSpeed shippingSpeed) {
        if (shippingSpeed == null) {
            shippingSpeed = ShippingSpeed.Standard;
        }
        try {
            return LIMIT_MAP.get(fulfillmentCountry).get(shippingType).get(shippingSpeed);
        } catch (Exception e) {
            LOGGER.error("No shipping limit found for {} {} {}", fulfillmentCountry, shippingType, shippingSpeed, e);
            return 0f;
        }
    }

    public boolean notExceed(Order order, float fee) {
        //skip
        if (OrderValidator.skipCheck(order, OrderValidator.SkipValidation.ShippingFee)) {
            return true;
        }

        ShippingSpeed shippingSpeed = order.shippingSpeed;
        ShippingType shippingType = order.isIntl() ? ShippingType.International : ShippingType.Domestic;
        Country fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(order);
        float feeLimit = getLimit(fulfillmentCountry, shippingType, shippingSpeed);

        return feeLimit >= fee;
    }

    public static void main(String[] args) {
        FeeLimitChecker feeLimitChecker = FeeLimitChecker.getInstance();
        float feeLimit = feeLimitChecker.getLimit(Country.CA, ShippingType.Domestic, ShippingSpeed.Standard);
        System.out.println(feeLimit);
    }
}
