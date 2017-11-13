package edu.olivet.harvester.model;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.CurrencyRateCalculator;
import edu.olivet.foundations.utils.RegexUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 6:52 PM
 */
@Data
public class Money {
    private Currency currency;
    private BigDecimal amount;

    @Inject
    CurrencyRateCalculator currencyRateCalculator;

    public Money(BigDecimal amount, Country country) {
        this.amount = amount;
        this.currency = country.getCurrency();
    }

    public Money(float amount, Country country) {
        this.amount = new BigDecimal(Float.toString(amount));
        this.currency = country.getCurrency();
    }

    public BigDecimal toUSDAmount() {
        if ("USD".equals(currency.getCurrencyCode())) {
            return rounded(amount);
        }

        float exchangeRate = currencyRateCalculator.get(currency.getCurrencyCode(), "USD");
        return rounded(amount.multiply(new BigDecimal(Float.toString(exchangeRate))));
    }

    //
    public static Money fromText(String text, Country country) throws ParseException {
        String amt = RegexUtils.getMatched(text.replaceAll(" ", ""), RegexUtils.Regex.AMOUNT);

        if (StringUtils.isBlank(amt)) {
            return new Money(0, country);
        }

        //for FR, amazon using different locale format!!!
        NumberFormat format = NumberFormat.getNumberInstance(country.locale());
        if (country == Country.FR) {
            format = NumberFormat.getNumberInstance(Locale.GERMAN);
        }
        if (format instanceof DecimalFormat) {
            ((DecimalFormat) format).setParseBigDecimal(true);
        }
        BigDecimal amount = (BigDecimal) format.parse(amt);
        return new Money(amount, country);
    }


    public String toString() {
        return currency.getSymbol() + amount.toString();
    }

    /**
     * Defined centrally, to allow for easy changes to the rounding mode.
     */
    private static int ROUNDING_MODE = BigDecimal.ROUND_HALF_EVEN;

    /**
     * Number of decimals to retain. Also referred to as "scale".
     */
    private static int DECIMALS = 2;

    private BigDecimal rounded(BigDecimal aNumber) {
        return aNumber.setScale(DECIMALS, ROUNDING_MODE);
    }
}
