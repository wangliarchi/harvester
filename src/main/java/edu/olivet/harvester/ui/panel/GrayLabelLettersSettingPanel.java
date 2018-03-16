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
public class GrayLabelLettersSettingPanel extends JPanel {
    public GrayLabelLettersSettingPanel() {
        initComponents();
    }

    private JComboBox<String> enableAutoSendingComboBox;
    private TimePicker sendingTimePicker;
    private JComboBox<Integer> allowedRangeComBox;
    private JComboBox<Integer> maxDaysComBox;
    private JComboBox<String> sendingMethodsComBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();


        final JLabel maxDaysLabel = new JLabel("Days to check");
        maxDaysComBox = new JComboBox<>();
        maxDaysComBox.setModel(new DefaultComboBoxModel<>(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}));
        maxDaysComBox.setSelectedItem(systemSettings.getGrayLabelLetterMaxDays());

        final JLabel sendingMethodLabel = new JLabel("Sending Message Via");
        sendingMethodsComBox = new JComboBox<>();
        sendingMethodsComBox.setModel(new DefaultComboBoxModel<>(new String[] {"Amazon Seller Central", "Email", "Both ASC and Email"}));
        sendingMethodsComBox.setSelectedItem(systemSettings.getGrayLabelLetterSendingMethod());


        final JLabel enableAutoExportLabel = new JLabel("Enable Auto Sending Gray Letters?");
        enableAutoSendingComboBox = new JComboBox<>();
        enableAutoSendingComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if (systemSettings.isEnableAutoSendGrayLabelLetters()) {
            enableAutoSendingComboBox.setSelectedItem("Yes");
        } else {
            enableAutoSendingComboBox.setSelectedItem("No");
        }

        final JLabel sendingTimeLabel = new JLabel("Daily Sending at ");
        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.initialTime = systemSettings.getGrayLabelLetterSendingTime();
        sendingTimePicker = new TimePicker(timeSettings);

        final JLabel rangeLabel = new JLabel("before or after");
        allowedRangeComBox = new JComboBox<>();
        allowedRangeComBox.setModel(new DefaultComboBoxModel<>(new Integer[] {0, 5, 10, 15, 20, 25, 30, 60, 90, 120}));
        allowedRangeComBox.setSelectedItem(systemSettings.getGrayLabelLetterSendingAllowedRange());
        final JLabel unitLabel = new JLabel("minutes");


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 180;
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)

                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(maxDaysLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(maxDaysComBox, 100, 100, 100)
                                .addContainerGap()

                        )

                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sendingMethodLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(sendingMethodsComBox, 200, 200, 200)
                                .addContainerGap()

                        )

                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(enableAutoExportLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(enableAutoSendingComboBox, 100, 100, 100)
                                .addContainerGap()

                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sendingTimeLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(sendingTimePicker, 100, 100, 100)
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
                                        .addComponent(maxDaysLabel)
                                        .addComponent(maxDaysComBox)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(sendingMethodLabel)
                                        .addComponent(sendingMethodsComBox)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(enableAutoExportLabel)
                                        .addComponent(enableAutoSendingComboBox)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(sendingTimeLabel)
                                        .addComponent(sendingTimePicker)
                                        .addComponent(rangeLabel)
                                        .addComponent(allowedRangeComBox)
                                        .addComponent(unitLabel)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }


    public void collectData() {
        SystemSettings systemSettings = SystemSettings.reload();
        boolean oldData = systemSettings.isEnableAutoSendGrayLabelLetters();
        //noinspection ConstantConditions
        if ("Yes".equalsIgnoreCase(enableAutoSendingComboBox.getSelectedItem().toString())) {
            systemSettings.setEnableAutoSendGrayLabelLetters(true);
        } else {
            systemSettings.setEnableAutoSendGrayLabelLetters(false);
        }

        if (oldData != systemSettings.isEnableAutoSendGrayLabelLetters()) {
            TaskScheduler taskScheduler = ApplicationContext.getBean(TaskScheduler.class);
            taskScheduler.deleteJob(BackgroundJob.SendingGrayLabelLetter.getClazz());
            if (oldData) {
                ProgressLogsPanel.getInstance().displayMsg("Auto sending gray label letters job was disabled successfully.");
            } else {
                Date nextTriggerTime = taskScheduler.startJob(BackgroundJob.SendingGrayLabelLetter.getCron(),
                        BackgroundJob.SendingGrayLabelLetter.getClazz());
                ProgressLogsPanel.getInstance().displayMsg(
                        "Auto sending gray label letters job was enabled successfully. Next trigger time will be " + nextTriggerTime);
            }
        }

        systemSettings.setGrayLabelLetterSendingTime(sendingTimePicker.getTime());
        systemSettings.setGrayLabelLetterSendingAllowedRange((int) allowedRangeComBox.getSelectedItem());

        systemSettings.setGrayLabelLetterMaxDays((int) maxDaysComBox.getSelectedItem());
        systemSettings.setGrayLabelLetterSendingMethod((String) sendingMethodsComBox.getSelectedItem());
        systemSettings.save();
    }

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Common Letters Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new GrayLabelLettersSettingPanel());
        frame.setVisible(true);
    }
}
