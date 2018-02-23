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
public class OrderSubmissionSettingPanel extends JPanel {
    public OrderSubmissionSettingPanel() {
        initComponents();
    }

    private JComboBox<Integer> maxThreadsJCombox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel maxThreadLabel= new JLabel("Maximum Running Tab?");

        maxThreadsJCombox = new JComboBox<>();
        maxThreadsJCombox.setModel(new DefaultComboBoxModel<>(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8}));
        maxThreadsJCombox.setSelectedItem(systemSettings.getMaxOrderProcessingThread());


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 150;
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(maxThreadLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(maxThreadsJCombox,100,100,100)
                                .addContainerGap()

                        ));


        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(maxThreadLabel)
                                        .addComponent(maxThreadsJCombox)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }


    public void collectData() {
        SystemSettings systemSettings = SystemSettings.load();

        systemSettings.setMaxOrderProcessingThread((int) maxThreadsJCombox.getSelectedItem());
        systemSettings.save();
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("ASINs Sync Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new OrderSubmissionSettingPanel());
        frame.setVisible(true);
    }
}
