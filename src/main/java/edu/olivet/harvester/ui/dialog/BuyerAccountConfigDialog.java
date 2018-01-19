package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.ui.panel.BuyerAccountPanel;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 11:51 AM
 */
public class BuyerAccountConfigDialog extends BaseDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyerAccountConfigDialog.class);

    private Settings settings;
    private Map<String, Account> buyerAccounts = new HashMap<>();
    private Set<BuyerAccountPanel> buyerAccountPanels = new HashSet<>();
    private BuyerAccountSettingUtils buyerAccountSettingUtils = BuyerAccountSettingUtils.load();

    public BuyerAccountConfigDialog() {
        super(null, true);
        loadBuyerAccountSettings();
        initComponents();

    }

    private void initComponents() {
        String title = "Configure Buyer Accounts";
        this.setTitle(title);
        this.initButtons();
        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel innerPanel = new JPanel();

        GroupLayout innerLayout = new GroupLayout(innerPanel);
        innerPanel.setLayout(innerLayout);

        innerPanel.setBorder(BorderFactory.createTitledBorder(null, title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP));

        GroupLayout.ParallelGroup hParallelGroup = innerLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.ParallelGroup vParallelGroup = innerLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup vSequentialGroup = innerLayout.createSequentialGroup();

        vSequentialGroup.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);

        JPanel headerPanel = initHeaderPanel();
        hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING, innerLayout.createSequentialGroup().addComponent(headerPanel));

        vSequentialGroup.addGroup(innerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(headerPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addGap(5);


        if (buyerAccountSettingUtils.getAccountSettings().size() > 0) {
            for (BuyerAccountSetting buyerAccountSetting : buyerAccountSettingUtils.getAccountSettings()) {
                BuyerAccountPanel buyerAccountPanel = new BuyerAccountPanel(buyerAccountSetting);
                buyerAccountPanels.add(buyerAccountPanel);
                hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING,
                        innerLayout.createSequentialGroup().addComponent(buyerAccountPanel));
                vSequentialGroup.addGroup(innerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(buyerAccountPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                        .addGap(5);
            }
        } else {
            BuyerAccountPanel buyerAccountPanel = new BuyerAccountPanel(null);
            buyerAccountPanels.add(buyerAccountPanel);
            hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING, innerLayout.createSequentialGroup().addComponent(buyerAccountPanel));

            vSequentialGroup.addGroup(innerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(buyerAccountPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                    .addGap(5);
        }


        innerLayout.setHorizontalGroup(hParallelGroup);
        innerLayout.setVerticalGroup(vParallelGroup.addGroup(vSequentialGroup));


        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        final int buttonHeight = 30;

        //add new Buyer account
        JButton addBuyerAccountButton = new JButton("Add New Buyer Account");
        addBuyerAccountButton.addActionListener(e -> {
            BuyerAccountPanel buyerAccountPanel = new BuyerAccountPanel(null);
            buyerAccountPanels.add(buyerAccountPanel);
            hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING, innerLayout.createSequentialGroup().addComponent(buyerAccountPanel));

            vSequentialGroup.addGroup(innerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(buyerAccountPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                    .addGap(5);
            pack();
        });

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(innerPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(addBuyerAccountButton)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addGap(20).addComponent(aboutBtn))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(innerPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(10)
                                .addComponent(addBuyerAccountButton, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addGap(20)
                                .addContainerGap()

                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(aboutBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(cancelBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(okBtn, buttonHeight, buttonHeight, buttonHeight)))
        );


        getRootPane().setDefaultButton(okBtn);

        pack();

    }

    private JPanel initHeaderPanel() {

        JPanel headerPanel = new JPanel();
        //add header
        final JLabel buyerEmailLabel = new JLabel("Email");
        final JLabel buyerPasswordLabel = new JLabel("Password");
        final JLabel countryLabel = new JLabel("Country");
        final JLabel typeLabel = new JLabel("Type");
        final JLabel primeLabel = new JLabel("Prime Buyer");

        GroupLayout layout = new GroupLayout(headerPanel);
        headerPanel.setLayout(layout);

        final int width = 700;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(buyerEmailLabel, 200, 200, 200)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buyerPasswordLabel, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(countryLabel, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typeLabel, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(primeLabel, 100, 100, 100)
                        ));

        int vGap = 5, height = 30;
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerEmailLabel)
                                        .addComponent(buyerPasswordLabel)
                                        .addComponent(countryLabel)
                                        .addComponent(typeLabel)
                                        .addComponent(primeLabel)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)));

        return headerPanel;
    }

    private void loadBuyerAccountSettings() {
        buyerAccountSettingUtils = BuyerAccountSettingUtils.load();
    }


    @Override
    public void ok() {

        List<BuyerAccountSetting> settings = new ArrayList<>();
        for (BuyerAccountPanel buyerAccountPanel : buyerAccountPanels) {
            BuyerAccountSetting buyerAccountSetting = buyerAccountPanel.collect();
            if (buyerAccountSetting != null && buyerAccountSetting.getBuyerAccount() != null) {
                settings.add(buyerAccountSetting);
            }
        }


        buyerAccountSettingUtils.setAccountSettings(settings);
        buyerAccountSettingUtils.save();


        doClose();
    }

    private String concatErrorMessages(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            String error = errors.get(i);
            String end = i < errors.size() ? (";" + StringUtils.LF) : ".";

            sb.append(i + 1).append(". ").append(error).append(end);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new BuyerAccountConfigDialog(), true);
        System.exit(0);
    }
}
