package edu.olivet.harvester.fulfill.model;

import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.model.Money;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Lists;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 1:52 PM
 */
@Data
public class ShippingOption {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippingOption.class);

    String estimatedDeliveryDate;
    Date latestDeliveryDate;
    String title;
    Money price;


    public ShippingOption(String eddText, String priceText, Country country) {
        estimatedDeliveryDate = eddText;
        title = priceText;
        latestDeliveryDate = parseEDD(eddText, country);
        price = parsePrice(priceText, country);
    }

    public BigDecimal getPriceAmount() {
        return price.getAmount();
    }

    //FREE Two-Day Shipping
    //$3.99 - Standard Shipping
    public Money parsePrice(String priceText, Country country) {
        try {
            return Money.fromText(priceText, country);
        } catch (ParseException e) {
            LOGGER.error("Cant parse price text {}", priceText, e);
        }

        return new Money(0f, country);
    }

    //Friday, Nov. 17 - Monday, Nov. 27
    //Monday, Nov. 13
    public Date parseEDD(String eddText, Country country) {
        List<String> formatPatterns = Lists.newArrayList("EEEE, MMM. dd", "MMM. dd");

        String[] parts = StringUtils.split(eddText, "-");
        String dateString = parts[parts.length - 1].trim();

        for (String pattern : formatPatterns) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, country.locale());
            try {
                Date date = dateFormat.parse(dateString);
                Date now = new Date();
                int years = Dates.getYear(now) - 1970;
                if (Dates.getField(date, Calendar.MONTH) < Dates.getField(now, Calendar.MONTH)) {
                    years += 1;
                }

                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.YEAR, years);
                date = c.getTime();
                return date;
            } catch (ParseException e) {
                //ingore
                //throw new BusinessException(e);
            }
        }

        if (StringUtils.containsAny(dateString.toLowerCase(), "days")) {
            try {
                String daysString = dateString.replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
                int days = IntegerUtils.parseInt(daysString, 1);

                Date date = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                for (int i = 0; i <= days; ) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    //here even sat and sun are added
                    //but at the end it goes to the correct week day.
                    //because i is only increased if it is week day
                    if (calendar.get(Calendar.DAY_OF_WEEK) <= 5) {
                        i++;
                    }

                }

                date = calendar.getTime();
                return date;
            } catch (Exception e) {
                //
            }
        }

        throw new BusinessException("Cant parse shipping option edd " + eddText);
    }

    public static void main(String[] args) {
        ShippingOption shippingOption = new ShippingOption("Friday, Nov. 17 - Monday, Jan. 27", "$3.99 - Standard Shipping", Country.US);
        System.out.println(shippingOption.parseEDD("averages 9-12 business days ", Country.US));
        System.out.println(shippingOption.parseEDD("Friday, Nov. 17 - Monday, Jan. 27", Country.US));
        System.out.println(shippingOption.parseEDD("Monday, Nov. 13", Country.US));
        System.out.println(shippingOption.parsePrice("FREE Two-Day Shipping", Country.US));
        System.out.println(shippingOption.parsePrice("$3.99 - Standard Shipping", Country.US));
        //
    }

}

