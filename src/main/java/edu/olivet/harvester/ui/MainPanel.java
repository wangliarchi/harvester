package edu.olivet.harvester.ui;

import com.google.inject.Singleton;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.ui.panel.ProgressLogsPanel;
import edu.olivet.harvester.ui.panel.RightTabPanel;
import edu.olivet.harvester.ui.panel.TabbedBuyerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 5:51 PM
 */
@Singleton
public class MainPanel extends JPanel {
    private static final MainPanel instance = new MainPanel();

    public static MainPanel getInstance() {
        return instance;
    }

    private MainPanel() {
        initComponents();
        initEventListeners();
    }

    private void initComponents() {

        rightTabPanel = RightTabPanel.getInstance();
        mainWindowPanel = TabbedBuyerPanel.getInstance();

        verticalSplitPane1 = new JSplitPane();
        horizontalSplitPane1 = new JSplitPane();

        verticalSplitPane1.setDividerSize(5);
        verticalSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane1.setBorder(null);

        horizontalSplitPane1.setDividerSize(5);
        horizontalSplitPane1.setBorder(null);

        ProgressLogsPanel progressLogsPanel = ProgressLogsPanel.getInstance();
        verticalSplitPane1.setTopComponent(horizontalSplitPane1);
        verticalSplitPane1.setBottomComponent(progressLogsPanel);
        horizontalSplitPane1.setLeftComponent(mainWindowPanel);
        horizontalSplitPane1.setRightComponent(rightTabPanel);

        resetSplitPanelSizes();


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(verticalSplitPane1, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(verticalSplitPane1, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );


        mainWindowPanel.addFirstBuyerAccountTab();
        mainWindowPanel.getSelectedBuyerPanel().toWelcomePage();

    }

    public void resetSplitPanelSizes() {
        int height = Math.max(rightTabPanel.getPreferredSize().height + 50, getHeight() - 200);
        verticalSplitPane1.setDividerLocation(height);
        horizontalSplitPane1.setDividerLocation(getWidth() - rightTabPanel.getPreferredSize().width - 20);
    }

    private void initEventListeners() {
        this.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                resetSplitPanelSizes();
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

    private JSplitPane verticalSplitPane1;
    private JSplitPane horizontalSplitPane1;
    private RightTabPanel rightTabPanel;
    private TabbedBuyerPanel mainWindowPanel;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(new Dimension(1200, 700));
        frame.setTitle("Seller Panel Demo");
        frame.setVisible(true);

        MainPanel mainPanel = new MainPanel();
        frame.getContentPane().add(mainPanel);

        UITools.setDialogAttr(frame, true);


    }


}
