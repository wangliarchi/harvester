package edu.olivet.harvester.fulfill.service.shipping;


import com.google.inject.Singleton;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.exception.Exceptions.ShippingFeeTooHighException;
import edu.olivet.harvester.fulfill.exception.OrderSubmissionException;
import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.model.Order;
import org.apache.commons.collections4.CollectionUtils;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * 加拿大运输方式选择处理:
 * CA目前都只有普递，加拿大直寄或者国际的 都选择普递，如果有free shiping选择free shiping
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
    private static final float STANDARD_SHIPPING_FEE_MAX = 10.0f;
    private static final float INTL_SHIPPING_FEE_MAX = 20.0f;
    private static final float SWITCH_SHIPPING_FEE_MAX = 25.0f;

    public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");
    public static final float CA_FREE_SHIPPING_PRICE = 25.0f;   // 2818.0f; // USD 25.0f;

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

        List<ShippingOption> freeShippings = validOptions.stream().filter(it -> it.isFree()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(freeShippings)) {
            freeShippings.sort(Comparator.comparing(ShippingOption::getLatestDeliveryDate));
            return freeShippings.get(0);
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

        return ShippingSpeed.Standard;
    }


    @Override
    public void checkFee(float shippingFee, Order order) throws ShippingFeeTooHighException {
        String fee = DOUBLE_FORMAT.format(shippingFee);
        if (order.switchCountry() && shippingFee > SWITCH_SHIPPING_FEE_MAX) {
            throw new ShippingFeeTooHighException(UIText.message("error.shippingfee.high", fee, SWITCH_SHIPPING_FEE_MAX));
        }

        if (!order.switchCountry()) {
            if (order.isIntl() && shippingFee > INTL_SHIPPING_FEE_MAX) {
                throw new ShippingFeeTooHighException(UIText.message("error.shippingfee.high", fee, INTL_SHIPPING_FEE_MAX));
            }

            if (!order.isIntl() && shippingFee > STANDARD_SHIPPING_FEE_MAX) {
                throw new ShippingFeeTooHighException(UIText.message("error.shippingfee.high", fee, STANDARD_SHIPPING_FEE_MAX));
            }
        }
    }

}
