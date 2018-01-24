package edu.olivet.harvester.utils.http;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Html抓取助手，或者基于HttpClient，或者基于WebDriver
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Dec 27, 2014 4:30:16 PM
 */
public class HtmlFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFetcher.class);

    /**
     * 基于HttpClient访问一个url地址，获取对应的Document
     *
     * @param url url地址
     */
    public Document getDocumentSilently(String url) throws ItemNotAvailableException, RobotFoundException, ServerFailException {
        String html = HttpUtils.getHTML(url);
        if (StringUtils.isBlank(html)) {
            return null;
        } else if (RegexUtils.containsRegex(html, HttpUtils.HTTP_FAIL_REGEX)) {
            // 遇到服务器端返回形如404、503等代码时尝试等待少许时间之后重复读取一次
            WaitTime.Shortest.execute();
            html = HttpUtils.getHTML(url);
        }

        //use jxbrowser if failed
        if (RegexUtils.containsRegex(html, HttpUtils.HTTP_FAIL_REGEX) || StringUtils.containsIgnoreCase(html, "captchacharacters")) {
            BrowserView browserView = JXBrowserHelper.getGeneralBrowser();
            JXBrowserHelper.loadPage(browserView.getBrowser(), url);
            html = browserView.getBrowser().getHTML();
        }
        if (StringUtils.isBlank(html)) {
            return null;
        }

        Document doc = Jsoup.parse(html);
        // 404意味着无货，可以继续在其他国家寻找，如果是其他比如500的内部服务器错误，直接取消本次找单
        if (html.startsWith(HttpUtils.FAIL + ":" + HttpStatus.SC_NOT_FOUND)) {
            throw new ItemNotAvailableException(html);
        } else if (html.matches(HttpUtils.HTTP_FAIL_REGEX)) {
            throw new ServerFailException(UIText.text("error.server.error", html));
        } else if (doc.select("input#captchacharacters").size() > 0) {
            throw new RobotFoundException(doc.title());
        }
        return doc;
    }
}
