package edu.olivet.harvester.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单提交成功结果日志记录
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 11:46 AM
 */
public class SellerHuntingLogger {
    private static final Logger logger = LoggerFactory.getLogger(SellerHuntingLogger.class);

    public static void info(String format, Object... arguments) {
        logger.info(format,arguments);
    }

    public static void error(String format, Object... arguments) {
        logger.error(format,arguments);
    }
}
