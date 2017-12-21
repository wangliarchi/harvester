package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.ui.panel.OrderConfirmationSettingPanel;
import edu.olivet.harvester.ui.panel.OrderExportSettingPanel;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:27 PM
 */
public class SystemSettingsDialog  extends BaseDialog {

    public SystemSettingsDialog() {
        super(null, true);

        initComponents();
    }



    private void initComponents() {
        String title = "System Settings";
        this.setTitle(title);
        this.initButtons();
        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JTabbedPane tabbedPane = new JTabbedPane();
        orderExportSettingPanel = new OrderExportSettingPanel();
        tabbedPane.addTab("Order Export",orderExportSettingPanel);
        orderConfirmationSettingPanel = new OrderConfirmationSettingPanel();
        tabbedPane.addTab("Order Confirmation",orderConfirmationSettingPanel);



        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        final int buttonHeight = 30;


        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(tabbedPane, 400, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addGap(20).addComponent(aboutBtn))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(tabbedPane, 300, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(aboutBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(cancelBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(okBtn, buttonHeight, buttonHeight, buttonHeight))
                                .addContainerGap())
        );


        getRootPane().setDefaultButton(okBtn);

        pack();

    }

    OrderExportSettingPanel orderExportSettingPanel;
    OrderConfirmationSettingPanel orderConfirmationSettingPanel;
    @Override
    public void ok() {
        orderExportSettingPanel.collectData();
        orderConfirmationSettingPanel.collectData();

        UITools.info("System settings have been saved successfully.");
        ok = true;
        doClose();
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new SystemSettingsDialog(), true);
        System.exit(0);
    }
}
