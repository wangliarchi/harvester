package edu.olivet.harvester.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 记录每次做单的数据，用于后续统计
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 11:52 AM
 */
public class StatisticLogger {
    private static final Logger logger = LoggerFactory.getLogger(StatisticLogger.class);

    public static void log(String msg) {
        logger.info(msg);
    }



}