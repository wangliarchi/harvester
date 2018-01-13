package edu.olivet.harvester.fulfill.service;

import edu.olivet.foundations.utils.Strings;

import javax.swing.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/18/17 11:00 AM
 */
public class ProgressUpdater {
    static JProgressBar progressBar;
    static JLabel progressTextLabel;
    static int successCount = 0;
    static int failedCount = 0;
    static long start;

    public static void setProgressBarComponent(JProgressBar progressBar, JLabel progressTextLabel) {
        ProgressUpdater.progressBar = progressBar;
        ProgressUpdater.progressTextLabel = progressTextLabel;
        init(0);
    }

    public static void init(int total) {
        successCount = 0;
        failedCount = 0;
        if (progressBar != null) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setValue(0);
        }
        if (progressTextLabel != null) {
            progressTextLabel.setText(String.format("%d of %d completed", 0, total));
        }

        start = System.currentTimeMillis();
    }

    public synchronized static void updateTotal(int total) {
        if (progressBar != null) {
            progressBar.setMaximum(progressBar.getMaximum() + total);
        }
        if (progressTextLabel != null) {
            progressTextLabel.setText(String.format("%d of %d completed", progressBar.getValue(), total));
        }
    }

    public synchronized static void success() {
        successCount++;
        update();
    }

    public synchronized static void failed() {
        failedCount++;
        update();
    }

    private synchronized static void update() {
        int total = successCount + failedCount;
        if (progressBar != null) {
            progressBar.setValue(total);
        }

        if (progressTextLabel != null) {
            progressTextLabel.setText(String.format("%d of %d, %d success, %d failed, took %s",
                total, progressBar.getMaximum(), successCount, failedCount, Strings.formatElapsedTime(start)));
        }
    }


    public static String toTable() {
        return String.format("%s\t%s\t%s", successCount + failedCount, successCount, failedCount);
    }
}
