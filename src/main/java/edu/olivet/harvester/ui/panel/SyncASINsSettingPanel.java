package edu.olivet.harvester.ui.panel;

import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.job.BackgroundJob;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:31 PM
 */
public class SyncASINsSettingPanel extends JPanel {
    public SyncASINsSettingPanel() {
        initComponents();
    }

    private JComboBox<String> enableAutoDownloadComboBox;
    private TimePicker exportTimePicker;
    private JComboBox<Integer> allowedRangeComBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel enableAutoExportLabel = new JLabel("Enable Auto Sync ASINs?");
        enableAutoDownloadComboBox = new JComboBox<>();
        enableAutoDownloadComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if (systemSettings.isEnableASINsSyncing()) {
            enableAutoDownloadComboBox.setSelectedItem("Yes");
        } else {
            enableAutoDownloadComboBox.setSelectedItem("No");
        }

        final JLabel autoExportTimeLabel = new JLabel("Daily Sync at ");
        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.initialTime = systemSettings.getAsinSyncTime();
        exportTimePicker = new TimePicker(timeSettings);

        final JLabel rangeLabel = new JLabel("before or after");
        allowedRangeComBox = new JComboBox<>();
        allowedRangeComBox.setModel(new DefaultComboBoxModel<>(new Integer[] {0, 5, 10, 15, 20, 25, 30, 60, 90, 120}));
        allowedRangeComBox.setSelectedItem(systemSettings.getAsinSyncAllowedRange());
        final JLabel unitLabel = new JLabel("minutes");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 150;
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(enableAutoExportLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(enableAutoDownloadComboBox, 100, 100, 100)
                                .addContainerGap()

                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(autoExportTimeLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(exportTimePicker, 100, 100, 100)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(rangeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(allowedRangeComBox, 100, 100, 100)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(unitLabel)
                                .addContainerGap()
                        ));


        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(enableAutoExportLabel)
                                        .addComponent(enableAutoDownloadComboBox)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(autoExportTimeLabel)
                                        .addComponent(exportTimePicker)
                                        .addComponent(rangeLabel)
                                        .addComponent(allowedRangeComBox)
                                        .addComponent(unitLabel)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }


    public void collectData() {
        SystemSettings systemSettings = SystemSettings.reload();
        boolean oldData = systemSettings.isEnableASINsSyncing();
        //noinspection ConstantConditions
        if ("Yes".equalsIgnoreCase(enableAutoDownloadComboBox.getSelectedItem().toString())) {
            systemSettings.setEnableASINsSyncing(true);
        } else {
            systemSettings.setEnableASINsSyncing(false);
        }

        if (oldData != systemSettings.isEnableASINsSyncing()) {
            TaskScheduler taskScheduler = ApplicationContext.getBean(TaskScheduler.class);
            taskScheduler.deleteJob(BackgroundJob.SyncASIN.getClazz());
            if (oldData) {
                ProgressLogsPanel.getInstance().displayMsg("ASIN auto sync job was disabled successfully.");
            } else {
                Date nextTriggerTime = taskScheduler.startJob(BackgroundJob.SyncASIN.getCron(),
                        BackgroundJob.SyncASIN.getClazz());
                ProgressLogsPanel.getInstance().displayMsg(
                        "ASIN auto sync job was enabled successfully. Next trigger time will be " + nextTriggerTime);
            }
        }

        systemSettings.setAsinSyncTime(exportTimePicker.getTime());
        systemSettings.setAsinSyncAllowedRange((int) allowedRangeComBox.getSelectedItem());
        systemSettings.save();
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("ASINs Sync Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new SyncASINsSettingPanel());
        frame.setVisible(true);
    }
}
