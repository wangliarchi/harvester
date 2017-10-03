package edu.olivet.harvester.ui;

import edu.olivet.foundations.ui.AbstractUIContainer;
import edu.olivet.foundations.ui.Action;
import edu.olivet.foundations.ui.Menu;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:59 AM
 */
public class UIElements extends AbstractUIContainer {
    private static final UIElements instance = new UIElements();

    static UIElements getInstance() {
        return instance;
    }

    private UIElements() {
    }

    private final Menu harvester = new Menu("Harvester", "H");

    @Override
    public Menu[] getMenus() {
        return new Menu[]{
            harvester,
            Menu.Settings,
            Menu.Help
        };
    }

    @Override
    public Map<Menu, Action[]> getMenuActions() {
        Map<Menu, Action[]> map = new HashMap<>();
        map.put(harvester, new Action[]{
            Actions.SubmitOrder,
            Action.Separator,
            Actions.FindSupplier,
            Action.Separator,
            Actions.ConfirmShipment,
            Actions.OrderConfirmationHistory
        });
        map.put(Menu.Settings, new Action[]{
            Action.Settings,
            Action.Separator,
            Action.CreateAutoStartTask,
            Action.DeleteAutoStartTask,
            Action.CreateShortCut,
            Action.Separator,
            Action.Restart
        });
        map.put(Menu.Help, new Action[]{
            Action.CurrentVersion,
            Action.UpgradeCheck
        });
        return map;
    }

    @Override
    public Map<Action, Action[]> getActionRelationships() {
        return new HashMap<>();
    }

    @Override
    public Action[] getToolbarActions() {
        return new Action[]{
            Actions.SubmitOrder,
            Actions.FindSupplier,
            Actions.ConfirmShipment,
            Action.Settings
        };
    }

    @Override
    public Action getAction(String command) {
        return this.getActionByCommand(command, Action.class, Actions.class);
    }

    public JTextArea createLogTextArea(Color foregroundColor) {
        JTextArea logTextArea = new JTextArea();
        logTextArea.setColumns(20);
        logTextArea.setRows(8);
        logTextArea.setLineWrap(true);
        logTextArea.setEditable(false);

        if (foregroundColor != null) {
            logTextArea.setForeground(foregroundColor);
        }

        return logTextArea;

    }

    public JPanel createLogPanel(String panelTitle, int height, JTextArea logTextArea) {
        JPanel logAreaPanel = new JPanel();
        logAreaPanel.setBorder(new TitledBorder(null, panelTitle, TitledBorder.LEADING, TitledBorder.TOP, null, null));

        JScrollPane logAreaScrollPane = new JScrollPane();

        GroupLayout logAreaLayout = new GroupLayout(logAreaPanel);
        logAreaLayout.setHorizontalGroup(
            logAreaLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(logAreaLayout.createSequentialGroup()
                    .addComponent(logAreaScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );
        logAreaLayout.setVerticalGroup(
            logAreaLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, logAreaLayout.createSequentialGroup()
                    .addComponent(logAreaScrollPane, GroupLayout.PREFERRED_SIZE, height, GroupLayout.PREFERRED_SIZE))
        );


        logAreaScrollPane.setViewportView(logTextArea);
        logAreaPanel.setLayout(logAreaLayout);


        return logAreaPanel;
    }


}
