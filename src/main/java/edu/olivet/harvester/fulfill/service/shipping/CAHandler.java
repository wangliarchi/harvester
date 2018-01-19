package edu.olivet.harvester.fulfill.service.shipping;


import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.common.model.Order;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * 加拿大运输方式选择处理:
 * CA目前都只有普递，加拿大直寄或者国际的 都选择普递，如果有free shipping选择free shipping
 * 运费上限国内单10加元，国际单20加元
 *
 * CA产品转运 - ca直寄
 * a.不是寄到本地的，都选择快递 ，(运费上限25加元或者美元 )
 *
 * </pre>
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 21, 2014 5:32:32 PM
 */
@Singleton
public class CAHandler extends DefaultHandler implements ShippingHandler {

    private static final CAHandler instance = new CAHandler();

    private CAHandler() {
    }


    public static CAHandler getInstance() {
        return instance;
    }


    @Override
    public ShippingOption determineShipOption(Order order, List<ShippingOption> shippingOptions) {
        List<ShippingOption> validOptions = getValidateOptions(order, shippingOptions);

        //国际直寄都选择快递
        if (order.isIntl()) {
            validOptions.removeIf(it -> !it.isExpedited());
        }

        List<ShippingOption> freeShippingOptions = validOptions.stream().filter(ShippingOption::isFree).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(freeShippingOptions)) {
            freeShippingOptions.sort(Comparator.comparing(ShippingOption::getLatestDeliveryDate));
            return freeShippingOptions.get(0);
        }

        ShippingSpeed shippingSpeed = determineFinalSpeed(order);

        for (ShippingOption option : validOptions) {

            if (Strings.containsAnyIgnoreCase(option.getFullText(), shippingSpeed.getKeywords().split(","))) {
                return option;
            }

        }

        throw new OrderSubmissionException("No valid shipping option found.");
    }

    @Override
    public ShippingSpeed determineFinalSpeed(Order order) {
        //国际直寄都选择快递
        if (order.isIntl()) {
            return ShippingSpeed.Expedited;
        }

        if (order.expeditedShipping()) {
            return ShippingSpeed.Expedited;
        }

        return ShippingSpeed.Standard;
    }




}
