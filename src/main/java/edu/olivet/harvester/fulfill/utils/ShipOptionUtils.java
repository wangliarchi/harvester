package edu.olivet.harvester.fulfill.utils;

import com.mchange.lang.IntegerUtils;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.fulfill.utils.OrderValidator.SkipValidation;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/14/17 11:08 AM
 */
public class ShipOptionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipOptionUtils.class);

    public static void selectShipOption(BuyerPanel buyerPanel) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        List<DOMElement> options = JXBrowserHelper.selectElementsByCssSelector(browser, ".shipping-speed.ship-option");
        List<ShippingOption> shippingOptions = listAllOptions(browser, buyerPanel.getCountry());
        List<ShippingOption> validShippingOptions = getValidateOptions(buyerPanel.getOrder(), shippingOptions);

        for (DOMElement option : options) {
            String eddText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-success").getInnerText().trim();
            if (eddText.equals(validShippingOptions.get(0).getEstimatedDeliveryDate())) {
                option.click();
                break;
            }
        }
    }

    public static List<ShippingOption> listAllOptions(Browser browser, Country country) {
        List<DOMElement> options = JXBrowserHelper.selectElementsByCssSelector(browser, ".shipping-speed.ship-option");

        List<ShippingOption> shippingOptions = new ArrayList<>();
        for (DOMElement option : options) {
            String eddText;
            String priceText;
            try {
                eddText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-success").getInnerText().trim();
            } catch (Exception e) {
                LOGGER.error("Error fetch shipping option edd {}", option.getInnerHTML());
                continue;
            }
            try {
                priceText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-secondary").getInnerText().trim();

            } catch (Exception e) {
                LOGGER.error("Error fetch shipping option price {}", option.getInnerHTML());
                continue;
            }

            ShippingOption shippingOption = new ShippingOption(eddText, priceText, country);
            shippingOptions.add(shippingOption);
        }

        return shippingOptions;
    }

    public static List<ShippingOption> getValidateOptions(Order order, List<ShippingOption> shippingOptions) {

        Date orderEdd = order.latestEdd();
        int maxDays = IntegerUtils.parseInt(RuntimeSettings.load().getEddLimit(), 7);

        DateTime start = new DateTime(orderEdd.getTime());

        List<ShippingOption> validShippingOptions = shippingOptions.stream().filter(it -> {
            Date latestDate = it.getLatestDeliveryDate();
            DateTime end = new DateTime(latestDate.getTime());
            int daysExceedOrderEdd = Days.daysBetween(start, end).getDays();

            //Expedited Shipping requested
            if (Remark.fastShipping(order.remark) && !it.isExpedited()) {
                return false;
            }
            return OrderValidator.skipCheck(order, SkipValidation.EDD) || Remark.isDN(order.remark) || latestDate.before(orderEdd) || daysExceedOrderEdd <= maxDays;

        }).collect(Collectors.toList());


        if (CollectionUtils.isEmpty(validShippingOptions)) {
            Date latestDate = shippingOptions.get(0).getLatestDeliveryDate();
            int days = Math.abs(Dates.daysBetween(latestDate, orderEdd));
            throw new BusinessException("No shipping option available. Earliest EDD is " + Dates.format(latestDate, DateFormat.US_FEEDBACK_DATE.pattern()) + ", order EDD is " + Dates.format(order.latestEdd(), DateFormat.US_FEEDBACK_DATE.pattern()) + ", exceed order EDD " + days + " days");
        }

        validShippingOptions.sort(Comparator.comparing(ShippingOption::getPriceAmount));
        return validShippingOptions;
    }
}
