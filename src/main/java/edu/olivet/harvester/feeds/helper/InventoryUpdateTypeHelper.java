package edu.olivet.harvester.feeds.helper;

import edu.olivet.harvester.common.model.Order;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 1:09 PM
 */
public class InventoryUpdateTypeHelper {
    /**
     * 补点利润下限
     */
    private static final float PROFIT_GAIN_LIMIT = 1.0f;
    /**
     * 下架/删点利润下限
     */
    private static final float PROFIT_LOSS_LIMIT = -7.0f;

    /**
     * 库存更新类型
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Aug 16, 2015 4:25:05 PM
     */
    public enum UpdateType {
        /**
         * 补点
         */
        AddQuantity,
        /**
         * 下架
         */
        ClearQuantity,
        /**
         * 灰条删点
         */
        DeleteASIN,
        /**
         * 标注删点
         */
        DeleteASINSYNC;

        public boolean updateQty() {
            return this == AddQuantity || this == ClearQuantity;
        }

        public boolean deleteASIN() {
            return this == DeleteASIN || this == DeleteASINSYNC;
        }
    }


    /**
     * 根据Reference数值获取对应的库存更新类型
     */
    public static UpdateType getUpdateType(float reference) {
        if (reference >= PROFIT_GAIN_LIMIT) {
            return UpdateType.AddQuantity;
        } else if (reference > PROFIT_LOSS_LIMIT && reference < PROFIT_GAIN_LIMIT) {
            return UpdateType.ClearQuantity;
        } else if (reference <= PROFIT_LOSS_LIMIT) {
            return UpdateType.ClearQuantity;
        }
        return null;
    }

    public static UpdateType getUpdateType(Order order) {
        //黑名单删点
        if (order.asinMarkDelete()) {
            return UpdateType.DeleteASINSYNC;
        }

        if (StringUtils.isBlank(order.sku)) {
            return null;
        }

        //包含buyer 或者cancel，cancelled，canceled 补点
        if (order.buyerCanceled()) {
            return UpdateType.AddQuantity;
        }

        //灰条删点
        if (order.colorIsGray()) {
            return UpdateType.DeleteASIN;
        }

        //有的时候，表格里面reference公式计算需要好长时间，此时显示的内容是 loading  order.reference = "loading";
        //此时满足order.reference.equals("Loading...") is true，也满足下面的不是数值也不是double数的判断，返回null
        //也有reference是空值的时候, 空值的时候满足  order.reference.length() == 0 is true， 但是不能由下面的numeric判断返回null
        if (order.reference == null || order.reference.length() == 0 || order.reference.equals("Loading...")) {
            return null;
        }

        float reference;
        try {
            reference = Float.parseFloat(order.reference);
        } catch (Exception e) {
            return null;
        }
        if (order.isUKForward() || order.purchaseBack()) {
            reference = reference - 10;
        }

        UpdateType ut = getUpdateType(reference);

        //补点只补找完的单
        if (ut == UpdateType.AddQuantity && !order.sellerHunted()) {
            return null;
        }
        return ut;
    }
}
