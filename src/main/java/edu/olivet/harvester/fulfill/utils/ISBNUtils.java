package edu.olivet.harvester.fulfill.utils;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.logger.ISBNLogger;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.utils.common.HttpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 10:29 AM
 */
public class ISBNUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ISBNUtils.class);
    private static final String PRODUCT_PAGE_URL = "%s/dp/%s";

    private static Map<String, String> cache = new ConcurrentHashMap<>();
    private static AtomicBoolean initFlag = new AtomicBoolean(false);

    /**
     * 载入本地保存的ISBN-名称文件到缓存中
     */
    public static void initCache() {
        try {
            if (initFlag.get()) {
                LOGGER.debug("ISBN书名缓存已经载入过了");
                return;
            }

            List<String> titles = FileUtils.readLines(ConfigEnums.Log.ISBN.file(), Constants.UTF8);
            cache = Configs.parseKeyValues(titles);
            initFlag.set(true);
            LOGGER.debug("ISBN书名缓存载入完毕，累计载入:{}条记录", titles.size());
        } catch (IOException e) {
            LOGGER.error("读取ISBN本地文件时出现异常:", e);
        }
    }

    public static boolean inCache(String key) {
        return cache.get(key) != null;
    }

    public static void add2Cache(String key, String title) {
        cache.put(key, title);
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        cache.clear();
        initFlag.set(false);
    }

    /**
     * 获取亚马逊上面一个ISBN对应产品的名称
     *
     * @param country 亚马逊国家
     * @param isbn    10位isbn
     */
    public static String getTitle(Country country, String isbn) {
        String key = isbn + Constants.HYPHEN + country.name();
        String title = cache.get(key);
        if (StringUtils.isNotBlank(title)) {
            return title;
        }

        title = _getTitle(country.baseUrl(), isbn);
        if (StringUtils.isNotBlank(title)) {
            ISBNLogger.save(key + "=" + title);
            cache.put(key, title);
        }
        return title;
    }

    /**
     * 获取亚马逊上面一个ISBN对应产品的名称
     *
     * @param baseUrl 亚马逊网址Host
     * @param isbn    10位isbn
     */
    public static String _getTitle(String baseUrl, String isbn) {
        String title = StringUtils.EMPTY;
        try {
            title = getTitleAtProductPage(baseUrl, isbn);
            if (StringUtils.isBlank(title)) {
                title = getTitleAtOfferListPage(baseUrl, isbn);
            }
        } catch (IOException e) {
            LOGGER.warn("在{}上面读取{}书名过程中出现异常:", baseUrl, isbn, e);
        }

        if (StringUtils.isBlank(title)) {
            LOGGER.warn("在{}上面读取{}书名时没有返回有效结果", baseUrl, isbn);
        }
        return title;
    }

    private static final String[] CONDITIONS = {ConditionUtils.Condition.New.name().toLowerCase(), ConditionUtils.Condition.Used.name().toLowerCase(), ConditionUtils.Condition.Collectible.name().toLowerCase()};
    private static final int[] INDEXES = {10, 20, 30};

    public static String getTitleAtOfferListPage(String baseUrl, String isbn) {
        int i = RandomUtils.nextInt(0, 3);
        String cond = CONDITIONS[i];
        int index = INDEXES[i];

        String _isbn = ISBNUtils.correct(isbn);
        String html = HttpUtils.getHTML(baseUrl + String.format(OrderCountryUtils.OFFER_LIST_URL_PATTERN, _isbn, cond, cond, index));
        Document doc = Jsoup.parse(html);
        if (doc.select("#captchacharacters").size() > 0) {
            LOGGER.warn("在{}上面基于HttpClient读取{}书名时被判定为机器人访问:{}", baseUrl, _isbn, doc.title());
            return StringUtils.EMPTY;
        }

        return doc.title().replaceFirst(RegexUtils.Regex.AMAZON_BUYING_CHOICE.val(), StringUtils.EMPTY).trim();
    }

    public static String getTitleAtProductPage(String baseUrl, String isbn) throws IOException {
        String _isbn = ISBNUtils.correct(isbn);
        Connection conn = Jsoup.connect(String.format(PRODUCT_PAGE_URL, baseUrl, _isbn));
        conn.userAgent("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.header("Accept-Encoding", "gzip, deflate, sdch");
        conn.header("Accept-Language", "en-US,en;q=0.8");
        conn.header("Connection", "keep-alive");
        String host = new URL(baseUrl).getHost();
        conn.header("Host", host);
        conn.timeout(5000);

        Document doc = conn.get();
        String title = HtmlParser.text(doc, "#productTitle");
        if (StringUtils.isBlank(title)) {
            title = HtmlParser.text(doc, "#btAsinTitle");
        }
        if (StringUtils.isBlank(title)) {
            title = doc.title().replaceFirst(": Amazon.*", StringUtils.EMPTY).trim();
        }

        if (doc.select("#captchacharacters").size() > 0) {
            LOGGER.warn("在{}上面基于JSOUP读取{}书名时被判定为机器人访问:{}", baseUrl, _isbn, doc.title());
            return StringUtils.EMPTY;
        }

        return title;
    }

    /**
     * 对数字类型文本做容错处理时，需补上缺少的字符内容:{@value}
     */
    public static final char DIGIT_ZERO = '0';

    /**
     * 数字类ISBN如果不是文本，前导0会略去，需做容错处理，补上缺少的位数到10位
     */
    public static String correct(String isbn) {
        if (StringUtils.isEmpty(isbn)) {
            return StringUtils.EMPTY;
        }
        return StringUtils.leftPad(isbn, 10, DIGIT_ZERO);
    }
}
