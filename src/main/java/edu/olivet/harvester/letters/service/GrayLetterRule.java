package edu.olivet.harvester.letters.service;

import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.letters.model.GrayEnums.GrayLetterType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:13 AM
 */
public class GrayLetterRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrayLetterRule.class);

    public enum GrayRule {
        SendLetter,
        FindSupplier,
        None
    }

    private static final String[] BLACKLIST_REMARKS = {
            "oversize", "hazardous", "prohibited", "fragile", "electric", "immoral", "blacklist",
            "大", "超大超重", "不健康", "易碎", "危险品", "黑名单", "电器", "big", "heavy"
    };

    //a.对于7天内的灰条(status为sg，remark含有sg/high price/price/价格)重新找单,找单成功后变成可做单（白条），对于第7-10天的灰条发灰条信
    //b.检查两天内的status（sg）和remark，对于remark含有 检查已经confirm后 随即发灰条信
    //c.检查两天内的status和remark，对于status和remark标有ng，wc，hp，ph，lw，检查已经confirm后随即发灰条信（wc除外）
    //d.对于us的快递单灰条（运费为6.99），重新找单失败后，检查已经confirm后随即发灰条信
    public static GrayRule getGrayRule(Order order, boolean huntSupplier) {

        if (order.buyerCanceled()) {
            return GrayRule.None;
        }

        GrayLetterType type = GrayLetterType.getTypeFromStatus(order.status);
        if (type == null) {
            return GrayRule.None;
        }

        if (huntSupplier && needFindSupplier(order)) {
            return GrayRule.FindSupplier;
        }

        if (needSendLetter(order, huntSupplier)) {
            return GrayRule.SendLetter;
        }

        return GrayRule.None;
    }


    public static boolean needFindSupplier(Order order) {
        GrayLetterType type = GrayLetterType.getTypeFromStatus(order.status);
        if (type == null) {
            return false;
        }

        if (type.handleImmediately()) {
            return false;
        }

        //检查两天内的status（sg）和remark，对于remark含有 检查已经confirm后 随即发灰条信
        if (Strings.containsAnyIgnoreCase(order.remark, BLACKLIST_REMARKS)) {
            return false;
        }

        //对于第7-10天的灰条发灰条信
        Date orderDate = getOrderDate(order);
        if (orderDate == null) {
            return false;
        }

        int days = Dates.daysBetween(orderDate, new Date());

        return days < 7;
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean needSendLetter(Order order, boolean waitToFindSupplier) {

        GrayLetterType type = GrayLetterType.getTypeFromStatus(order.status);
        if (type == null) {
            return false;
        }

        //检查两天内的status（sg）和remark，对于remark含有 检查已经confirm后 随即发灰条信
        if (Strings.containsAnyIgnoreCase(order.remark, BLACKLIST_REMARKS)) {
            LOGGER.info("order {} -  {} to send letter as remark contains blacklist {}", order.sheetName, order.order_id, order.remark);
            return true;
        }

        //c.检查两天内的status和remark，对于status和remark标有ng，wc，hp，ph，lw，检查已经confirm后随即发灰条信（wc除外）
        if (type.handleImmediately()) {
            LOGGER.info("order {} -  {} to send letter as its status is {}", order.sheetName, order.order_id, order.status);
            return true;
        }

        if (!waitToFindSupplier) {
            LOGGER.info("order {} -  {} to send letter as it's triggered manually, status is {}", order.sheetName, order.order_id, order.status);
            return true;
        }

        //对于第7-10天的灰条发灰条信
        Date orderDate = getOrderDate(order);
        if (orderDate == null) {
            LOGGER.info("order {} -  {} to send letter as its purchase date {} is unknown", order.sheetName, order.order_id, order.purchase_date);
            return true;
        }

        int days = Dates.daysBetween(orderDate, new Date());
        if (days >= 7 && days <= 10) {
            LOGGER.info("order {} -  {} to send letter as its purchase date {} is {} days to today", order.sheetName, order.order_id, order.purchase_date, days);
            return true;
        }

        //d.对于us的快递单灰条（运费为6.99），重新找单失败后，检查已经confirm后随即发灰条信
        if (order.buyerExpeditedShipping()) {
            LOGGER.info("order {} -  {} to send letter as it's expedited [{}] but no seller found", order.sheetName, order.order_id, order.shipping_service);
            return true;
        }

        return false;
    }

    private static Date getOrderDate(Order order) {
        Date orderDate = null;
        try {
            orderDate = order.getPurchaseDate();
        } catch (Exception e) {
            try {
                orderDate = Dates.parseDateOfGoogleSheet(order.sheetName);
            } catch (Exception e2) {
                //
                LOGGER.error("Fail to get order purchase date", order);
            }
        }

        return orderDate;
    }

}
