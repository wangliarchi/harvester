package edu.olivet.harvester.fulfill.service.shipping;


import com.google.inject.Singleton;
import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.exception.Exceptions.OrderSubmissionException;
import edu.olivet.harvester.fulfill.exception.Exceptions.SellerEddTooLongException;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.Remark;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * </pre>
 */
@Singleton
public class DefaultHandler implements ShippingHandler {

    private static final DefaultHandler instance = new DefaultHandler();

    DefaultHandler() {
    }

    public static DefaultHandler getInstance() {
        return instance;
    }

    @Override
    public ShippingOption determineShipOption(Order order, List<ShippingOption> shippingOptions) {
        List<ShippingOption> validOptions = getValidateOptions(order, shippingOptions);
        //remove any one without price
        validOptions.removeIf(it -> it.getPrice() == null);

        if (CollectionUtils.isEmpty(validOptions)) {
            throw new OrderSubmissionException("No shipping option available.");
        }
        validOptions.sort(Comparator.comparing(ShippingOption::getPriceAmount));

        return validOptions.get(0);
    }

    @Override
    public ShippingSpeed determineFinalSpeed(Order order) {
        if (order.selfOrder) {
            return ShippingSpeed.Standard;
        }
        return null;
    }


    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public List<ShippingOption> getValidateOptions(Order order, List<ShippingOption> shippingOptions) {

        if (CollectionUtils.isEmpty(shippingOptions)) {
            throw new SellerEddTooLongException("No shipping option available.");
        }

        Date orderEdd = order.latestEdd();
        int maxDays = order.buyerExpeditedShipping() ? 3 : IntegerUtils.parseInt(order.getRuntimeSettings().getEddLimit(), 7);

        DateTime start = new DateTime(orderEdd.getTime());

        List<ShippingOption> validShippingOptions = shippingOptions.stream().filter(it -> {
            if (Strings.containsAnyIgnoreCase(it.getFullText().toLowerCase(), "trial", "prueba", "Kostenlose Testphase", "l'essai")) {
                return false;
            }
            //Expedited Shipping requested
            if (order.expeditedShipping() && !it.isExpedited()) {
                return false;
            }

            if (order.stdShipping() && it.isExpedited()) {
                return false;
            }

            Date latestDate = it.getLatestDeliveryDate();
            DateTime end = new DateTime(latestDate.getTime());
            int daysExceedOrderEdd = Days.daysBetween(start, end).getDays();


            return (OrderValidator.skipCheck(order, OrderValidator.SkipValidation.EDD) ||
                    Remark.isDN(order.remark) ||
                    latestDate.before(orderEdd) ||
                    daysExceedOrderEdd <= maxDays);
        }).collect(Collectors.toList());


        if (CollectionUtils.isEmpty(validShippingOptions)) {

            Date latestDate = shippingOptions.get(0).getLatestDeliveryDate();
            int days = Math.abs(Dates.daysBetween(latestDate, orderEdd));
            throw new SellerEddTooLongException("No shipping option available. Earliest EDD is " +
                    Dates.format(latestDate, DateFormat.US_FEEDBACK_DATE.pattern()) + ", order EDD is " +
                    Dates.format(order.latestEdd(), DateFormat.US_FEEDBACK_DATE.pattern()) +
                    ", exceed order EDD " + days + " days");
        }

        return validShippingOptions;
    }

}
