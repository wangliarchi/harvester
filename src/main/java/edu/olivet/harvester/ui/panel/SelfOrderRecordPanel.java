package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/2/2018 7:00 AM
 */
public class SelfOrderRecordPanel extends JPanel {

    @Getter
    @Setter
    private SelfOrder selfOrder;

    public SelfOrderRecordPanel(@NotNull SelfOrder selfOrder) {
        this.selfOrder = selfOrder;
        this.initComponents();
    }


    public SelfOrder collectData() {
        selfOrder.setBuyerAccountEmail((String) buyerAccountJCombox.getSelectedItem());
        return selfOrder;
    }

    private void initComponents() {
        JTextField accountField = new JTextField(selfOrder.ownerAccountCode);
        JTextField accountNameField = new JTextField(selfOrder.ownerAccountStoreName);
        JTextField accountCountryField = new JTextField(selfOrder.country);
        JTextField asinField = new JTextField(selfOrder.asin);
        JTextField sheetNameField = new JTextField(selfOrder.sheetName);
        JTextField rowField = new JTextField(String.valueOf(selfOrder.row));

        accountField.setEnabled(false);
        accountCountryField.setEnabled(false);
        accountNameField.setEnabled(false);
        asinField.setEnabled(false);
        sheetNameField.setEnabled(false);
        rowField.setEnabled(false);

        Country country = Country.fromCode(selfOrder.country);
        Country settingCountry = country.europe() ? Country.UK : country;
        buyerAccountJCombox = new JComboBox<>();

        List<Account> buyers = BuyerAccountSettingUtils.load().getAccounts(settingCountry);
        List<String> emails = buyers.stream().map(Account::getEmail).collect(Collectors.toList());
        emails.add(0, "");
        String[] buyerEmails = emails.toArray(new String[emails.size()]);
        buyerAccountJCombox.setModel(new DefaultComboBoxModel<>(buyerEmails));

        try {
            String accountEmail = selfOrder.buyerAccountEmail;
            if (StringUtils.isBlank(accountEmail) || !emails.contains(accountEmail)) {
                Account buyer = Settings.load().getConfigByCountry(settingCountry).getBuyer();
                if (buyer != null && StringUtils.isNotBlank(buyer.getEmail())) {
                    accountEmail = buyer.getEmail();
                } else {
                    accountEmail = Settings.load().getConfigByCountry(settingCountry).getPrimeBuyer().getEmail();
                }
            }

            buyerAccountJCombox.setSelectedItem(accountEmail);
        } catch (Exception e) {
            //
        }

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sheetNameField, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rowField, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(accountField, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(accountNameField, 120, 120, 120)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(accountCountryField, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(asinField, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buyerAccountJCombox, 200, 200, 200)
                                .addContainerGap()
                        ));


        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sheetNameField)
                                        .addComponent(rowField)
                                        .addComponent(accountField)
                                        .addComponent(accountNameField)
                                        .addComponent(accountCountryField)
                                        .addComponent(asinField)
                                        .addComponent(buyerAccountJCombox)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)));
        UITools.addListener2Textfields(this);
    }

    private JComboBox<String> buyerAccountJCombox;

    public static void main(String[] args) {
        SelfOrder selfOrder = new SelfOrder();
        selfOrder.ownerAccountCode = "22US";
        selfOrder.ownerAccountStoreName = "Onlinebohop";
        selfOrder.country = "US";
        selfOrder.asin = "B01K15AN3W";
        selfOrder.buyerAccountCode = "704";

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("");
        frame.setSize(700, 180);
        frame.getContentPane().add(new SelfOrderRecordPanel(selfOrder));
        frame.setVisible(true);
    }

}
