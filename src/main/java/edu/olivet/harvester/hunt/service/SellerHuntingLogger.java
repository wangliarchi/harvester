package edu.olivet.harvester.hunt.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.Configs.LogMode;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.ui.Harvester;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * 订单提交成功结果日志记录
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 11:46 AM
 */
public class SellerHuntingLogger {
    //private static final Logger logger = LoggerFactory.getLogger(SellerHuntingLogger.class);
    private static SellerHuntingLogger instance;
    private LoggerContext loggerContext;
    private FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender;
    @Setter
    public LogMode logMode = LogMode.Productive;

    @Getter
    private Logger logger;

    public static SellerHuntingLogger getLogger(Class<?> clazz) {
        return new SellerHuntingLogger(clazz);
    }

    public static SellerHuntingLogger getInstance(Class<?> clazz) {
        return getLogger(clazz);
    }

    private SellerHuntingLogger(Class<?> clazz) {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        fileAppender = new FileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("seller_hunting");
        // set the file name
        fileAppender.setFile(Directory.Log.path() + "log/" + System.currentTimeMillis() + ".log");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss}  %-5level - %msg%n");
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.start();

        fileAppender.setEncoder(encoder);


        // attach the rolling file appender to the logger of your choice
        logger = loggerContext.getLogger(clazz);
        logger.addAppender(fileAppender);

    }


    public void info(Order order, String format, Object... arguments) {
        if (logMode == LogMode.Development || Harvester.debugFlag) {
            return;
        }
        this.setOrder(order);
        this.getLogger().info(format, arguments);
    }

    public void error(Order order, String format, Object... arguments) {
        if (logMode == LogMode.Development || Harvester.debugFlag) {
            return;
        }
        this.setOrder(order);
        this.getLogger().error(format, arguments);
    }

    public void saveHtml(Document document) {
        String title = document.outerHtml();
        title = title.replaceAll(RegexUtils.Regex.NON_ALPHA_LETTER_DIGIT.val(), "");
        String filePath = Directory.WebPage.path() + "/sellers/" + DateFormat.DATE_TIME_AS_FILENAME + "-" + title + ".html";
        try {
            File file = new File(filePath);
            Tools.writeStringToFile(file, document.outerHtml());
        } catch (Exception e) {
            //LOGGER.error("尝试保存HTML文件到{}失败：", filePath, e);
        }
    }

    public void saveHtml(Order order, Document document) {
        saveHtml(order, document.title(), document.outerHtml());
    }

    public void saveHtml(Order order, String title, String html) {
        title = title.trim().replaceAll(RegexUtils.Regex.NON_ALPHA_LETTER_DIGIT.val(), "-");
        String filePath = getOrderLogDir(order) + "/html/" + DateFormat.DATE_TIME_AS_FILENAME.format(new Date()) + "-" + title + ".html";
        try {
            File file = new File(filePath);
            Tools.writeStringToFile(file, html);
        } catch (Exception e) {
            //System.out.println(e.getMessage());
        }
    }

    public SellerHuntingLogger setOrder(Order order) {
        setFile(getFilePath(order));
        return this;
    }

    public void setFile(String filePath) {
        try {
            fileAppender.stop();
            fileAppender.setFile(filePath);
            fileAppender.start();
        } catch (Exception e) {
            //
        }
    }


    public void setDefaultFile() {
        setFile(Directory.Log.path() + "/seller_hunting." + DateFormat.FULL_DATE.format(new Date()) + ".log");
    }

    public String getOrderLogDir(Order order) {
        Country country = OrderCountryUtils.getMarketplaceCountry(order);

        return String.format("%s/hunt/%s-%s/%s/%s",
                Directory.Log.path(), country.europe() ? "eu" : country.name().toLowerCase(),
                order.type().name().toLowerCase(),
                order.sheetName.replaceAll("/", "-"), order.row + "-" + order.order_id);
    }

    public String getFilePath(Order order) {
        try {
            return String.format("%s/%s-%s.log", getOrderLogDir(order), order.isbn, order.original_condition);
        } catch (Exception e) {
            return Directory.Log.path() + "/hunt/misc/" + order.order_id + ".log";
        }
    }


    public static void main(String[] args) {
        SellerHuntingLogger logger = SellerHuntingLogger.getInstance(SellerHuntingLogger.class);
        Order order = new Order();
        order.sheetName = "01/25";
        order.order_id = "114-7000378-4180241";
        order.isbn = "B00K1KZVJY";
        order.original_condition = "New";
        logger.info(order, "something is wrong {}", order.sheetName);
    }
}
