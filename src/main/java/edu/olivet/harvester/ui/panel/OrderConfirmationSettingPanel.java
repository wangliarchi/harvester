package edu.olivet.harvester.ui.panel;

import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.model.SystemSettings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:31 PM
 */
public class OrderConfirmationSettingPanel extends JPanel {
    public OrderConfirmationSettingPanel() {
        initComponents();
    }

    private JComboBox<String> enableAutoConfirmationComboBox;
    private TimePicker confirmationTimePicker;
    private JComboBox<Integer> allowedRangeComBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel enableAutoExportLabel = new JLabel("Enable Auto Confirmation?");
        enableAutoConfirmationComboBox = new JComboBox<>();
        enableAutoConfirmationComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if (systemSettings.isEnableOrderConfirmation()) {
            enableAutoConfirmationComboBox.setSelectedItem("Yes");
        } else {
            enableAutoConfirmationComboBox.setSelectedItem("No");
        }

        final JLabel autoExportTimeLabel = new JLabel("Daily Confirm at ");
        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.initialTime = systemSettings.getOrderConfirmationTime();
        confirmationTimePicker = new TimePicker(timeSettings);

        final JLabel rangeLabel = new JLabel("before or after");
        allowedRangeComBox = new JComboBox<>();
        allowedRangeComBox.setModel(new DefaultComboBoxModel<>(new Integer[] {0, 5, 10, 15, 20, 25, 30, 60, 90, 120}));
        allowedRangeComBox.setSelectedItem(systemSettings.getOrderConfirmationAllowedRange());
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
                                .addComponent(enableAutoConfirmationComboBox)
                                .addContainerGap()

                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(autoExportTimeLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(confirmationTimePicker)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(rangeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(allowedRangeComBox)
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
                                        .addComponent(enableAutoConfirmationComboBox)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(autoExportTimeLabel)
                                        .addComponent(confirmationTimePicker)
                                        .addComponent(rangeLabel)
                                        .addComponent(allowedRangeComBox)
                                        .addComponent(unitLabel)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }


    public void collectData() {
        SystemSettings systemSettings = SystemSettings.load();
        boolean oldData = systemSettings.isEnableOrderConfirmation();
        //noinspection ConstantConditions
        if ("Yes".equalsIgnoreCase(enableAutoConfirmationComboBox.getSelectedItem().toString())) {
            systemSettings.setEnableOrderConfirmation(true);
        } else {
            systemSettings.setEnableOrderConfirmation(false);
        }

        if (oldData != systemSettings.isEnableOrderConfirmation()) {
            TaskScheduler taskScheduler = ApplicationContext.getBean(TaskScheduler.class);
            taskScheduler.deleteJob(BackgroundJob.ShipmentConfirmation.getClazz());
            if (oldData) {
                ProgressLogsPanel.getInstance().displayMsg("Order auto confirmation job was disabled successfully.");
            } else {
                Date nextTriggerTime = taskScheduler.startJob(BackgroundJob.ShipmentConfirmation.getCron(),
                        BackgroundJob.ShipmentConfirmation.getClazz());
                ProgressLogsPanel.getInstance().displayMsg(
                        "Order auto confirmation job was enabled successfully. Next trigger time will be " + nextTriggerTime);
            }
        }


        systemSettings.setOrderConfirmationTime(confirmationTimePicker.getTime());
        systemSettings.setOrderConfirmationAllowedRange((int) allowedRangeComBox.getSelectedItem());
        systemSettings.save();
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Order Confirmation Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new OrderConfirmationSettingPanel());
        frame.setVisible(true);
    }
}
