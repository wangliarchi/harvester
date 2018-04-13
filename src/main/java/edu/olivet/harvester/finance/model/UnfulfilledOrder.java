package edu.olivet.harvester.finance.model;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.utils.common.DateFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/12/2018 11:42 AM
 */
@Data
@NoArgsConstructor
public class UnfulfilledOrder {
    String orderDate;
    String orderNumber;
    String sku;
    String orderDescription;
    String price;
    String shipping;
    String qty;
    String totalPrice;
    String estimatedCost;
    String fulfilledAmount;
    String fulfilledDate;
    String refundAmount;
    String refundDate;
    String salesChannel;
    String remark;

    public static UnfulfilledOrder init(Order order, List<Refund> refunds) {
        UnfulfilledOrder unfulfilledOrder = new UnfulfilledOrder();
        unfulfilledOrder.orderDate = DateFormat.FULL_DATE.format(order.getPurchaseDate());
        unfulfilledOrder.orderNumber = order.order_id;
        unfulfilledOrder.orderDescription = order.item_name;
        if (StringUtils.isBlank(order.fulfilledDate)) {
            unfulfilledOrder.estimatedCost = StringUtils.EMPTY;
            unfulfilledOrder.fulfilledAmount = StringUtils.EMPTY;
            unfulfilledOrder.fulfilledDate = StringUtils.EMPTY;
        } else {
            unfulfilledOrder.estimatedCost = StringUtils.isNotBlank(order.cost) ? order.cost : order.reference;
            unfulfilledOrder.fulfilledAmount = order.cost;
            unfulfilledOrder.fulfilledDate = order.fulfilledDate;
        }
        unfulfilledOrder.salesChannel = order.sales_chanel;
        unfulfilledOrder.sku = order.sku;
        unfulfilledOrder.price = order.price;
        unfulfilledOrder.shipping = order.shipping_fee;
        unfulfilledOrder.qty = order.quantity_purchased;
        unfulfilledOrder.remark = order.remark;
        //totalPrice
        try {
            float totalPrice = (Float.parseFloat(order.price) + Float.parseFloat(order.shipping_fee)) * Float.parseFloat(order.quantity_purchased);
            Country country = OrderCountryUtils.getMarketplaceCountry(order);
            unfulfilledOrder.totalPrice = new Money(totalPrice, country).toString();
        } catch (Exception e) {
            unfulfilledOrder.totalPrice = StringUtils.EMPTY;
            //
        }
        if (CollectionUtils.isNotEmpty(refunds)) {
            float total = 0;
            for (Refund refund : refunds) {
                try {
                    total += refund.getAmount().getAmount().floatValue() * Float.parseFloat(refund.getQuantity());
                } catch (Exception e) {
                    //
                }
            }

            Money totalRefund = new Money(total, refunds.get(0).getAmount().getCurrency());
            unfulfilledOrder.refundAmount = totalRefund.toString();
            unfulfilledOrder.refundDate = DateFormat.FULL_DATE.format(refunds.get(0).getDate());
        }

        return unfulfilledOrder;
    }
}
