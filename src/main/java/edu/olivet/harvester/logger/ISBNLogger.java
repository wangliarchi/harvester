package edu.olivet.harvester.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 10:35 AM
 */
public class ISBNLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ISBNLogger.class);

    public static void save(String entry) {
        LOGGER.info(entry);
    }
}
