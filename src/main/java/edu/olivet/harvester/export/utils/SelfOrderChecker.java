package edu.olivet.harvester.export.utils;

import com.amazonservices.mws.orders._2013_09_01.model.OrderItem;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/19/17 8:22 PM
 */
public class SelfOrderChecker {


    public static boolean isSelfOrder(AmazonOrder order) {
        OrderItem orderItem = MWSUtils.buildMwsObject(order.getItemXml(), OrderItem.class);
        String promotion = orderItem.getPromotionDiscount().getAmount();
        return StringUtils.isNotBlank(promotion) && FloatUtils.parseFloat(promotion, 0) != 0;
    }

    public static void markAsSelfOrder(Order order) {
        order.quantity_purchased = StringUtils.EMPTY;
        order.remark = Remark.SELF_ORDER.appendTo(order.remark);
    }
}
