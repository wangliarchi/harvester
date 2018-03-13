package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTaskHandler;
import edu.olivet.harvester.ui.panel.OrderSubmissionSettingsPanel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/11/17 10:50 AM
 */
public class AddOrderSubmissionTaskDialog extends BaseDialog {
    //private static final Logger LOGGER = LoggerFactory.getLogger(AddOrderSubmissionTaskDialog.class);
    private OrderSubmissionTaskHandler orderSubmissionTaskHandler;

    public AddOrderSubmissionTaskDialog(OrderSubmissionTaskHandler orderSubmissionTaskHandler) {
        super(null, true);
        this.orderSubmissionTaskHandler = orderSubmissionTaskHandler;
        initComponents();
    }

    private OrderSubmissionSettingsPanel orderSubmissionSettingsPanel;

    private void initComponents() {
        String title = "Add order submission task";
        this.setTitle(title);
        this.initButtons();

        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        orderSubmissionSettingsPanel = new OrderSubmissionSettingsPanel(this);

        GroupLayout layout = new GroupLayout(getContentPane());

        layout.setHorizontalGroup(layout
                .createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addComponent(orderSubmissionSettingsPanel, 580, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addContainerGap()
                ));


        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup()
                        .addComponent(orderSubmissionSettingsPanel, 100, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(cancelBtn).addComponent(okBtn))
                        .addContainerGap()
        ));

        getContentPane().setLayout(layout);
        getRootPane().setDefaultButton(okBtn);
        pack();

    }

    @Getter
    List<OrderSubmissionTask> orderSubmissionTasks = new ArrayList<>();

    @Override
    public void ok() {
        try {
            orderSubmissionTasks = orderSubmissionSettingsPanel.collectData();
            ok = true;
            okBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            new Thread(() -> {
                if (orderSubmissionTaskHandler != null) {
                    orderSubmissionTaskHandler.saveTasks(orderSubmissionTasks);
                }

                doClose();
            }).start();

        } catch (Exception e) {
            UITools.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new AddOrderSubmissionTaskDialog(null), true);
        System.exit(0);

    }


}
