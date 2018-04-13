package edu.olivet.harvester.fulfill.model;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.utils.common.DatetimeHelper;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 1:52 PM
 */
@Data
public class ShippingOption {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingOption.class);

    private String estimatedDeliveryDate;
    private Date latestDeliveryDate;
    private String title;
    private Money price;
    private Date now;
    private ShippingSpeed shippingSpeed;
    private String fullText;
    private int index = 0;


    public ShippingOption(String fullText, String eddText, String priceText, Country country, Date today) {
        now = today;
        this.fullText = fullText;
        estimatedDeliveryDate = eddText;
        title = priceText;

        shippingSpeed = ShippingSpeed.get(fullText);

        try {
            latestDeliveryDate = parseEDD(eddText, country);
        } catch (Exception e) {
            latestDeliveryDate = parseEDD(fullText, country);
        }
        price = parsePrice(priceText, country);


    }

    public ShippingOption(String fullText, String eddText, String priceText, Country country) {
        this(fullText, eddText, priceText, country, new Date());
    }

    public ShippingOption(String eddText, String priceText, Country country) {
        this(eddText + " " + priceText, eddText, priceText, country, new Date());
    }

    public ShippingOption(String eddText, String priceText, Country country, Date today) {
        this(eddText + " " + priceText, eddText, priceText, country, today);
    }

    public boolean isFree() {
        return price != null && price.getAmount().floatValue() == 0;
    }

    public boolean isExpedited() {
        if (shippingSpeed == ShippingSpeed.Expedited) {
            return true;
        }

        if (now == null) {
            now = new Date();
        }
        int days = Dates.daysBetween(now, latestDeliveryDate);
        return days <= 10;
    }

    public BigDecimal getPriceAmount() {
        return price.getAmount();
    }

    //FREE Two-Day Shipping
    //$3.99 - Standard Shipping
    public Money parsePrice(String priceText, Country country) {
        if (Strings.containsAnyIgnoreCase(fullText + " " + priceText, "free", "GRATUIT", "frei", "gratis", "gratuito")) {
            return new Money(0f, country);
        }

        if (StringUtils.isBlank(priceText)) {
            return null;
        }

        try {
            return Money.fromText(priceText, country);
        } catch (Exception e) {
            LOGGER.error("Cant parse price text {}", priceText, e);
        }

        return new Money(0f, country);
    }

    //Friday, Nov. 17 - Monday, Nov. 27
    //Monday, Nov. 13, Standard-Shipping --get it Dec 4 - 5
    public Date parseEDD(String eddText, Country country) {

        try {
            return DatetimeHelper.parseEdd(eddText, country, now);
        } catch (Exception e) {
            LOGGER.error("Cant parse shipping option edd {}", eddText);
        }

        if (shippingSpeed == ShippingSpeed.Standard) {
            return DatetimeHelper.afterWorkDays(10, now);
        } else if (shippingSpeed == ShippingSpeed.Expedited) {
            return DatetimeHelper.afterWorkDays(5, now);
        }
        throw new BusinessException("Cant parse shipping option edd " + eddText);
    }


}

