package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.SystemSettings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:31 PM
 */
public class OrderSubmissionSettingPanel extends JPanel {
    public OrderSubmissionSettingPanel() {
        initComponents();
    }

    private JComboBox<Integer> maxThreadsJCombox;
    private JComboBox<String> debugModeComboBox;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();
        final JLabel maxThreadLabel = new JLabel("Maximum Running Tab?");

        maxThreadsJCombox = new JComboBox<>();
        maxThreadsJCombox.setModel(new DefaultComboBoxModel<>(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8}));
        maxThreadsJCombox.setSelectedItem(systemSettings.getMaxOrderProcessingThread());


        final JLabel debugModeLabel = new JLabel("Debug Mode?");
        debugModeComboBox = new JComboBox<>();
        debugModeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"No", "Yes"}));
        if (systemSettings.isOrderSubmissionDebugModel()) {
            debugModeComboBox.setSelectedItem("Yes");
        } else {
            debugModeComboBox.setSelectedItem("No");
        }

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 150;
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(maxThreadLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(maxThreadsJCombox, 100, 100, 100)
                                .addContainerGap()

                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(debugModeLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(debugModeComboBox, 100, 100, 100)
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
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(debugModeLabel)
                                        .addComponent(debugModeComboBox)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }


    @SuppressWarnings("ConstantConditions")
    public void collectData() {
        SystemSettings systemSettings = SystemSettings.reload();
        systemSettings.setMaxOrderProcessingThread((int) maxThreadsJCombox.getSelectedItem());

        if ("Yes".equalsIgnoreCase(debugModeComboBox.getSelectedItem().toString())) {
            systemSettings.setOrderSubmissionDebugModel(true);
        } else {
            systemSettings.setOrderSubmissionDebugModel(false);
        }

        systemSettings.save();
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("");
        frame.setSize(500, 180);
        frame.getContentPane().add(new OrderSubmissionSettingPanel());
        frame.setVisible(true);
    }
}
