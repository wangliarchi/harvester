package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.CreditCard;
import edu.olivet.harvester.ui.panel.BankCardPanel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 11:51 AM
 */
public class BankCardConfigDialog extends BaseDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankCardConfigDialog.class);
    private static final long serialVersionUID = 4799410915878809682L;

    private Map<String, CreditCard> creditCards = new HashMap<>();
    private Set<String> buyerAccountEmails;
    private Set<BankCardPanel> bankCardPanels = new HashSet<>();

    public BankCardConfigDialog() {
        super(null, true);
        buyerAccountEmails = loadBuyerAccountEmails();
        loadCreditCards();
        initComponents();

    }

    private void initComponents() {
        String title = "Configure Bank Card Information";
        this.setTitle(title);
        this.initButtons();
        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel innerPanel = new JPanel();

        GroupLayout innerLayout = new GroupLayout(innerPanel);
        innerPanel.setLayout(innerLayout);

        innerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
                null, title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP));

        GroupLayout.ParallelGroup hParallelGroup = innerLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.ParallelGroup vParallelGroup = innerLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup vSequentialGroup = innerLayout.createSequentialGroup();


        vSequentialGroup.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        List<String> emails = new ArrayList<>(creditCards.keySet());
        for (String buyerEmail : buyerAccountEmails) {
            if (!emails.contains(buyerEmail.toLowerCase())) {
                emails.add(buyerEmail.toLowerCase());
            }
        }


        emails.forEach(buyerEmail -> {
            BankCardPanel bankCardPanel = new BankCardPanel(buyerEmail);
            if (creditCards.containsKey(buyerEmail)) {
                bankCardPanel.load(creditCards.get(buyerEmail));
            }
            bankCardPanels.add(bankCardPanel);
            hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING, innerLayout.createSequentialGroup().addComponent(bankCardPanel));

            vSequentialGroup.addGroup(innerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bankCardPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                    .addGap(5);


        });
        innerLayout.setHorizontalGroup(hParallelGroup);
        innerLayout.setVerticalGroup(vParallelGroup.addGroup(vSequentialGroup));


        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        final int buttonHeight = 30;


        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(innerPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
                                .addGap(5)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(aboutBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(cancelBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(okBtn, buttonHeight, buttonHeight, buttonHeight)))
        );


        getRootPane().setDefaultButton(okBtn);

        pack();

    }

    private void loadCreditCards() {
        creditCards = CreditCardUtils.loadCreditCards();
    }

    private List<BuyerAccountSetting> buyerAccountSettings;

    private Set<String> loadBuyerAccountEmails() {
        Set<String> buyerAccounts = new HashSet<>();
        buyerAccountSettings = BuyerAccountSettingUtils.load().getAccountSettings();
        if (buyerAccountSettings != null && CollectionUtils.isNotEmpty(buyerAccountSettings)) {
            buyerAccounts = buyerAccountSettings.stream().map(it -> it.getBuyerAccount().getEmail()).collect(Collectors.toSet());
        }

        return buyerAccounts;
    }


    @Override
    public void ok() {
        List<CreditCard> creditCards = new ArrayList<>(bankCardPanels.size());
        for (BankCardPanel bankCardPanel : bankCardPanels) {
            CreditCard card = bankCardPanel.collect();
            List<String> errors = card.validate();
            if (CollectionUtils.isEmpty(errors)) {
                creditCards.add(card);
            } else {
                UITools.error(String.format("Please fix %d credit card error(s) for account %s:%n%n%s",
                        errors.size(), card.getAccountEmail(), concatErrorMessages(errors)));
                return;
            }
        }
        creditCards.forEach(creditCard -> this.creditCards.put(creditCard.getAccountEmail(), creditCard));

        CreditCardUtils.saveToFile(creditCards);

        ok = true;
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
        UITools.setDialogAttr(new BankCardConfigDialog(), true);
        System.exit(0);
    }
}
