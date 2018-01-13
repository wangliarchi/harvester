package edu.olivet.harvester.ui.panel;

import com.google.common.collect.Lists;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.model.BuyerAccountSetting;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 12:08 PM
 */
public class BuyerAccountPanel extends JPanel {

    @Getter
    @Setter
    private BuyerAccountSetting buyerAccountSetting;

    public BuyerAccountPanel(BuyerAccountSetting buyerAccountSetting) {
        this.initComponents();
        this.buyerAccountSetting = buyerAccountSetting;

        if (buyerAccountSetting != null) {
            initData();
        }


    }


    private void initData() {
        buyerEmailField.setText(buyerAccountSetting.getBuyerAccount().getEmail());
        buyerPasswordField.setText(buyerAccountSetting.getBuyerAccount().getPassword());
        typeCombox.setSelectedItem(buyerAccountSetting.getType());
        primeCombox.setSelectedItem(buyerAccountSetting.getPrimeBuyer());
        if (buyerAccountSetting.getCountries().size() == 1) {
            countryCombox.setSelectedItem(buyerAccountSetting.getCountries().get(0).name());
        } else {
            countryCombox.setSelectedItem("All");
        }
    }

    private void initComponents() {


        List<String> countries = Settings.load().listAllCountries().stream().map(it -> it.name()).collect(Collectors.toList());
        countries.add(0, "All");
        countryCombox = new JComboBox<>();
        countryCombox.setModel(new DefaultComboBoxModel<>(countries.toArray(new String[countries.size()])));

        typeCombox = new JComboBox<>();
        typeCombox.setModel(new DefaultComboBoxModel<>(new String[]{"Both", "Book", "Product"}));

        primeCombox = new JComboBox<>();
        primeCombox.setModel(new DefaultComboBoxModel<>(new String[]{"Both", "Prime", "Non-Prime"}));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        final int width = 700;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()

                                .addComponent(buyerEmailField, 200, 200, 200)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buyerPasswordField, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addComponent(countryCombox, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addComponent(typeCombox, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addComponent(primeCombox, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        ));

        int vGap = 5, height = 30;
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)

                                        .addComponent(buyerEmailField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                        .addComponent(buyerPasswordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                        .addComponent(countryCombox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                        .addComponent(typeCombox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                        .addComponent(primeCombox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)));
        UITools.addListener2Textfields(this);


    }

    public BuyerAccountSetting collect() {
        BuyerAccountSetting buyerAccountSetting = new BuyerAccountSetting();
        if (StringUtils.isNotBlank(buyerEmailField.getText()) && StringUtils.isNotBlank(buyerPasswordField.getText())) {
            Account account = new Account(buyerEmailField.getText(), buyerPasswordField.getText(), Account.AccountType.Buyer);
            buyerAccountSetting.setBuyerAccount(account);
            buyerAccountSetting.setType((String) typeCombox.getSelectedItem());
            buyerAccountSetting.setPrimeBuyer((String) primeCombox.getSelectedItem());
            if ("All".equalsIgnoreCase((String) countryCombox.getSelectedItem())) {
                buyerAccountSetting.setCountries(Settings.load().listAllCountries());
            } else {
                buyerAccountSetting.setCountries(Lists.newArrayList(Country.valueOf((String) countryCombox.getSelectedItem())));
            }
        }
        return buyerAccountSetting;
    }

    private JTextField buyerEmailField = new JTextField();
    private JTextField buyerPasswordField = new JTextField();
    private JComboBox<String> countryCombox;
    private JComboBox<String> typeCombox;
    private JComboBox<String> primeCombox;
    private JButton editButton;
    private JButton deleteButton;


    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Buyer Account Configuration");
        frame.setSize(700, 180);
        frame.getContentPane().add(new BuyerAccountPanel(null));
        frame.setVisible(true);
    }
}
