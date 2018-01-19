package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.ui.panel.SellerPanel;
import edu.olivet.harvester.utils.Settings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:27 PM
 */
public class SellerPanelDialog extends BaseDialog {
    private Account sellerAccount;
    private Country country;
    private SellerPanel sellerPanel;
    public MarketWebServiceIdentity marketWebServiceIdentity;

    public SellerPanelDialog(Country country, Account sellerAccount) {
        super(null, true);
        this.sellerAccount = sellerAccount;
        this.country = country;
        initComponents();
    }


    private void initComponents() {
        String title = "Seller Central";
        this.setTitle(title);
        this.initButtons();
        okBtn.setText("Start Fetching Info >>");

        sellerPanel = new SellerPanel(1, country, sellerAccount, 1);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(sellerPanel, 1200, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(okBtn)
                                .addContainerGap()
                        ));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(sellerPanel, 600, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(cancelBtn)
                                        .addComponent(okBtn))
                                .addContainerGap())
        );


        getRootPane().setDefaultButton(okBtn);

        pack();

        sellerPanel.toHomePage();

    }


    @Override
    public void ok() {

        okBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        new Thread(() -> {
            try {
                marketWebServiceIdentity = sellerPanel.fetchMWSInfo();
                ok = true;
                doClose();
            } catch (Exception e) {
                UITools.error("Fail to fetch MWS/Seller ID info - " + e.getMessage());
                okBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
            }
        }).start();


    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        SellerPanelDialog sellerPanelDialog = new SellerPanelDialog(Country.US, Settings.load().getConfigByCountry(Country.US).getSeller());


        UITools.setDialogAttr(sellerPanelDialog);
    }
}
