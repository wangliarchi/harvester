package edu.olivet.harvester.ui;

import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.model.CreditCard;
import lombok.Getter;

import javax.swing.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 12:08 PM
 */
public class BankCardPanel extends JPanel {
    @Getter
    private final String accountEmail;

    public BankCardPanel(String accountEmail) {
        this.accountEmail = accountEmail;
        this.initComponents();
    }

    private void initComponents() {
        final JLabel buyerAccountLabel = new JLabel("Buyer Account:");
        final JLabel bankCardNoLabel = new JLabel("Bank Card:");
        final JLabel cvvLabel = new JLabel("CVV:");

        buyerAccountField.setText(accountEmail);
        buyerAccountField.setEnabled(false);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        final int width = 700;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(buyerAccountLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(buyerAccountField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                .addGap(22, 22, 22)
                                .addComponent(bankCardNoLabel)
                                .addGap(8, 8, 8)
                                .addComponent(bankCardNoField, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                .addGap(22, 22, 22)
                                .addComponent(cvvLabel)
                                .addGap(8, 8, 8)
                                .addComponent(cvvField, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                        ));

        int vGap = 5, height = 30;
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerAccountLabel)
                                        .addComponent(buyerAccountField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(bankCardNoLabel)
                                        .addComponent(bankCardNoField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cvvLabel)
                                        .addComponent(cvvField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)));
        UITools.addListener2Textfields(this);


    }

    private JTextField buyerAccountField = new JTextField();
    private JTextField bankCardNoField = new JTextField();
    private JTextField cvvField = new JTextField();

    public CreditCard collect() {
        return new CreditCard(buyerAccountField.getText(), bankCardNoField.getText(), cvvField.getText());
    }

    public void load(CreditCard creditCard) {
        bankCardNoField.setText(creditCard.getCardNo());
        cvvField.setText(creditCard.getCvv());
    }

    public static void main(String[] args) {

        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Bank Card Configuration");
        frame.setSize(700, 180);
        frame.getContentPane().add(new BankCardPanel("reviewsstudy@gmail.com"));
        frame.setVisible(true);
    }
}
