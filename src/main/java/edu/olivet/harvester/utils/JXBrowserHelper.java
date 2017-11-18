package edu.olivet.harvester.utils;

import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.dom.*;
import com.teamdev.jxbrowser.chromium.events.FinishLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.LoadAdapter;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import com.teamdev.jxbrowser.chromium.swing.internal.LightWeightWidget;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.BuyerPanel;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyEventType.*;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/26/17 2:09 PM
 */
public class JXBrowserHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JXBrowserHelper.class);
    private static final int TIME_OUT_SECONDS = 60;

    static {
        try {
            Field e = ay.class.getDeclaredField("e");
            e.setAccessible(true);
            Field f = ay.class.getDeclaredField("f");
            f.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(e, e.getModifiers() & ~Modifier.FINAL);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            e.set(null, new BigInteger("1"));
            f.set(null, new BigInteger("1"));
            modifiersField.setAccessible(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static final double DEFAULT_ZOOM_OUT_LEVEL = -3.6;

    public static void saveScreenshot(String filePath, BrowserView browserView) {
        LightWeightWidget lightWeightWidget = (LightWeightWidget) browserView.getComponent(0);
        Image image = lightWeightWidget.getImage();
        try {
            File file = new File(filePath);
            Tools.createFileIfNotExist(file);
            ImageIO.write((RenderedImage) image, "PNG", file);
        } catch (Exception e) {
            LOGGER.error("尝试保存截图文件到{}失败：", filePath, e);
        }
    }

    public static void saveHTMLSourceFile(Browser browser) {
        String title = browser.getTitle().replaceAll(" ", "");
        title = RegexUtils.getMatched(title, "[A-Za-z-]");
        String filePath = Directory.WebPage.path() + "/specials/" + System.currentTimeMillis() + title + ".html";
        saveHTMLSourceFile(filePath, browser);
    }

    public static void saveHTMLSourceFile(String filePath, Browser browser) {
        try {
            File file = new File(filePath);
            Tools.writeStringToFile(file, browser.getHTML());
        } catch (Exception e) {
            LOGGER.error("尝试保存HTML文件到{}失败：", filePath, e);
        }
    }


    public static void saveOrderScreenshot(Order order, BuyerPanel buyerPanel, String step) {

        String title = buyerPanel.getBrowserView().getBrowser().getTitle().replaceAll(" ", "");
        title = RegexUtils.getMatched(title, "[A-Za-z-]");
        String filePath = Directory.WebPage.path() + "/orders/" + order.sheetName.replaceAll("/", "") + "/" + order.row + "_" + order.order_id + "/images/" + step + "-" + title + ".png";
        saveScreenshot(filePath, buyerPanel.getBrowserView());

        String htmlFilePath = filePath.replaceAll(".png", ".html").replaceAll("/images/", "/html/");
        saveHTMLSourceFile(htmlFilePath, buyerPanel.getBrowserView().getBrowser());

    }


    /**
     * 初始化一个JXBrowser View
     *
     * @param profileDirName 该BrowserView对应Profile路径名称，需要注意：一个路径同一时间只能一个Browser使用
     * @param zoomLevel      缩放级别，100%常规模式可设定为1，放大或缩小可以设置其他值
     * @return 初始化好的BrowserView实例
     */
    public static BrowserView init(String profileDirName, double zoomLevel) {
        String dataDir = Directory.Tmp.path() + File.separator + profileDirName;

        BrowserContextParams params = new BrowserContextParams(dataDir);
        BrowserContext context = new BrowserContext(params);
        Browser browser = new Browser(BrowserType.LIGHTWEIGHT, context);
        browser.setZoomEnabled(true);

        BrowserPreferences preferences = browser.getPreferences();
        preferences.setImagesEnabled(true);
        preferences.setJavaScriptEnabled(true);
        browser.setPreferences(preferences);

        // 默认缩小，方便操作
        browser.addLoadListener(new LoadAdapter() {
            @Override
            public void onFinishLoadingFrame(FinishLoadingEvent event) {
                if (event.isMainFrame() && browser.getZoomLevel() > zoomLevel) {
                    browser.setZoomLevel(zoomLevel);
                }
            }
        });

        return new BrowserView(browser);
    }

    public static BrowserView init(String profileDirName) {
        return init(profileDirName, DEFAULT_ZOOM_OUT_LEVEL);
    }

    @InvokedExternally
    public static void wait(Browser browser, By by) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            if (browser.getDocument().findElement(by) == null) {
                WaitTime.Shorter.execute();
            } else {
                break;
            }
        }

        if (browser.getDocument().findElement(by) == null) {
            saveHTMLSourceFile(browser);
            throw new BusinessException(String.format("等待%d次，%d秒之后，期待的Dom元素%s(%s)仍未出现",
                    Constants.MAX_REPEAT_TIMES, Constants.MAX_REPEAT_TIMES * WaitTime.Shorter.val(), by.getValue(), by.getType()));
        }
    }

    @InvokedExternally
    public static void waitUntilVisible(Browser browser, String selector) {

        int timeConsumed = 0;
        while (true) {

            if (!isVisible(browser, selector)) {
                WaitTime.Shortest.execute();
                timeConsumed += WaitTime.Shortest.val();

                if (timeConsumed > TIME_OUT_SECONDS) {
                    saveHTMLSourceFile(browser);
                    throw new BusinessException(String.format("等待%d秒之后，期待的Dom元素%s还不可见",
                            timeConsumed, selector));
                }
                continue;
            }
            break;
        }

    }


    @InvokedExternally
    //insert an element to page, and wait until it's gone
    public static void waitUntilNewPageLoaded(Browser browser) {

        //insert tracker
        DOMDocument document = browser.getDocument();
        DOMNode root = document.findElement(By.tagName("body"));
        DOMNode textNode = document.createTextNode("Loading...");
        DOMElement paragraph = document.createElement("p");
        paragraph.setAttribute("id", "loading-checker");
        paragraph.appendChild(textNode);
        root.appendChild(paragraph);

        //check tracker
        waitUntilNotFound(browser, "#loading-checker");
    }

    @InvokedExternally
    public static void waitUntilNotFound(Browser browser, String selector) {

        int timeConsumed = 0;
        while (true) {
            DOMElement element = JXBrowserHelper.selectElementByCssSelector(browser, selector);
            if (element != null) {
                if (!isVisible(browser, selector)) {
                    break;
                }

                WaitTime.Shortest.execute();
                timeConsumed += WaitTime.Shortest.val();

                if (timeConsumed > TIME_OUT_SECONDS) {
                    saveHTMLSourceFile(browser);
                    throw new BusinessException(String.format("等待%d秒之后，期待的Dom元素%s还在",
                            timeConsumed, selector));
                }
                continue;
            }
            break;
        }

    }


    /**
     * <pre>
     * Wait until element loaded.
     * </pre>
     */
    public static DOMElement selectElementByCssSelectorWaitUtilLoaded(Browser browser, String selector) {
        int timeConsumed = 0;
        while (true) {
            DOMElement element = JXBrowserHelper.selectElementByCssSelector(browser, selector);
            if (element == null) {
                WaitTime.Shortest.execute();
                timeConsumed += WaitTime.Shortest.val();

                if (timeConsumed > TIME_OUT_SECONDS) {
                    saveHTMLSourceFile(browser);
                    throw new BusinessException(String.format("等待%d秒之后，期待的Dom元素%s仍未出现",
                            timeConsumed, selector));
                }
                continue;
            }
            return element;
        }
    }


    @InvokedExternally
    public static Map<String, String> getCookies(Browser browser) {
        java.util.List<Cookie> cookies = browser.getCookieStorage().getAllCookies();
        Map<String, String> map = new HashMap<>();
        for (Cookie cookie : cookies) {
            map.put(cookie.getName(), cookie.getValue());
        }
        return map;
    }

    @InvokedExternally
    public static CookieStore getCookieStore(Browser browser) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        for (Cookie cookie : browser.getCookieStorage().getAllCookies()) {
            BasicClientCookie bsc = new BasicClientCookie(cookie.getName(), cookie.getValue());
            bsc.setDomain(cookie.getDomain());
            bsc.setSecure(cookie.isSecure());
            bsc.setExpiryDate(new Date(cookie.getExpirationTime()));
            bsc.setPath(cookie.getPath());
            cookieStore.addCookie(bsc);
        }
        return cookieStore;
    }

    public static void forwardKeyEvent(Browser browser, BrowserKeyEvent.KeyCode code, char character) {
        browser.forwardKeyEvent(new BrowserKeyEvent(PRESSED, code, character));
        browser.forwardKeyEvent(new BrowserKeyEvent(TYPED, code, character));
        browser.forwardKeyEvent(new BrowserKeyEvent(RELEASED, code, character));
    }

    public static void loadPage(Browser browser, String url) {
        Browser.invokeAndWaitFinishLoadingMainFrame(browser, it -> it.loadURL(url));
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public static DOMElement selectElementByName(Browser browser, String name) {
        return browser.getDocument().findElement(By.name(name));
    }

    public static DOMElement selectElementByCssSelector(Browser browser, String selector) {
        return selectElementByCssSelector(browser.getDocument(), selector);
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public static DOMElement selectElementByCssSelector(DOMDocument document, String selector) {
        return document.findElement(By.cssSelector(selector));
    }

    public static DOMElement selectElementByCssSelector(DOMElement element, String selector) {
        return element.findElement(By.cssSelector(selector));
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public static List<DOMElement> selectElementsByCssSelector(DOMDocument document, String selector) {
        return document.findElements(By.cssSelector(selector));
    }

    public static List<DOMElement> selectElementsByCssSelector(Browser browser, String selector) {
        return selectElementsByCssSelector(browser.getDocument(), selector);
    }

    public static List<DOMElement> selectElementsByCssSelector(DOMElement element, String selector) {
        return element.findElements(By.cssSelector(selector));
    }


    public static List<DOMElement> selectElementsByCssSelectors(Browser browser, String... selectors) {
        String selector = StringUtils.join(selectors, ",");
        return selectElementsByCssSelector(browser.getDocument(), selector);
    }

    public static List<DOMElement> selectElementsByCssSelectors(DOMDocument document, String... selectors) {
        String selector = StringUtils.join(selectors, ",");
        return selectElementsByCssSelector(document, selector);
    }


    public static List<DOMElement> selectElementsByCssSelectors(DOMElement element, String... selectors) {
        String selector = StringUtils.join(selectors, ",");
        return selectElementsByCssSelector(element, selector);
    }

    public static DOMElement selectVisibleElement(Browser browser, String selector) {
        List<DOMElement> elements = selectElementsByCssSelector(browser, selector);
        for (DOMElement element : elements) {
            Rectangle r = element.getBoundingClientRect();
            if (!r.isEmpty()) {
                return element;
            }
        }

        return null;
    }


    public static boolean isVisible(DOMElement element) {
        return !isHidden(element);
    }

    public static boolean isHidden(DOMElement element) {
        return element.getBoundingClientRect().isEmpty();
    }

    public static String text(DOMElement doc, String selector) {
        DOMElement element = selectElementByCssSelector(doc, selector);
        if (element != null) {
            return element.getInnerText().trim();
        }
        return StringUtils.EMPTY;
    }

    public static String text(Browser browser, String selector) {
        DOMElement element = selectElementByCssSelector(browser, selector);
        if (element != null) {
            return element.getInnerText().trim();
        }
        return StringUtils.EMPTY;
    }

    public static void fillValueForFormField(Browser browser, String selector, String value) {
        DOMElement element = JXBrowserHelper.selectElementByCssSelector(browser, selector);
        ((DOMFormControlElement) element).setValue(value);
    }

    public static void fillValueForFormField(DOMElement parentElement, String selector, String value) {
        DOMElement element = JXBrowserHelper.selectElementByCssSelector(parentElement, selector);
        ((DOMFormControlElement) element).setValue(value);
    }

    public static void setValueForFormSelect(Browser browser, String selector, String value) {
        DOMSelectElement select = (DOMSelectElement) JXBrowserHelper.selectElementByCssSelector(browser, selector);
        List<DOMOptionElement> options = select.getOptions();

        for (DOMElement optionElm : options) {
            try {
                DOMOptionElement option = (DOMOptionElement) optionElm;
                if (value.equalsIgnoreCase(option.getAttribute("value")) || value.equalsIgnoreCase(option.getInnerText())) {
                    option.setSelected(true);
                    break;
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public static boolean isVisible(Browser browser, String selector) {
        JSValue result = browser.executeJavaScriptAndReturnValue(String.format("var elm = document.querySelector('%s');var style = window.getComputedStyle(elm); style.display;", selector));
        try {
            return !"none".equalsIgnoreCase(result.getStringValue());
        } catch (Exception e) {
            return false;
        }


    }

    public static void main(String[] args) {
        Tools.switchLogMode(Configs.LogMode.Development);
        JFrame frame = new JFrame("Prototype of Harvester Web");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        BrowserView view = JXBrowserHelper.init("test");
        frame.add(view, BorderLayout.CENTER);
        frame.setSize(1400, 860);
        frame.setLocationRelativeTo(null);
        UITools.setDialogAttr(frame, true);


        Browser.invokeAndWaitFinishLoadingMainFrame(view.getBrowser(),
                it -> it.loadURL("https://www.amazon.com/dp/B01FLO5914"));

        System.out.println(JXBrowserHelper.isVisible(view.getBrowser(), "#be"));
    }
}
