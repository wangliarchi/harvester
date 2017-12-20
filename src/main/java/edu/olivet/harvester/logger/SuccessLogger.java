package edu.olivet.harvester.logger;

import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.model.Order;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单提交成功结果日志记录
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 11:46 AM
 */
public class SuccessLogger {
    private static final Logger logger = LoggerFactory.getLogger(SuccessLogger.class);

    /**
     * 提交成功的order信息备份，在数据核对时作为原始依据
     *
     * @param order 提交成功的订单
     */
    public static void log(Order order) {
        String message = order.successRecord() + Constants.TAB +
                StringUtils.defaultString(order.getContext()) + Constants.TAB +
                StringUtils.defaultString(order.getContextUrl());
        logger.info(message);
    }
}
