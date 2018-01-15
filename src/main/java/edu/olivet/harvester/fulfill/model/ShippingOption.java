package edu.olivet.harvester.fulfill.model;

import com.google.common.collect.Lists;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.model.Money;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
        if (Strings.containsAnyIgnoreCase(fullText, "free ", "GRATUIT", "frei ", "gratis ", "gratuito ")) {
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
        List<String> formatPatterns = Lists.newArrayList("MMM dd", "dd MMM");

        String[] eddParts = StringUtils.split(eddText, ",");
        ArrayUtils.reverse(eddParts);
        for (String part : eddParts) {
            try {
                String[] parts = StringUtils.split(part, "-");

                String dateString = parts[parts.length - 1].trim();

                if (dateString.length() <= 2) {
                    dateString = parts[parts.length - 2].trim();
                    String[] dateStringParts = dateString.split(" ");
                    dateString = StringUtils.join(Arrays.copyOf(dateStringParts, dateStringParts.length - 1), " ") + " " + parts[parts.length - 1].trim();
                }

                dateString = dateString.replaceAll("[^\\p{L}\\p{Nd} ]+", "").trim();
                dateString = dateString.replace(" de ", " ");

                String[] dateStringParts = dateString.split(" ");
                List<String> list = Lists.newArrayList(dateStringParts);
                list.removeIf(StringUtils::isBlank);

                dateString = list.get(list.size() - 2) + " " + list.get(list.size() - 1);
                index = 3;
                while (!RegexUtils.match(dateString, ".*\\d+.*") && list.size() >= index) {
                    dateString = list.get(list.size() - index) + " " + dateString;
                    index++;
                }


                for (String pattern : formatPatterns) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, country.locale());
                    try {
                        Date date = dateFormat.parse(dateString);
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
                        //LOGGER.error("", e);
                        //ignore
                        //throw new BusinessException(e);
                    }
                }

                if (Strings.containsAnyIgnoreCase(part.toLowerCase(), "days", "Werktage", "lavorativi", "ouvrés", "días", "jours", "Tage", "dias", "giorni")) {
                    try {
                        String[] dayParts = StringUtils.split(part, "-");
                        String daysString = dayParts[dayParts.length - 1].trim();

                        daysString = daysString.replaceAll(RegexUtils.Regex.NON_DIGITS.val(), "");
                        int days = IntegerUtils.parseInt(daysString, 1);

                        return afterWorkDays(days);

                    } catch (Exception e) {
                        //
                    }
                }
            }catch (Exception e) {
                //
            }
        }

        if (country != Country.US) {
            return parseEDD(eddText, Country.US);
        }

        if (shippingSpeed == ShippingSpeed.Standard) {
            return afterWorkDays(10);
        } else if (shippingSpeed == ShippingSpeed.Expedited) {
            return afterWorkDays(5);
        }
        throw new BusinessException("Cant parse shipping option edd " + eddText);
    }

    public Date afterWorkDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        for (int i = 0; i <= days; ) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            //here even sat and sun are added
            //but at the end it goes to the correct week day.
            //because i is only increased if it is week day
            if (calendar.get(Calendar.DAY_OF_WEEK) <= 5) {
                i++;
            }

        }

        return calendar.getTime();
    }

}

