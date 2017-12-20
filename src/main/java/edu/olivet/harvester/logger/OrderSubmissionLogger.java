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
public class OrderSubmissionLogger {
    private static final Logger logger = LoggerFactory.getLogger(OrderSubmissionLogger.class);

    public static void info(String message) {
        logger.info(message);
    }

    public static void error(String message) {
        logger.error(message);
    }
}
