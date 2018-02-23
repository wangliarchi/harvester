package edu.olivet.harvester.ui.panel;

import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.common.model.SystemSettings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:31 PM
 */
public class OrderExportSettingPanel extends JPanel {
    public OrderExportSettingPanel() {
        initComponents();
    }

    private JComboBox<String> enableAutoExportComboBox;
    private TimePicker exportTimePicker;
    private JComboBox<Integer> allowedRangeComBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel enableAutoExportLabel = new JLabel("Enable Auto Export?");
        enableAutoExportComboBox = new JComboBox<>();
        enableAutoExportComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if (systemSettings.isEnableOrderExport()) {
            enableAutoExportComboBox.setSelectedItem("Yes");
        } else {
            enableAutoExportComboBox.setSelectedItem("No");
        }

        final JLabel autoExportTimeLabel = new JLabel("Daily Export at ");
        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.initialTime = systemSettings.getOrderExportTime();
        exportTimePicker = new TimePicker(timeSettings);

        final JLabel rangeLabel = new JLabel("before or after");
        allowedRangeComBox = new JComboBox<>();
        allowedRangeComBox.setModel(new DefaultComboBoxModel<>(new Integer[] {0, 5, 10, 15, 20, 25, 30, 60, 90, 120}));
        allowedRangeComBox.setSelectedItem(systemSettings.getOrderExportAllowedRange());
        final JLabel unitLabel = new JLabel("minutes");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 120;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(enableAutoExportLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(enableAutoExportComboBox,100,100,100)
                                .addContainerGap()

                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(autoExportTimeLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(exportTimePicker)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(rangeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(allowedRangeComBox)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(unitLabel)
                                .addContainerGap()
                        ));


        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(enableAutoExportLabel)
                                        .addComponent(enableAutoExportComboBox)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
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
        SystemSettings systemSettings = SystemSettings.load();
        boolean oldData = systemSettings.isEnableOrderExport();
        //noinspection ConstantConditions
        if ("Yes".equalsIgnoreCase(enableAutoExportComboBox.getSelectedItem().toString())) {
            systemSettings.setEnableOrderExport(true);
        } else {
            systemSettings.setEnableOrderExport(false);
        }

        if (oldData != systemSettings.isEnableOrderExport()) {
            TaskScheduler taskScheduler = ApplicationContext.getBean(TaskScheduler.class);
            taskScheduler.deleteJob(BackgroundJob.OrderExporting.getClazz());
            if (oldData) {
                ProgressLogsPanel.getInstance().displayMsg("Order auto exporting job was disabled successfully.");
            } else {
                Date nextTriggerTime = taskScheduler.startJob(BackgroundJob.OrderExporting.getCron(),
                        BackgroundJob.OrderExporting.getClazz());
                ProgressLogsPanel.getInstance().displayMsg(
                        "Order auto exporting job was enabled successfully. Next trigger time will be " + nextTriggerTime);
            }
        }

        systemSettings.setOrderExportTime(exportTimePicker.getTime());
        systemSettings.setOrderExportAllowedRange((int) allowedRangeComBox.getSelectedItem());
        systemSettings.save();
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Order Exporting Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new OrderExportSettingPanel());
        frame.setVisible(true);
    }
}
