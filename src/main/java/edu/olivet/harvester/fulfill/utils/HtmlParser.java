package edu.olivet.harvester.fulfill.utils;

import com.google.inject.Singleton;
import com.mchange.lang.IntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 10:51 AM
 */
@Singleton
public class HtmlParser {


    /**
     * 根据多个CSS选择器查询DOM元素, 判定其中是否至少存在一个有效结果
     */
    public static boolean anyExist(Element doc, String... selectors) {
        Elements elements = elements(doc, selectors);
        return elements != null && elements.size() > 0;
    }

    /**
     * 在多个CSS选择器中查询DOM元素, 其中任何一个找到有效结果时即刻返回, 适用于同一页面具有多变样式的场景
     */
    public static Elements elements(Element doc, String... selectors) {
        Elements elements = null;
        for (String selector : selectors) {
            elements = doc.select(selector);
            if (elements.size() > 0) {
                return elements;
            }
        }
        return elements;
    }


    /**
     * 从给定的多个ID中, 定位第一个存在的DOM元素ID
     */
    public static String getExistingId(Element doc, String... ids) {
        for (String id : ids) {
            if (doc.select("#" + id).size() > 0) {
                return id;
            }
        }
        return null;
    }

    private boolean ifWavorLowQuailityWords(String rowHtml) {

        String[] lowqualities = new String[]{"water,damage,heavy,loose"};

        return !StringUtils.containsAny(rowHtml, lowqualities);
    }

    private static final Float ZERO = 0.0f;


    private int[] getDigits(String source) {
        String[] array = StringUtils.split(StringUtils.defaultString(source), '(');
        int[] digits = new int[2];
        digits[0] = IntegerUtils.parseInt(array[0], 0);
        if (array.length >= 2) {
            digits[1] = IntegerUtils.parseInt(array[1], 0);
        }
        return digits;
    }

    /**
     * <pre>
     * 在给定的元素范围内，按照给定的选择器查找，并将所得结果<strong>第一个</strong>元素的文本内容返回
     * 如果找不到，则返回空白字符串
     * </pre>
     */
    public static String text(Element doc, String selector) {
        Elements elements = doc.select(selector);
        if (elements.size() > 0) {
            return elements.get(0).text().trim();
        }
        return StringUtils.EMPTY;
    }

    public static String outertext(Element doc, String selector) {
        Elements elements = doc.select(selector);
        if (elements.size() > 0) {
            return elements.get(0).outerHtml().trim();
        }
        return StringUtils.EMPTY;
    }

    public static String ownText(Element doc, String selector) {
        Elements elements = doc.select(selector);
        if (elements.size() > 0) {
            return elements.get(0).ownText().trim();
        }
        return StringUtils.EMPTY;
    }

    /**
     * <pre>
     * 在给定的元素范围内，按照给定的选择器查找，并将所得结果<strong>所有</strong>元素的文本内容返回
     * 如果找不到，则返回空白字符串
     * </pre>
     */
    public static String texts(Element doc, String... selectors) {
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (elements.size() > 0) {
                return elements.text().trim();
            }
        }
        return StringUtils.EMPTY;
    }

    public static List<String> texts(Elements elements) {
        List<String> results = new ArrayList<String>(elements.size());
        for (Element element : elements) {
            results.add(element.text().trim());
        }
        return results;
    }

    /**
     * Replace "&lt;br&gt;" in html string to new line
     *
     * @param html source html string
     */
    public static String replaceBr2NewLine(String html) {
        String result = html.replace("<br>", StringUtils.LF).replace("</br>", StringUtils.EMPTY)
                .replace("<br />", StringUtils.LF);
        return result.trim();
    }

    public static String getInputValue(Element doc, String inputName) {
        Elements elements = doc.select("input[name=" + inputName + "]");
        if (elements.size() > 0) {
            return elements.get(0).val();
        }
        return StringUtils.EMPTY;
    }
}

