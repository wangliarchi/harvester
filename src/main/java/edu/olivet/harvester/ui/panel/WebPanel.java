package edu.olivet.harvester.ui.panel;


import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.utils.JXBrowserHelper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 9:52 AM
 */
public abstract class WebPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebPanel.class);
    @Getter
    @Setter
    protected BrowserView browserView;
    protected double zoomLevel;

    public WebPanel(BorderLayout borderLayout) {
        super(borderLayout);
    }

    public abstract void toHomePage();


    public abstract String getKey();

    public abstract String getIcon();

    public abstract boolean running();

    public abstract String profilePathName();

    public void recreateBrowser() {
        killBrowser();
        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel);
        toHomePage();
        WaitTime.Short.execute();
    }

    public void killBrowser() {
        try {
            int pid = (int) browserView.getBrowser().getRenderProcessInfo().getPID();
            killProcess(pid);
        } catch (Exception e) {
            //
            //LOGGER.error("", e);
        }

        try {
            browserView.getBrowser().dispose();
        } catch (Exception e) {
            //
        }
    }

    private void killProcess(int pid) {
        Runtime rt = Runtime.getRuntime();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                rt.exec("taskkill " + pid);
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        } else {
            try {
                rt.exec("kill -9 " + pid);
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
    }

    public void toWelcomePage() {
        Browser browser = browserView.getBrowser();
        String html = Configs.loadByFullPath("/welcome.html");
        browser.loadHTML(html);
    }
}
