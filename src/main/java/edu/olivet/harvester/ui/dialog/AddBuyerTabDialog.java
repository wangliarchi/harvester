package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import lombok.Getter;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/10/2018 10:44 AM
 */
public class AddBuyerTabDialog extends BaseDialog {


    public AddBuyerTabDialog() {
        super(null, true);

        initComponents();
    }

    private void initComponents() {
        String title = "Configure Bank Card Information";
        this.setTitle(title);
        this.initButtons();
        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));


        BuyerAccountSettingUtils buyerAccountSettingUtils = BuyerAccountSettingUtils.load();
        List<Account> buyers = buyerAccountSettingUtils.getAccountSettings().stream().map(it -> it.getBuyerAccount()).collect(Collectors.toList());

        JLabel buyerLabel = new JLabel("Buyer Account");
        buyerJComboBox = new JComboBox<>();
        buyerJComboBox.setModel(new DefaultComboBoxModel<>(buyers.toArray(new Account[buyers.size()])));

        JLabel countryLabel = new JLabel("Country");
        countryJComboBox = new JComboBox<>();
        countryJComboBox.setModel(new DefaultComboBoxModel<>(Country.values()));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);


        int labelWidth = 120;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(buyerLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buyerJComboBox)
                                .addContainerGap()
                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(countryLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(countryJComboBox)
                                .addContainerGap()
                        )
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
                                .addContainerGap()
                                //.addComponent(innerPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerLabel)
                                        .addComponent(buyerJComboBox))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(countryLabel)
                                        .addComponent(countryJComboBox))
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(aboutBtn)
                                        .addComponent(cancelBtn)
                                        .addComponent(okBtn))
                                .addContainerGap())
        );


        getRootPane().setDefaultButton(okBtn);

        pack();
    }

    private JComboBox<Account> buyerJComboBox;
    private JComboBox<Country> countryJComboBox;

    @Getter
    private Account selectedAccount;

    @Getter
    private Country selectedCountry;

    @Override
    public void ok() {
        selectedAccount = (Account) buyerJComboBox.getSelectedItem();
        selectedCountry = (Country) countryJComboBox.getSelectedItem();
        ok = true;
        doClose();
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        AddBuyerTabDialog addBuyerTabDialog = new AddBuyerTabDialog();

        UITools.setDialogAttr(addBuyerTabDialog);
    }
}
