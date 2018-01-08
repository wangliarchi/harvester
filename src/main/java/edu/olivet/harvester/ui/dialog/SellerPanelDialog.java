package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.ui.panel.SellerPanel;
import edu.olivet.harvester.utils.Settings;

import javax.swing.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:27 PM
 */
public class SellerPanelDialog extends BaseDialog {
    Account sellerAccount;
    Country country;
    public SellerPanel sellerPanel;
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

        sellerPanel = new SellerPanel(1, country, sellerAccount, 1);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(sellerPanel, 1200, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(sellerPanel, 600, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );


        getRootPane().setDefaultButton(okBtn);

        pack();

        WaitTime.Long.execute();
        fetchMWSInfo();

    }

    public void fetchMWSInfo() {
        marketWebServiceIdentity = sellerPanel.fetchMWSInfo();
        ok();
    }

    @Override
    public void ok() {
        ok = true;
        doClose();
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        SellerPanelDialog sellerPanelDialog = new SellerPanelDialog(Country.US, Settings.load().getConfigByCountry(Country.US).getSeller());


        UITools.setDialogAttr(sellerPanelDialog, true);
    }
}
