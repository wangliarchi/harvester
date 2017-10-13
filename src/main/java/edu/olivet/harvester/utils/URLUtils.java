package edu.olivet.harvester.utils;

import org.nutz.lang.Lang;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/12/17 8:46 PM
 */
public class URLUtils {

    public static final String UTF8 = "UTF-8";

    public static String encodeParamValue(String param) {
        try {
            return URLEncoder.encode(param, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw Lang.wrapThrow(e);
        }
    }

    public static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw Lang.wrapThrow(e);
        }
    }
}
