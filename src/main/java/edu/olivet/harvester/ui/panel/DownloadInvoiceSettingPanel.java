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
public class DownloadInvoiceSettingPanel extends JPanel {
    public DownloadInvoiceSettingPanel() {
        initComponents();
    }

    private JComboBox<String> enableAutoDownloadComboBox;
    private TimePicker exportTimePicker;
    private JComboBox<Integer> allowedRangeComBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel enableAutoExportLabel = new JLabel("Enable Auto Download?");
        enableAutoDownloadComboBox = new JComboBox<>();
        enableAutoDownloadComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if (systemSettings.isEnableInvoiceDownloading()) {
            enableAutoDownloadComboBox.setSelectedItem("Yes");
        } else {
            enableAutoDownloadComboBox.setSelectedItem("No");
        }

        final JLabel autoExportTimeLabel = new JLabel("Daily Download at ");
        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.initialTime = systemSettings.getInvoiceDownloadTime();
        exportTimePicker = new TimePicker(timeSettings);

        final JLabel rangeLabel = new JLabel("before or after");
        allowedRangeComBox = new JComboBox<>();
        allowedRangeComBox.setModel(new DefaultComboBoxModel<>(new Integer[] {0, 5, 10, 15, 20, 25, 30, 60, 90, 120}));
        allowedRangeComBox.setSelectedItem(systemSettings.getInvoiceDownloadingAllowedRange());
        final JLabel unitLabel = new JLabel("minutes");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 120;
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
        boolean oldData = systemSettings.isEnableInvoiceDownloading();
        //noinspection ConstantConditions
        if ("Yes".equalsIgnoreCase(enableAutoDownloadComboBox.getSelectedItem().toString())) {
            systemSettings.setEnableInvoiceDownloading(true);
        } else {
            systemSettings.setEnableInvoiceDownloading(false);
        }

        if (oldData != systemSettings.isEnableInvoiceDownloading()) {
            TaskScheduler taskScheduler = ApplicationContext.getBean(TaskScheduler.class);
            taskScheduler.deleteJob(BackgroundJob.InvoiceDownloading.getClazz());
            if (oldData) {
                ProgressLogsPanel.getInstance().displayMsg("Invoice auto downloading job was disabled successfully.");
            } else {
                Date nextTriggerTime = taskScheduler.startJob(BackgroundJob.InvoiceDownloading.getCron(),
                        BackgroundJob.InvoiceDownloading.getClazz());
                ProgressLogsPanel.getInstance().displayMsg(
                        "Invoice auto downloading job was enabled successfully. Next trigger time will be " + nextTriggerTime);
            }
        }

        systemSettings.setInvoiceDownloadTime(exportTimePicker.getTime());
        systemSettings.setInvoiceDownloadingAllowedRange((int) allowedRangeComBox.getSelectedItem());
        systemSettings.save();
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Download Invoice Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new DownloadInvoiceSettingPanel());
        frame.setVisible(true);
    }
}
