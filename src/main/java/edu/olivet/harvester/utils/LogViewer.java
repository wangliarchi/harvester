package edu.olivet.harvester.utils;

import edu.olivet.foundations.ui.Action;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.model.ConfigEnums.Log;
import edu.olivet.harvester.ui.dialog.DisplayContentDialog;
import edu.olivet.harvester.ui.dialog.LogViewDialog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 12:58 PM
 */
public class LogViewer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogViewer.class);
    /**
     * 在外部程序比如Notepad++或Notepad中打开日志文件
     *
     * @param log 日志类型
     */
    private void openLogExternally(Log log) {
        try {
            String filePath = log.filePath();
            openTextFile(filePath);
        } catch (IOException ex) {
            LOGGER.warn("打开{}文件过程中出现异常:", log.desc(), ex);
            // 如果调用外部 程序打开日志文件失败，使用内部实现
            openLogInternally(log);
        }
    }

    /**
     * 显示日志文件：或者基于Windows的Notepad打开文件，或者基于Swing UI查看文本(比如Linux系统下)
     *
     * @param log 日志类型{@link Log}
     */
    public void displayLogs(Log log) {
        if (SystemUtils.IS_OS_WINDOWS) {
            openLogExternally(log);
        } else {
            openLogInternally(log);
        }
    }

    /**
     * 在内部Swing UI中打开日志文件，目前只包含找单日志、做单日志、统计日志
     *
     * @param log 日志类型{@link Log}
     */
    private void openLogInternally(Log log) {
        if (log == Log.Success || log == Log.Statistic) {
            String context = RuntimeSettings.load().context();
            UITools.setDialogAttr(new LogViewDialog(null, true, context));
        }
    }

    private void displayContentDialog(final Action action) {
        UITools.setDialogAttr(new DisplayContentDialog(null, true, action));
    }


    private static final String NOTEPAD_PLUS = "C:\\Program Files (x86)\\Notepad++\\notepad++.exe";
    private static final String NOTEPAD = "C:\\Windows\\notepad.exe";

    public static void openTextFile(String filePath) throws IOException {
        // 如果有notepad++，优先用其打开日志文本
        if (new File(NOTEPAD_PLUS).exists()) {
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(NOTEPAD_PLUS, "-ro", filePath));
            pb.start();
        } else {
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(NOTEPAD, StringUtils.EMPTY, filePath));
            pb.start();
        }
    }
}
