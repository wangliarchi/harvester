package edu.olivet.harvester.fulfill.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.utils.http.HtmlFetcher;
import edu.olivet.harvester.utils.http.HtmlParser;
import edu.olivet.harvester.logger.ISBNLogger;
import edu.olivet.harvester.common.model.ConfigEnums;
import edu.olivet.harvester.utils.http.HttpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    public static void add2Cache(Country country, String isbn, String title) {
        String key = isbn + Constants.HYPHEN + country.name();
        add2Cache(key, title);
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


    public static String getTitleFromCache(Country country, String isbn) {
        String key = isbn + Constants.HYPHEN + country.name();
        String title = cache.get(key);
        if (StringUtils.isNotBlank(title)) {
            return title;
        }

        return null;
    }


    /**
     * 获取亚马逊上面一个ISBN对应产品的名称
     *
     * @param country 亚马逊国家
     * @param isbn 10位isbn
     */
    public static String getTitle(Country country, String isbn) {
        if (StringUtils.isBlank(isbn)) {
            return "";
        }
        String key = isbn + Constants.HYPHEN + country.name();
        String title = getTitleFromCache(country, isbn);
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
     * @param isbn 10位isbn
     */
    public static String _getTitle(String baseUrl, String isbn) {
        String title = StringUtils.EMPTY;
        try {
            title = getTitleAtProductPage(baseUrl, isbn);
            if (StringUtils.isBlank(title)) {
                title = getTitleAtOfferListPage(baseUrl, isbn);
            }
        } catch (Exception e) {
            LOGGER.warn("在{}上面读取{}书名过程中出现异常:", baseUrl, isbn, e);
        }

        if (StringUtils.isBlank(title)) {
            LOGGER.warn("在{}上面读取{}书名时没有返回有效结果", baseUrl, isbn);
        }
        return title;
    }

    private static final String[] CONDITIONS = {
            ConditionUtils.Condition.New.name().toLowerCase(),
            ConditionUtils.Condition.Used.name().toLowerCase(),
            ConditionUtils.Condition.Collectible.name().toLowerCase()
    };

    private static final int[] INDEXES = {10, 20, 30};

    public static String getTitleAtOfferListPage(String baseUrl, String isbn) {
        int i = RandomUtils.nextInt(0, 3);
        String cond = CONDITIONS[i];
        int index = INDEXES[i];

        String correctedIsbn = ISBNUtils.correct(isbn);
        String html = HttpUtils.getHTML(baseUrl +
                String.format(OrderCountryUtils.OFFER_LIST_URL_PATTERN, correctedIsbn, cond, cond, index));
        Document doc = Jsoup.parse(html);
        if (doc.select("#captchacharacters").size() > 0) {
            LOGGER.warn("在{}上面基于HttpClient读取{}书名时被判定为机器人访问:{}", baseUrl, correctedIsbn, doc.title());
            return StringUtils.EMPTY;
        }

        return doc.title().replaceFirst(RegexUtils.Regex.AMAZON_BUYING_CHOICE.val(), StringUtils.EMPTY).trim();
    }

    /**
     * {"asin":"B0027X8YT2",
     * "title":"Gundam Mr. Color 174 - Fluorescent Pink (Gloss \/ Primary) Paint 10ml. Bottle Hobby",
     * "binding":"Toy","publication_date":null,
     * "brand":"Mr. Color","type":"Toy","region":"US","manufacturer":"Mr. Hobby"}
     */
    public static String getTitleAtESWeb(String isbn) {
        String correctedIsbn = ISBNUtils.correct(isbn);
        try {
            String url = String.format("http://35.188.127.209/web/product.php?asin=%s", correctedIsbn);
            String json = HttpUtils.get(url);
            JSONObject response = JSON.parseObject(json);
            return response.getString("title");
        } catch (Exception e) {
            throw Lang.wrapThrow(e);
        }
    }

    public static String getTitleAtProductPage(String baseUrl, String isbn) {
        try {
            return getTitleAtESWeb(isbn);
        } catch (Exception e) {
            //
        }
        String correctedIsbn = ISBNUtils.correct(isbn);
        String pageUrl = String.format(PRODUCT_PAGE_URL, baseUrl, correctedIsbn);

        Document doc = HtmlFetcher.getDocument(pageUrl);
        String title = HtmlParser.text(doc, "#productTitle");
        if (StringUtils.isBlank(title)) {
            title = HtmlParser.text(doc, "#btAsinTitle");
        }
        if (StringUtils.isBlank(title)) {
            title = doc.title().replaceFirst(": Amazon.*", StringUtils.EMPTY).trim();
        }

        if (doc.select("#captchacharacters").size() > 0) {
            LOGGER.warn("在{}上面基于JSOUP读取{}书名时被判定为机器人访问:{}", baseUrl, correctedIsbn, doc.title());
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

    public static void main(String[] args) {
        String title = ISBNUtils.getTitle(Country.US, "1401303706");
        System.out.println(title);
    }
}
