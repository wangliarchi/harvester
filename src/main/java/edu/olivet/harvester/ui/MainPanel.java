package edu.olivet.harvester.ui;

import edu.olivet.foundations.ui.UITools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 5:51 PM
 */
public class MainPanel extends JPanel {
    public MainPanel() {
        initComponents();
        initEventListeners();
    }

    private void initComponents() {

        runtimeSettingsPanel = RuntimeSettingsPanel.getInstance();
        mainWindowPanel = TabbedBuyerPanel.getInstance();
        progressLogsPanel = ProgressLogsPanel.getInstance();
        progressLogsPanel.setMinimumSize(new Dimension(100, 150));

        jSplitPane1 = new JSplitPane();
        jSplitPane2 = new JSplitPane();

        jSplitPane1.setDividerLocation(400);
        jSplitPane1.setDividerSize(5);
        jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setBorder(null);
        jSplitPane2.setDividerSize(5);
        jSplitPane2.setBorder(null);


        jSplitPane1.setTopComponent(jSplitPane2);
        jSplitPane1.setBottomComponent(progressLogsPanel);
        jSplitPane2.setLeftComponent(mainWindowPanel);
        jSplitPane2.setRightComponent(runtimeSettingsPanel);


        javax.swing.GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );


        mainWindowPanel.addFirstBuyerAccountTab();
        mainWindowPanel.getSelectedBuyerPanel().toWelcomePage();
        //mainWindowPanel.resetZoomLevel();
    }

    public void initEventListeners() {
        this.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                int height = Math.max(runtimeSettingsPanel.getPreferredSize().height + 50, getHeight() - 250);
                height = Math.min(height, getHeight() - 100);
                jSplitPane1.setDividerLocation(height);
                jSplitPane2.setDividerLocation(getWidth() - runtimeSettingsPanel.getPreferredSize().width - 20);
                mainWindowPanel.resetZoomLevel();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
    }

    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private RuntimeSettingsPanel runtimeSettingsPanel;
    public TabbedBuyerPanel mainWindowPanel;
    private ProgressLogsPanel progressLogsPanel;

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setSize(new Dimension(1200, 700));
        frame.setTitle("Seller Panel Demo");
        frame.setVisible(true);

        MainPanel mainPanel = new MainPanel();
        frame.getContentPane().add(mainPanel);

        UITools.setDialogAttr(frame, true);


    }


}
