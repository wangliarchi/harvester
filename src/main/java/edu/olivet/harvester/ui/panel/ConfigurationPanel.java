package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Account.AccountType;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.BuyerAccountConfigDialog;
import edu.olivet.harvester.utils.FinderCodeUtils;
import edu.olivet.harvester.utils.Settings.Configuration;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration panel for single marketplace
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 9:51 PM
 */
public class ConfigurationPanel extends JPanel {

    @Getter
    private final Country country;
    private MarketWebServiceIdentity marketWebServiceIdentity;

    public ConfigurationPanel(Country country) {
        this.country = country;
        this.initComponents();
    }

    private JPanel mainPanel;
    private SellerPanel sellerPanel;

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


        addBuyerAccountButton.addActionListener(e -> {
            BuyerAccountConfigDialog dialog = UITools.setDialogAttr(new BuyerAccountConfigDialog());
            loadBuyerAccounts();
        });

        loadMWSInfoButton.setText("Find Seller Id");
        loadMWSInfoButton.addActionListener(evt -> new Thread(() -> {
            String sellerEmail = sellerEmailFld.getText();
            if (StringUtils.isBlank(sellerEmail)) {
                UITools.error("Please enter seller email/password first");
                return;
            }

            Account seller = new Account(sellerEmailFld.getText(), AccountType.Seller);
            sellerPanel = new SellerPanel(0, country, seller, 1);

            try {
                showSellerPanel();
                WaitTime.Short.execute();
                MarketWebServiceIdentity marketWebServiceIdentity = sellerPanel.fetchMWSInfo();
                if (marketWebServiceIdentity != null) {
                    String[] idName = marketWebServiceIdentity.getSellerId().split("\t");
                    sellerIdFld.setText(idName[0]);
                    storeNameFld.setText(idName[1]);
                    mwsAccessKeyFld.setText(marketWebServiceIdentity.getAccessKey());
                    mwsSecretKeyFld.setText(marketWebServiceIdentity.getSecretKey());
                }

            } catch (Exception e) {
                UITools.error("Error fetching seller id - " + e.getMessage());
            } finally {
                showMainPanel();
            }
        }).start());


        mainPanel = new JPanel();
        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);

        final int width = 480;
        final int loadMWSButtonWidth = (int) loadMWSInfoButton.getPreferredSize().getWidth();
        final int sellerIdFieldWidth = width - loadMWSButtonWidth;
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
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(sellerIdFld, sellerIdFieldWidth, sellerIdFieldWidth, sellerIdFieldWidth)
                                                .addComponent(loadMWSInfoButton))
                                        .addGroup(layout.createSequentialGroup().addComponent(mwsAccessKeyFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(mwsSecretKeyFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(bookDataSourceUrlFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(productDataSourceUrlFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(userCodeFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(bookPrimeBuyerJCombox, width - 130, width - 130, width - 130)
                                                .addComponent(addBuyerAccountButton))
                                        .addGroup(layout.createSequentialGroup().addComponent(bookBuyerJCombox, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(ebatesBuyerFld, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(prodBuyerJCombox, width, width, width))
                                        .addGroup(layout.createSequentialGroup().addComponent(prodPrimeBuyerJCombox, width, width, width))
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
                                        .addComponent(sellerIdLbl).addComponent(sellerIdFld, height, height, height)
                                        .addComponent(loadMWSInfoButton))
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
                                        .addComponent(primeBuyerLbl).addComponent(bookPrimeBuyerJCombox, height, height, height).addComponent(addBuyerAccountButton))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerLbl).addComponent(bookBuyerJCombox, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(productDataSourceUrlLbl)
                                        .addComponent(productDataSourceUrlFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(prodPrimeBuyerLbl).addComponent(prodPrimeBuyerJCombox, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(prodBuyerLbl).addComponent(prodBuyerJCombox, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(ebatesBuyerLbl).addComponent(ebatesBuyerFld, height, height, height))
                                .addGap(vGap)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(userCodeLbl).addComponent(userCodeFld, height, height, height))
                                .addGap(vGap)
                        ));


        showMainPanel();

        UITools.addListener2Textfields(this);
    }

    private void showMainPanel() {
        mainPanel.setVisible(true);
        if (sellerPanel != null) {
            sellerPanel.setVisible(false);
            try {
                sellerPanel.getBrowser().dispose();
            } catch (Exception e) {
                //
            }
        }
        this.setLayout(null);
        GroupLayout newLayout = new GroupLayout(this);
        newLayout.setHorizontalGroup(
                newLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainPanel));
        newLayout.setVerticalGroup(
                newLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainPanel));
        this.setLayout(newLayout);
        this.revalidate();
        this.repaint();
    }

    public void showSellerPanel() {
        mainPanel.setVisible(false);
        sellerPanel.setVisible(true);
        this.setLayout(null);
        GroupLayout newLayout = new GroupLayout(this);
        newLayout.setHorizontalGroup(
                newLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(sellerPanel));
        newLayout.setVerticalGroup(
                newLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(sellerPanel));
        this.setLayout(newLayout);
        this.revalidate();
        this.repaint();
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
    private JComboBox<Account> bookBuyerJCombox = new JComboBox();
    private JComboBox<Account> bookPrimeBuyerJCombox = new JComboBox();
    private JComboBox<Account> prodBuyerJCombox = new JComboBox();
    private JComboBox<Account> prodPrimeBuyerJCombox = new JComboBox();
    private JButton addBuyerAccountButton = new JButton("Add/Edit Buyer");
    private JButton loadMWSInfoButton = new JButton();

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
        if (bookPrimeBuyerJCombox.getSelectedItem() != null) {
            Account account = ((Account) bookPrimeBuyerJCombox.getSelectedItem());
            account.setType(AccountType.PrimeBuyer);
            cfg.setPrimeBuyer(account);
        }
        if (bookBuyerJCombox.getSelectedItem() != null) {
            Account account = (Account) bookBuyerJCombox.getSelectedItem();
            account.setType(AccountType.Buyer);
            cfg.setBuyer(account);
        }

        cfg.setProductDataSourceUrl(AppScript.getSpreadId(productDataSourceUrlFld.getText().trim()));
        if (prodPrimeBuyerJCombox.getSelectedItem() != null) {
            Account account = (Account) prodPrimeBuyerJCombox.getSelectedItem();
            account.setType(AccountType.PrimeBuyer);
            cfg.setProdPrimeBuyer(account);
        }
        if (prodBuyerJCombox.getSelectedItem() != null) {
            Account account = (Account) prodBuyerJCombox.getSelectedItem();
            account.setType(AccountType.Buyer);
            cfg.setProdBuyer(account);
        }
        cfg.setEbatesBuyer(new Account(ebatesBuyerFld.getText(), AccountType.Buyer));

        cfg.setUserCode(userCodeFld.getText().trim());
        return cfg;
    }

    public void load(@Nullable Configuration cfg) {
        this.cfg = cfg;
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

        loadBuyerAccounts();

    }

    Configuration cfg;

    public void loadBuyerAccounts() {
        List<Account> bookBuyers = BuyerAccountSettingUtils.load().getAccounts(country, OrderEnums.OrderItemType.BOOK, false)
                .stream().collect(Collectors.toList());
        bookBuyerJCombox.setModel(new DefaultComboBoxModel<>(bookBuyers.toArray(new Account[bookBuyers.size()])));

        List<Account> bookPrimeBuyers = BuyerAccountSettingUtils.load().getAccounts(country, OrderEnums.OrderItemType.BOOK, true)
                .stream().collect(Collectors.toList());
        bookPrimeBuyerJCombox.setModel(new DefaultComboBoxModel<>(bookPrimeBuyers.toArray(new Account[bookPrimeBuyers.size()])));


        List<Account> prodBuyers = BuyerAccountSettingUtils.load().getAccounts(country, OrderEnums.OrderItemType.PRODUCT, false)
                .stream().collect(Collectors.toList());
        prodBuyerJCombox.setModel(new DefaultComboBoxModel<>(prodBuyers.toArray(new Account[prodBuyers.size()])));


        List<Account> prodPrimeBuyers = BuyerAccountSettingUtils.load().getAccounts(country, OrderEnums.OrderItemType.PRODUCT, true)
                .stream().collect(Collectors.toList());
        prodPrimeBuyerJCombox.setModel(new DefaultComboBoxModel<>(prodPrimeBuyers.toArray(new Account[prodPrimeBuyers.size()])));


        if (cfg != null) {
            bookBuyerJCombox.setSelectedItem(cfg.getBuyer());
            bookPrimeBuyerJCombox.setSelectedItem(cfg.getPrimeBuyer());
            prodBuyerJCombox.setSelectedItem(cfg.getProdBuyer());
            prodPrimeBuyerJCombox.setSelectedItem(cfg.getBuyer());
        }
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
