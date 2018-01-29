package edu.olivet.harvester.utils.http;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.exception.Exceptions.*;
import edu.olivet.harvester.utils.JXBrowserHelper;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nutz.aop.interceptor.async.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Html抓取助手，或者基于HttpClient，或者基于WebDriver
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Dec 27, 2014 4:30:16 PM
 */
public class HtmlFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFetcher.class);

    @Setter
    private boolean silentMode = true;

    @Repeat(expectedExceptions = BusinessException.class)
    public Document getDocument(String url) {
        if (silentMode) {
            try {
                return getDocumentSilently(url);
            } catch (Exception e) {
                //if robot check found, disable silent mode for this batch
                if (e instanceof ServerFailException || e instanceof RobotFoundException) {
                    silentMode = false;
                }
                LOGGER.error("failed to load html  via silent mode for {} - ", url, e);
            }
        }

        try {
            return getDocumentByBrowser(url);
        } catch (Exception e) {
            LOGGER.error("failed to load html via jxbrowser  for {} ", url, e);
        }

        throw new BusinessException("Fail to load html for url " + url);
    }

    /**
     * 基于HttpClient访问一个url地址，获取对应的Document
     *
     * @param url url地址
     */
    public Document getDocumentSilently(String url) {
        String html = HttpUtils.getHTML(url);
        checkResponse(html);
        Document doc = Jsoup.parse(html);
        return doc;
    }

    @Async
    public Document getDocumentByBrowser(String url) {
        BrowserView browserView = JXBrowserHelper.getGeneralBrowser();
        JXBrowserHelper.loadPage(browserView.getBrowser(), url);
        String html = browserView.getBrowser().getHTML();

        checkResponse(html);

        return Jsoup.parse(html);
    }

    public void checkResponse(String html) throws ItemNotAvailableException, RobotFoundException, ServerFailException {
        if (StringUtils.isBlank(html)) {
            throw new ItemNotAvailableException(html);
        }


        if (html.startsWith(HttpUtils.FAIL + ":" + HttpStatus.SC_NOT_FOUND)) {
            throw new ItemNotAvailableException(html);
        }

        if (RegexUtils.containsRegex(html, HttpUtils.HTTP_FAIL_REGEX)) {
            throw new ServerFailException(UIText.text("error.server.error", html));
        }

        Document doc = Jsoup.parse(html);
        if (doc.select("input#captchacharacters").size() > 0) {
            throw new RobotFoundException(doc.title());
        }
    }
}
