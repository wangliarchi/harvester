package edu.olivet.harvester.letters.service;

import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:13 AM
 */
public class GrayLetterRule {


    public enum GrayRule {
        SendLetter,
        FindSupplier,
        None
    }

    private static final String[] BLACKLIST_REMARKS = {
            "oversize", "hazardous", "prohibited", "fragile", "electric", "immoral", "blacklist",
            "大", "超大超重", "不健康", "易碎", "危险品", "黑名单", "电器", "big", "heavy"
    };

    private static final String[] GRAY_LETTER_REMARKS = {"ng", "hp", "ph", "wc", "lw", "dn"};
    private static final String[] COMMON_GRAY_LETTER_REMARKS = {"sg"};

    //a.对于7天内的灰条(status为sg，remark含有sg/high price/price/价格)重新找单,找单成功后变成可做单（白条），对于第7-10天的灰条发灰条信
    //b.检查两天内的status（sg）和remark，对于remark含有 检查已经confirm后 随即发灰条信
    //c.检查两天内的status和remark，对于status和remark标有ng，wc，hp，ph，lw，检查已经confirm后随即发灰条信（wc除外）
    //d.对于us的快递单灰条（运费为6.99），重新找单失败后，检查已经confirm后随即发灰条信

    public static GrayRule getGrayRule(Order order) {

        if (!StringUtils.equalsAnyIgnoreCase(order.status, ArrayUtils.addAll(GRAY_LETTER_REMARKS, COMMON_GRAY_LETTER_REMARKS))) {
            return GrayRule.None;
        }

        if (needFindSupplier(order)) {
            return GrayRule.FindSupplier;
        }

        if (needSendLetter(order)) {
            return GrayRule.SendLetter;
        }

        return GrayRule.None;
    }


    public static boolean needFindSupplier(Order order) {
        if (!StringUtils.equalsAnyIgnoreCase(order.status, COMMON_GRAY_LETTER_REMARKS)) {
            return false;
        }

        //对于第7-10天的灰条发灰条信
        Date orderDate;
        try {
            orderDate = order.getPurchaseDate();
        } catch (Exception e) {
            orderDate = Dates.parseDateOfGoogleSheet(order.sheetName);
        }

        int days = Dates.daysBetween(orderDate, new Date());

        if (days >= 7) {
            return false;
        }

        return true;
    }

    public static boolean needSendLetter(Order order) {

        if (!StringUtils.equalsAnyIgnoreCase(order.status, ArrayUtils.addAll(GRAY_LETTER_REMARKS, COMMON_GRAY_LETTER_REMARKS))) {
            return false;
        }

        //检查两天内的status（sg）和remark，对于remark含有 检查已经confirm后 随即发灰条信
        if (Strings.containsAnyIgnoreCase(order.remark, BLACKLIST_REMARKS)) {
            return true;
        }

        //c.检查两天内的status和remark，对于status和remark标有ng，wc，hp，ph，lw，检查已经confirm后随即发灰条信（wc除外）
        if (StringUtils.equalsAnyIgnoreCase(order.status, GRAY_LETTER_REMARKS)) {
            return true;
        }

        //对于第7-10天的灰条发灰条信
        Date orderDate;
        try {
            orderDate = order.getPurchaseDate();
        } catch (Exception e) {
            orderDate = Dates.parseDateOfGoogleSheet(order.sheetName);
        }

        int days = Dates.daysBetween(orderDate, new Date());
        if (days >= 7 && days <= 10) {
            return true;
        }

        //d.对于us的快递单灰条（运费为6.99），重新找单失败后，检查已经confirm后随即发灰条信
        if (order.buyerExpeditedShipping()) {
            return true;
        }

        return false;
    }


}
