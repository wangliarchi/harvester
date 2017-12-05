package edu.olivet.harvester.utils;

import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.CurrencyRateCalculator;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/1/17 10:18 AM
 */
@Singleton
public class CurrencyConvertor implements CurrencyRateCalculator {
    private static CurrencyConvertor instance = new CurrencyConvertor();
    private CurrencyRateCalculator currencyRateCalculator;

    public static CurrencyConvertor getInstance() {
        return instance;
    }

    private CurrencyConvertor() {
        currencyRateCalculator = ApplicationContext.getBean(CurrencyRateCalculator.class);
    }

    @Override
    public float get(String from, String to) {
        return currencyRateCalculator.get(from, to);
    }

    @Override
    public double convertAmount(Country fromCountry, Country toCountry, double amount) {
        return currencyRateCalculator.convertAmount(fromCountry, toCountry, amount);
    }

    @Override
    public double convertAmountInUSD(Country fromCountry, double amount) {
        return currencyRateCalculator.convertAmountInUSD(fromCountry, amount);
    }
}
