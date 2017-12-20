package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.fulfill.model.setting.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.utils.validation.Predicates;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 1:46 PM
 */
public class OrderFilter {

    /**
     * 根据当前设置的程序运行参数和系统参数，对读取的订单进行筛选过滤
     *
     * @param orders   当前Sheet的所有可做订单
     * @param advs 当前程序运行设置参数
     * @return 筛选过后的订单，有可能为空，调用方需自行处理
     */
    public static List<Order> filterOrders(List<Order> orders, AdvancedSubmitSetting advs) {
        if (CollectionUtils.isEmpty(orders) || advs == null) {
            return orders;
        }

        List<Order> result = new ArrayList<>(orders);
        ConfigEnums.SubmitRange scopeType = advs.getSubmitRange();
        if (scopeType == ConfigEnums.SubmitRange.LimitCount && advs.getCountLimit() < orders.size()) {
            return result.subList(0, advs.getCountLimit());
        } else if (scopeType == ConfigEnums.SubmitRange.SINGLE) {
            CollectionUtils.filter(result, new Predicates.SingleRowPredicate(advs.getSingleRowNo()));
        } else if (scopeType == ConfigEnums.SubmitRange.SCOPE) {
            CollectionUtils.filter(result, new Predicates.ScopePredicate(advs.getStartRowNo(), advs.getEndRowNo()));
        } else if (scopeType == ConfigEnums.SubmitRange.MULTIPLE) {
            CollectionUtils.filter(result, new Predicates.MultiRowPredicate(advs.getMultiRows()));
        }

        OrderEnums.Status category = advs.getStatusFilterValue();
        if (category != null) {
            Predicates.CategoryPredicate predicate = new Predicates.CategoryPredicate(category);
            CollectionUtils.filter(result, predicate);
        }
        return result;
    }

}
