package edu.olivet.harvester.logger.appender;

import ch.qos.logback.core.FileAppender;
import org.slf4j.event.LoggingEvent;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/27/2018 10:02 AM
 */
public class HunterFileAppender<E> extends FileAppender<E> {
    private static String path = null;


    protected void subAppend(LoggingEvent event) {
        this.closeOutputStream();
        setFileName();
        this.setFile(fileName);
        this.subAppend(event);
    }

    private void setFileName() {
        if (fileName != null) {
            //
        }
    }
}
