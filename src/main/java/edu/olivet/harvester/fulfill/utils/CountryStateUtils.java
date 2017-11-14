package edu.olivet.harvester.fulfill.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 2:38 PM
 */
@Singleton
public class CountryStateUtils {
    private Map<String, String> countryCodes;
    private Map<String, String> usStates;

    @Inject
    public void init() {
        countryCodes = Configs.load(Config.CountryCode.fileName());
        usStates = Configs.load(Config.USStates.fileName(), Configs.KeyCase.UpperCase);
    }


    /**
     * 根据国家完整名称获取对应国家代码
     *
     * @param shippingCountry 国家完整名称，比如United States
     */
    public String getCountryCode(String shippingCountry) {
        if (StringUtils.isBlank(shippingCountry) || Country.US.name().equals(shippingCountry)) {
            return Country.US.name();
        }

        String countryCode = countryCodes.get(shippingCountry);
        if (StringUtils.isBlank(countryCode)) {
            if (!countryCodes.containsValue(shippingCountry.toUpperCase())) {
                throw new IllegalArgumentException(UIText.message("error.shipcountry.invalid", shippingCountry));
            } else {
                countryCode = shippingCountry;
            }
        }

        return countryCode;
    }

    public String getUSStateAbbr(String stateName) {
        if (StringUtils.length(stateName) == 2) {
            return stateName.toUpperCase();
        }

        return usStates.get(stateName.toUpperCase()).toUpperCase();
    }
}
