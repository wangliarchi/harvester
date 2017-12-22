package edu.olivet.harvester.ui.panel;

import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.model.SystemSettings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.time.LocalTime;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:31 PM
 */
public class OrderExportSettingPanel extends JPanel {
    public OrderExportSettingPanel() {
        initComponents();
    }

    JComboBox<String> enableAutoExportComboBox;
    TimePicker exportTimePicker;
    JComboBox<Integer> allowedRangeComBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel enableAutoExportLabel = new JLabel("Enable Auto Export?");
        enableAutoExportComboBox = new JComboBox();
        enableAutoExportComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if(systemSettings.isEnableOrderExport()) {
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
                                .addComponent(enableAutoExportComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()

                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(autoExportTimeLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(exportTimePicker, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(rangeLabel)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(allowedRangeComBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
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
                                        .addComponent(enableAutoExportComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(autoExportTimeLabel)
                                        .addComponent(exportTimePicker, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(rangeLabel)
                                        .addComponent(allowedRangeComBox)
                                        .addComponent(unitLabel)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }


    public void collectData() {
        SystemSettings systemSettings = SystemSettings.load();
        if("Yes".equalsIgnoreCase(enableAutoExportComboBox.getSelectedItem().toString())) {
            systemSettings.setEnableOrderExport(true);
        } else {
            systemSettings.setEnableOrderExport(false);
        }

        systemSettings.setOrderExportTime(exportTimePicker.getTime());
        systemSettings.setOrderExportAllowedRange((int)allowedRangeComBox.getSelectedItem());
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