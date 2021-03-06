package edu.olivet.harvester.fulfill.service;

import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.ui.utils.ProgressBarComponent;

import javax.swing.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/18/17 11:00 AM
 */
public class ProgressUpdater {
    private static JProgressBar progressBar;
    private static JLabel progressTextLabel;
    private static int successCount = 0;
    private static int failedCount = 0;
    private static long start;

    public static void setProgressBarComponent(ProgressBarComponent progressBarComponent, int total) {
        ProgressUpdater.progressBar = progressBarComponent.getProgressBar();
        ProgressUpdater.progressTextLabel = progressBarComponent.getProgressTextLabel();
        init(total);
    }

    public static void setProgressBarComponent(ProgressBarComponent progressBarComponent) {
        setProgressBarComponent(progressBarComponent, 0);
    }

    private static void init(int total) {
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

    public static synchronized void updateTotal(int total) {
        if (progressBar != null) {
            progressBar.setMaximum(progressBar.getMaximum() + total);
        }

        update();
    }

    public static synchronized void setTotal(int total) {
        if (progressBar != null) {
            progressBar.setMaximum(total);
        }

        update();
    }

    public static synchronized void success() {
        successCount++;
        update();
    }

    public static synchronized void failed() {
        failedCount++;
        update();
    }

    private static synchronized void update() {
        int total = successCount + failedCount;
        if (progressBar != null) {
            progressBar.setValue(total);

            if (progressTextLabel != null) {
                progressTextLabel.setText(String.format("%d of %d, %d success, %d failed, took %s",
                        total, progressBar.getMaximum(), successCount, failedCount, Strings.formatElapsedTime(start)));
            }

            if (progressBar.getPercentComplete() == 1) {
                try {
                    PSEventListener.end();
                } catch (Exception e) {
                    //
                }
            }
        }


    }


    public static String toTable() {
        return String.format("%s\t%s\t%s", successCount + failedCount, successCount, failedCount);
    }

    public static synchronized String progress() {
        return String.format("%s/%s", successCount + failedCount, progressBar.getMaximum());
    }

    public static synchronized String timeSpent() {
        return Strings.formatElapsedTime(start);
    }
}
