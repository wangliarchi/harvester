package edu.olivet.harvester.utils.common;

import edu.olivet.foundations.utils.BusinessException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 10:40 AM
 */
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    private static PoolingHttpClientConnectionManager connectionManager;
    /**
     * Http连接超时上限设置:{@value} (毫秒)
     */
    private static final int TIME_OUT = 30000;
    public static final String FAIL = "Fail";
    public static final String HTTP_FAIL_REGEX = FAIL + ":[0-9]{3}";
    public static final String COLON = ":";
    public static final int MAX_MSG_LENGTH = 150;

    /**
     * 访问一个url地址，获取返回的文本内容
     *
     * @param url url地址
     */
    public static String getText(String url) {
        return getResponse(url, false);
    }

    /**
     * 提交一个Post请求
     */
    public static String post(String url, List<NameValuePair> nvps) {
        CloseableHttpClient client = buildHttpClient();

        HttpPost post = new HttpPost(url);
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        String result = null;
        try {
            post.setEntity(new UrlEncodedFormEntity(nvps));
            response = client.execute(post);

            entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                result = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
            } else {
                throw new BusinessException(String.format("Failed to submit post request against url %s, http status code: %s", url, statusCode));
            }
        } catch (IOException e) {
            logger.error("提交Post请求(Url: {})过程中出现异常:", url, e);
            throw new BusinessException(e);
        } finally {
            post.releaseConnection();
            IOUtils.closeQuietly(response);
        }
        return result;
    }

    /**
     * 访问一个url地址，获取其对应的response结果字符串
     *
     * @param url url地址
     */
    private static String getResponse(String url, boolean acceptHtml) {
        CloseableHttpClient client = buildHttpClient();

        HttpGet get = acceptHtml ? prepareHttpGet(url) : new HttpGet(url);

        CloseableHttpResponse response = null;
        String result = null;
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                result = EntityUtils.toString(entity);
            } else {
                String msg = StringUtils.defaultString(EntityUtils.toString(entity));
                logger.warn("访问{}没有成功，HTTP返回状态码:{}, {}", url, statusCode, msg);
                result = FAIL + COLON + statusCode + COLON + StringUtils.abbreviate(msg, MAX_MSG_LENGTH);
            }
            EntityUtils.consume(entity);
        } catch (Exception e) {
            logger.warn("访问URL:{}时出现异常:", url, e);
        } finally {
            get.releaseConnection();
            IOUtils.closeQuietly(response);
        }
        return result;
    }

    /**
     * 访问一个url地址，获取返回的html代码
     *
     * @param url url地址
     */
    public static String getHTML(String url) {
        return getResponse(url, true);
    }

    /**
     * 创建一个{@link CloseableHttpClient}实例
     *
     * @return 一个{@link CloseableHttpClient}实例
     */
    public static CloseableHttpClient buildHttpClient() {
        initConnManager();

        RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BEST_MATCH)
                .setConnectTimeout(TIME_OUT).build();
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(globalConfig).build();

        return client;
    }

    /**
     * 初始化Http连接池
     */
    private static synchronized void initConnManager() {
        if (connectionManager == null) {
            connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(200);
            connectionManager.setDefaultMaxPerRoute(50);
        }
    }

    /**
     * 关闭Http连接池
     */
    public static void closeConnectionPool() {
        IOUtils.closeQuietly(connectionManager);
    }

    /**
     * 准备一个{@link HttpGet}实例
     */
    public static HttpGet prepareHttpGet(String url) {
        HttpGet get = new HttpGet(url);
        get.addHeader("Connection", "keep-alive");
        get.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.90 Safari/537.36");
        get.addHeader("Accept-Encoding", "gzip, deflate, sdch");
        get.addHeader("Accept-Language", "en-US,en;q=0.8");
        get.addHeader("Cache-Control", "max-age=0");
        try {
            URL _url = new URL(url);
            get.addHeader("Host", _url.getHost());
        } catch (MalformedURLException ex) {
            logger.error("访问url获取对应HTML代码时出错。原因：非法url:{}", url);
            throw Lang.wrapThrow(ex);
        }
        return get;
    }
}
