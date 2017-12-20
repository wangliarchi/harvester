package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Account.AccountType;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.FinderCodeUtils;
import edu.olivet.harvester.utils.Settings.Configuration;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

/**
 * Configuration panel for single marketplace
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 9:51 PM
 */
public class ConfigurationPanel extends JPanel {

    @Getter
    private final Country country;

    public ConfigurationPanel(Country country) {
        this.country = country;
        this.initComponents();
    }

    private void initComponents() {
        final JLabel sellerLbl = new JLabel("Seller Account:");
        final JLabel sellerEmailLbl = new JLabel("Seller Email:");
        final JLabel storeNameLbl = new JLabel("Store Name:");
        final JLabel signatureLbl = new JLabel("Email Signature:");

        final JLabel sellerIdLbl = new JLabel("Seller Id:");
        final JLabel mwsAccessKeyLbl = new JLabel("MWS Access Key:");
        final JLabel mwsSecretKeyLbl = new JLabel("MWS Secret Key:");

        final JLabel bookDataSourceUrlLbl = new JLabel("Book Spreadsheet:");
        final JLabel productDataSourceUrlLbl = new JLabel("Product Spreadsheet:");

        final JLabel userCodeLbl = new JLabel("User Code:");
        final JLabel primeBuyerLbl = new JLabel("Book Prime Buyer:");
        final JLabel buyerLbl = new JLabel("Book Buyer:");
        final JLabel prodPrimeBuyerLbl = new JLabel("Product Prime Buyer:");
        final JLabel prodBuyerLbl = new JLabel("Product Buyer:");

        final JLabel ebatesBuyerLbl = new JLabel("Ebates Buyer:");

        sellerFld.setToolTipText("Input seller account. Example: user@gmail.com/password");
        sellerEmailFld.setToolTipText("Input seller email. Example: user@gmail.com/password");
        storeNameFld.setToolTipText("Input your store front name");
        signatureFld.setToolTipText("Input your customer helper email signature");

        bookDataSourceUrlFld.setToolTipText("Input book order update spreadsheet url");
        primeBuyerFld.setToolTipText("Input prime buyer account that will fulfill book orders");
        buyerFld.setToolTipText("Input non-prime buyer account that will fulfill book orders");

        productDataSourceUrlFld.setToolTipText("Input product order update spreadsheet url");
        prodPrimeBuyerFld.setToolTipText("Input prime buyer account that will fulfill product orders");
        prodBuyerFld.setToolTipText("Input non-prime buyer account that will fulfill product orders");

        userCodeFld.setToolTipText("Input user code for validation usage");
        ebatesBuyerFld.setToolTipText("Input ebay buyer account for product order fulfillment benefit");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        final int width = 480;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(20)
                                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(sellerLbl)
                                        .addComponent(sellerEmailLbl)
                                        .addComponent(storeNameLbl)
                                        .addComponent(signatureLbl)
                                        .addComponent(sellerIdLbl)
                                        .addComponent(mwsAccessKeyLbl)
                                        .addComponent(mwsSecretKeyLbl)
                                        .addComponent(bookDataSourceUrlLbl)
                                        .addComponent(productDataSourceUrlLbl)
                                        .addComponent(userCodeLbl)
                                        .addComponent(primeBuyerLbl)
                                        .addComponent(buyerLbl)
                                        .addComponent(prodPrimeBuyerLbl)
                                        .addComponent(prodBuyerLbl)
                                        .addComponent(ebatesBuyerLbl))
                                .addGap(20)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup().addComponent(sellerFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(sellerEmailFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(storeNameFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(signatureFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(sellerIdFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(mwsAccessKeyFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(mwsSecretKeyFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(bookDataSourceUrlFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(productDataSourceUrlFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(userCodeFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(primeBuyerFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(buyerFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(prodPrimeBuyerFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(prodBuyerFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(ebatesBuyerFld, width, width, width))
                                        .addGap(20)
                                )));

        int vGap = 5, height = 30;
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(10)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sellerLbl).addComponent(sellerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sellerEmailLbl).addComponent(sellerEmailFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(storeNameLbl).addComponent(storeNameFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(signatureLbl).addComponent(signatureFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sellerIdLbl).addComponent(sellerIdFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(mwsAccessKeyLbl).addComponent(mwsAccessKeyFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(mwsSecretKeyLbl).addComponent(mwsSecretKeyFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(bookDataSourceUrlLbl).addComponent(bookDataSourceUrlFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(primeBuyerLbl).addComponent(primeBuyerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerLbl).addComponent(buyerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(productDataSourceUrlLbl).addComponent(productDataSourceUrlFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(prodPrimeBuyerLbl).addComponent(prodPrimeBuyerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(prodBuyerLbl).addComponent(prodBuyerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(ebatesBuyerLbl).addComponent(ebatesBuyerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(userCodeLbl).addComponent(userCodeFld, height, height, height))
                                .addGap(vGap)
                        ));
        UITools.addListener2Textfields(this);
    }

    private JTextField sellerFld = new JTextField();
    private JTextField sellerEmailFld = new JTextField();
    private JTextField storeNameFld = new JTextField();
    private JTextField signatureFld = new JTextField();
    private JTextField sellerIdFld = new JTextField();
    private JTextField mwsAccessKeyFld = new JTextField();
    private JTextField mwsSecretKeyFld = new JTextField();
    private JTextField bookDataSourceUrlFld = new JTextField();
    private JTextField productDataSourceUrlFld = new JTextField();
    private JTextField userCodeFld = new JTextField();
    private JTextField primeBuyerFld = new JTextField();
    private JTextField buyerFld = new JTextField();
    private JTextField prodPrimeBuyerFld = new JTextField();
    private JTextField prodBuyerFld = new JTextField();
    private JTextField ebatesBuyerFld = new JTextField();

    public Configuration collect() {
        Configuration cfg = new Configuration();

        cfg.setCountry(this.country);

        cfg.setSeller(new Account(sellerFld.getText(), AccountType.Seller));
        cfg.setSellerEmail(new Account(sellerEmailFld.getText(), AccountType.Seller));

        cfg.setStoreName(storeNameFld.getText().trim());
        cfg.setSignature(signatureFld.getText().trim());

        cfg.setMwsCredential(new MarketWebServiceIdentity(sellerIdFld.getText().trim(),
                mwsAccessKeyFld.getText().trim(), mwsSecretKeyFld.getText().trim(), country.marketPlaceId()));

        cfg.setBookDataSourceUrl(AppScript.getSpreadId(bookDataSourceUrlFld.getText().trim()));
        if (StringUtils.isNotBlank(primeBuyerFld.getText())) {
            cfg.setPrimeBuyer(new Account(primeBuyerFld.getText(), AccountType.PrimeBuyer));
        }
        if (StringUtils.isNotBlank(buyerFld.getText())) {
            cfg.setBuyer(new Account(buyerFld.getText(), AccountType.Buyer));
        }

        cfg.setProductDataSourceUrl(AppScript.getSpreadId(productDataSourceUrlFld.getText().trim()));
        if (StringUtils.isNotBlank(prodPrimeBuyerFld.getText())) {
            cfg.setProdPrimeBuyer(new Account(prodPrimeBuyerFld.getText(), AccountType.PrimeBuyer));
        }
        if (StringUtils.isNotBlank(prodBuyerFld.getText())) {
            cfg.setProdBuyer(new Account(prodBuyerFld.getText(), AccountType.Buyer));
        }
        cfg.setEbatesBuyer(new Account(ebatesBuyerFld.getText(), AccountType.Buyer));

        cfg.setUserCode(userCodeFld.getText().trim());
        return cfg;
    }

    public void load(@Nullable Configuration cfg) {
        if (cfg == null) {
            userCodeFld.setText(FinderCodeUtils.generate());
            return;
        }

        sellerFld.setText(this.abbrevAccount(cfg.getSeller()));
        sellerEmailFld.setText(this.abbrevAccount(cfg.getSellerEmail()));
        storeNameFld.setText(cfg.getStoreName());
        signatureFld.setText(cfg.getSignature());

        MarketWebServiceIdentity mwsCredential = cfg.getMwsCredential();
        if (mwsCredential != null && mwsCredential.valid()) {
            sellerIdFld.setText(mwsCredential.getSellerId());
            mwsAccessKeyFld.setText(mwsCredential.getAccessKey());
            mwsSecretKeyFld.setText(mwsCredential.getSecretKey());
        }

        bookDataSourceUrlFld.setText(cfg.getBookDataSourceUrl());
        productDataSourceUrlFld.setText(cfg.getProductDataSourceUrl());

        primeBuyerFld.setText(this.abbrevAccount(cfg.getPrimeBuyer()));
        buyerFld.setText(this.abbrevAccount(cfg.getBuyer()));

        prodPrimeBuyerFld.setText(this.abbrevAccount(cfg.getProdPrimeBuyer()));
        prodBuyerFld.setText(this.abbrevAccount(cfg.getProdBuyer()));

        ebatesBuyerFld.setText(this.abbrevAccount(cfg.getEbatesBuyer()));
        userCodeFld.setText(cfg.getUserCode());
    }

    private String abbrevAccount(Account account) {
        return account == null ? null : account.abbrev();
    }

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("US Marketplace Configuration");
        frame.setSize(600, 480);
        frame.getContentPane().add(new ConfigurationPanel(Country.US));
        frame.setVisible(true);
    }
}
