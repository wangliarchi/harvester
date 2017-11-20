package edu.olivet.harvester.fulfill.utils;

import com.mchange.lang.IntegerUtils;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.fulfill.utils.OrderValidator.SkipValidation;
import edu.olivet.harvester.fulfill.utils.OrderValidator.Validator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/14/17 11:08 AM
 */
public class ShipOptionUtils {

    public static List<ShippingOption> getValidateOptions(Order order, List<ShippingOption> shippingOptions) {

        Date orderEdd = order.latestEdd();
        int maxDays = IntegerUtils.parseInt(RuntimeSettings.load().getEddLimit(), 7);
        List<ShippingOption> validShippingOptions = shippingOptions.stream().filter(it -> {
            if(OrderValidator.skipCheck(SkipValidation.EDD) || Remark.isDN(order.remark)) {
                return true;
            }
            Date latestDate = it.getLatestDeliveryDate();
            int days = Dates.daysBetween(latestDate, orderEdd);
            return latestDate.before(orderEdd) || Math.abs(days) <= maxDays;
        }).collect(Collectors.toList());


        if (CollectionUtils.isEmpty(validShippingOptions)) {
            Date latestDate = shippingOptions.get(0).getLatestDeliveryDate();
            int days = Math.abs(Dates.daysBetween(latestDate, orderEdd));
            throw new BusinessException("No shipping option available. Earlies EDD is " + latestDate + ", exceed order EDD " + days + " days");
        }

        validShippingOptions.sort(Comparator.comparing(ShippingOption::getPriceAmount));
        return validShippingOptions;
    }
}
