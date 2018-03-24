package edu.olivet.harvester.fulfill.utils;

import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 2:38 PM
 */
@Singleton
public class CountryStateUtils {
    private Map<String, String> countryCodes;
    private Map<String, String> countryNames;
    private Map<String, String> usStates;
    private Map<String, String> caStates;
    private List<String> euCountries;
    private static final CountryStateUtils instance = new CountryStateUtils();

    public static CountryStateUtils getInstance() {
        return instance;
    }

    private CountryStateUtils() {
        countryCodes = Configs.load(Config.CountryCode.fileName());
        countryNames = Configs.load(Config.CountryName.fileName());
        usStates = Configs.load(Config.USStates.fileName(), Configs.KeyCase.UpperCase);
        caStates = Configs.load(Config.CAProvinces.fileName(), Configs.KeyCase.UpperCase);
        euCountries = Configs.readLines(Config.EUCountry.fileName());
    }


    /**
     * 根据国家完整名称获取对应国家代码
     *
     * @param shippingCountry 国家完整名称，比如United States
     */
    public String getCountryCode(String shippingCountry) {
        if (StringUtils.length(shippingCountry) == 2) {
            return shippingCountry.toUpperCase();
        }
        if (StringUtils.isBlank(shippingCountry) || Country.US.name().equals(shippingCountry)) {
            return Country.US.name();
        }

        String countryCode = countryCodes.get(shippingCountry);
        if (StringUtils.isNotBlank(countryCode)) {
            return countryCode;
        }
        return shippingCountry;
    }


    public boolean isEUCountry(String country) {
        String code = getCountryCode(country);
        return euCountries.contains(code);
    }

    /**
     * 根据国家代码获取对应国家完整名称
     *
     * @param countryCode 国家代码，比如US
     */
    public String getCountryName(String countryCode) {
        if (StringUtils.isBlank(countryCode)) {
            return Country.US.name();
        }

        if (!countryNames.containsKey(countryCode.toUpperCase())) {
            return countryCode;
        }

        String countryName = countryNames.get(countryCode.toUpperCase());
        if (StringUtils.isBlank(countryName)) {
            return countryCode;
        }

        return countryName;
    }

    public String getUSStateAbbr(String stateName) {
        if (StringUtils.length(stateName) == 2) {
            return stateName.toUpperCase();
        }

        String cleaned = stateName.replaceAll(Regex.NON_ALPHA_LETTERS.val(), "");
        if (StringUtils.length(cleaned) == 2) {
            return cleaned.toUpperCase();
        }
        return usStates.get(stateName.toUpperCase()).toUpperCase();
    }


    public String getUSStateName(String stateAbbr) {

        String cleaned = stateAbbr.replaceAll(Regex.NON_ALPHA_LETTERS.val(), "");

        if (StringUtils.length(cleaned) > 2) {
            return stateAbbr;
        }

        return MapUtils.invertMap(usStates).get(cleaned.toUpperCase());
    }

    public String getCAStateAbbr(String stateName) {
        if (StringUtils.length(stateName) == 2) {
            return stateName.toUpperCase();
        }

        return caStates.get(stateName.toUpperCase()).toUpperCase();
    }

    public String getCAStateName(String stateAbbr) {
        if (StringUtils.length(stateAbbr) > 2) {
            return stateAbbr;
        }

        return MapUtils.invertMap(caStates).get(stateAbbr.toUpperCase());
    }

    public static void main(String[] args) {
        List<String> countries = Configs.readLines(Config.EUCountry.fileName());
        System.out.println(countries);
    }
}
