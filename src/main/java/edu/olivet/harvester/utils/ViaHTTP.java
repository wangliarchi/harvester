package edu.olivet.harvester.utils;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.CustomProxyConfig;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import javax.swing.*;

public class ViaHTTP {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViaHTTP.class);

    static int local_port = 8107;
    static String localhost = "127.0.0.1";
    static String remote_host = "23.92.28.212";
    static String login = "root";
    static String password = "V9tWhDrq;54~%9u";



    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            String path = Directory.Tools.path().replace("/", File.separator);
            String command = String.format("cmd.exe /c echo y | " + path + "\\plink.exe -ssh %s@%s -pw %s -D %s:%d",
                    login, remote_host, password, localhost, local_port);
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(command);
            InputStream std = p.getInputStream();
            OutputStream out = p.getOutputStream();
            InputStream err = p.getErrorStream();

            //out.write("ssh root@198.74.48.104 -D 127.0.0.1:8104\n".getBytes());
            //out.flush();

            Thread.sleep(1000);

            int value = 0;
            if (std.available() > 0) {
                System.out.println("STD:");
                value = std.read();
                System.out.print((char) value);

                while (std.available() > 0) {
                    value = std.read();
                    System.out.print((char) value);
                }
            }

            if (err.available() > 0) {
                System.out.println("ERR:");
                value = err.read();
                System.out.print((char) value);

                while (err.available() > 0) {
                    value = err.read();
                    System.out.print((char) value);
                }
            }

            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }


        String proxy = localhost + ":" + local_port;
        String proxyRules = String.format("socks=%s", proxy);
        String exceptions = "<local>";  // bypass proxy server for local web pages
        CustomProxyConfig customProxyConfig = new CustomProxyConfig(proxyRules, exceptions);

        Tools.switchLogMode(Configs.LogMode.Development);
        JFrame frame = new JFrame("Prototype of Harvester Web");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        BrowserView view = JXBrowserHelper.init("test", -1, customProxyConfig);
        frame.add(view, BorderLayout.CENTER);
        frame.setSize(1400, 860);
        frame.setLocationRelativeTo(null);
        UITools.setDialogAttr(frame, true);


        Browser.invokeAndWaitFinishLoadingMainFrame(view.getBrowser(), it -> it.loadURL("https://whatismyipaddress.com/"));
    }


}

