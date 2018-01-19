package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.service.RuntimePanelObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 7:32 PM
 */
public class ProgressBarPanel extends JPanel implements RuntimePanelObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressBarPanel.class);

    private RuntimeSettings settings;

    private static ProgressBarPanel instance;

    public static ProgressBarPanel getInstance() {
        if (instance == null) {
            instance = new ProgressBarPanel();
        }

        return instance;
    }

    private ProgressBarPanel() {
        initComponents();
    }


    private JProgressBar progressBar;
    private JLabel progressTextLabel;


    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("")));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        JLabel progressLabel = new JLabel();
        progressLabel.setText("Progress");

        progressTextLabel = new JLabel();
        progressTextLabel.setText("No tasks running yet.");
        progressTextLabel.setForeground(Color.BLUE);
        Font font = progressTextLabel.getFont();
        progressTextLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 2));


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        int fieldWidth = 150;
        int labelMinWidth = 55;
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(layout.createSequentialGroup()
                            .addComponent(progressLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(progressBar, labelMinWidth, fieldWidth * 2, Short.MAX_VALUE)
                        )
                        .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addGap(labelMinWidth)
                            .addComponent(progressTextLabel)
                            .addContainerGap()
                        )


                    )
                    .addContainerGap()
                )
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(12, 12, 12)

                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(progressLabel)
                        .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addComponent(progressTextLabel)
                    .addContainerGap()
                )
        );
    }


    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Runtime Settings");
        frame.setSize(400, 580);
        ProgressBarPanel runtimeSettingsPanel = ProgressBarPanel.getInstance();
        frame.getContentPane().add(runtimeSettingsPanel);
        frame.setVisible(true);
        ProgressUpdater.success();
    }

    @Override
    public void updateSpending(String spending) {

    }

    @Override
    public void updateBudget(String budget) {

    }
}
