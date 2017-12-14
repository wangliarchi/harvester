package edu.olivet.harvester.fulfill.service;

import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.RuntimeSettingsPanel;

import javax.swing.*;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/18/17 11:00 AM
 */
public class ProgressUpdater {
    static JProgressBar progressBar = RuntimeSettingsPanel.getInstance().progressBar;
    static JLabel progressTextLabel = RuntimeSettingsPanel.getInstance().progressTextLabel;
    static int successCount = 0;
    static int failedCount = 0;
    static long start;

    public static void init(List<Order> orders) {
        successCount = 0;
        failedCount = 0;
        progressBar.setMinimum(0);
        progressBar.setMaximum(orders.size());
        progressBar.setValue(0);
        progressTextLabel.setText(String.format("%d of %d completed", 0, orders.size()));
        start = System.currentTimeMillis();
    }

    public static void success() {
        successCount++;
        update();
    }

    public static void failed() {
        failedCount++;
        update();
    }

    private static void update() {
        int total = successCount + failedCount;
        progressBar.setValue(total);
        progressTextLabel.setText(String.format("%d of %d, %d success, %d failed, took %s", total, progressBar.getMaximum(), successCount, failedCount, Strings.formatElapsedTime(start)));
    }


    public static String toTable() {
        return String.format("%s\t%s\t%s", successCount + failedCount, successCount, failedCount);
    }
}
