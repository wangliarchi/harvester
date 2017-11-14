package edu.olivet.harvester.ui;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.fulfill.model.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.events.MarkStatusEvent;
import edu.olivet.harvester.ui.events.SubmitOrdersEvent;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 7:32 PM
 */
public class RuntimeSettingsPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSettingsPanel.class);

    private Worksheet selectedWorksheet;
    private RuntimeSettings settings;

    public RuntimeSettingsPanel() {
        initComponents();
        initData();
        initEvents();
    }


    public void initData() {

        //RuntimeSettings
        settings = RuntimeSettings.load();
        Settings systemSettings = Settings.load();
        List<Country> countries = systemSettings.listAllCountries();
        settings.setSid(systemSettings.getSid());

        marketplaceComboBox.setModel(new DefaultComboBoxModel<>(countries.toArray(new Country[countries.size()])));
        if (settings.getMarketplaceName() == null) {
            settings.setMarketplaceName(((Country) marketplaceComboBox.getSelectedItem()).name());
        } else {
            marketplaceComboBox.setSelectedItem(Country.valueOf(settings.getMarketplaceName()));
        }

        setAccounts4Country();

        if (StringUtils.isNotBlank(settings.getSheetName())) {
            googleSheetTextField.setText(settings.getSheetName());
            googleSheetTextField.setToolTipText(settings.getSpreadsheetName() + " - " + settings.getSheetName());
        }


        selectRangeButton.setText("Select");
        selectedRangeLabel.setText(settings.getAdvancedSubmitSetting().toString());

        if (StringUtils.isBlank(settings.getNoInvoiceText())) {
            settings.setNoInvoiceText("{No Invoice}");
        }
        noInvoiceTextField.setText(settings.getNoInvoiceText());


        lostLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"5", "7"}));
        if (StringUtils.isNotBlank(settings.getLostLimit())) {
            lostLimitComboBox.setSelectedItem(settings.getLostLimit());
        }

        priceLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"3", "5"}));
        if (StringUtils.isNotBlank(settings.getPriceLimit())) {
            priceLimitComboBox.setSelectedItem(settings.getPriceLimit());
        }

        setOrderFinder();
        if (StringUtils.isBlank(settings.getFinderCode())) {
            settings.setFinderCode(finderCodeTextField.getText());
        }
        finderCodeTextField.setText(settings.getFinderCode());


        maxDaysOverEddComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"}));
        if (StringUtils.isNotBlank((settings.getEddLimit()))) {
            maxDaysOverEddComboBox.setSelectedItem(settings.getEddLimit());
        }


        settings.save();
    }

    public void initEvents() {
        marketplaceComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem() != Country.valueOf(settings.getMarketplaceName())) {
                    clearGoogleSheet();
                    clearSubmitRange();
                    settings = new RuntimeSettings();
                    settings.setMarketplaceName(((Country) e.getItem()).name());
                    settings.save();
                    initData();
                }
            }
        });


        googleSheetTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectGoogleSheet();
            }
        });
        googleSheetTextField.setEditable(false);
        googleSheetTextField.setCursor(new Cursor(Cursor.HAND_CURSOR));

        selectRangeButton.addActionListener(evt -> selectRange());

        finderCodeTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                finderCodeTextFieldActionPerformed();
            }
        });

        noInvoiceTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                noInvoiceTextFieldActionPerformed();
            }
        });

        lostLimitComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                settings.setLostLimit(e.getItem().toString());
                settings.save();
            }
        });

        priceLimitComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                settings.setPriceLimit(e.getItem().toString());
                settings.save();
            }
        });

        maxDaysOverEddComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                settings.setEddLimit(e.getItem().toString());
                settings.save();
            }
        });

        markStatusButton.addActionListener(evt -> {
            Thread worker = new Thread(() -> {
                try {
                    disableAllBtns();
                    ApplicationContext.getBean(MarkStatusEvent.class).excute();
                } catch (Exception e) {
                    UITools.error("ERROR while marking order status:" + e.getMessage(), UIText.title("title.code_error"));
                    LOGGER.error("ERROR while marking order status:", e);
                } finally {
                    restAllBtns();
                }
            });
            worker.start();
        });

        submitButton.addActionListener(evt -> {
            Thread worker = new Thread(() -> {
                try {
                    disableAllBtns();
                    ApplicationContext.getBean(SubmitOrdersEvent.class).excute();
                } catch (Exception e) {
                    UITools.error(UIText.message("message.submit.exception", e.getMessage()), UIText.title("title.code_error"));
                    LOGGER.error("做单过程中出现异常:", e);
                } finally {
                    restAllBtns();
                }
            });
            worker.start();


        });


    }

    public void disableAllBtns() {
        marketplaceComboBox.setEnabled(false);
        googleSheetTextField.setEnabled(false);
        lostLimitComboBox.setEnabled(false);
        maxDaysOverEddComboBox.setEnabled(false);
        priceLimitComboBox.setEnabled(false);
        selectRangeButton.setEnabled(false);
        markStatusButton.setEnabled(false);
        huntSupplierButton.setEnabled(false);
        submitButton.setEnabled(false);
        noInvoiceTextField.setEnabled(false);
        finderCodeTextField.setEnabled(false);
    }

    public void restAllBtns() {
        marketplaceComboBox.setEnabled(true);
        googleSheetTextField.setEnabled(true);
        maxDaysOverEddComboBox.setEnabled(true);
        lostLimitComboBox.setEnabled(true);
        priceLimitComboBox.setEnabled(true);
        selectRangeButton.setEnabled(true);
        markStatusButton.setEnabled(true);
        huntSupplierButton.setEnabled(true);
        submitButton.setEnabled(true);
        noInvoiceTextField.setEnabled(true);
        finderCodeTextField.setEnabled(true);
    }

    public void selectGoogleSheet() {
        StringBuilder spreadsheetIdError = new StringBuilder();
        AppScript appScript = new AppScript();
        Country selectedCountry = (Country) marketplaceComboBox.getSelectedItem();
        List<edu.olivet.harvester.spreadsheet.Spreadsheet> spreadsheets = Settings.load().listSpreadsheets(selectedCountry, appScript);

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("No order update google sheet found. Please make sure it's configured and shared with " + Constants.RND_EMAIL, "Error");
        }

        ChooseSheetDialog chooseSheetDialog = new ChooseSheetDialog(spreadsheets, appScript);
        chooseSheetDialog.setSheetNameListSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (StringUtils.isNotBlank(settings.getSpreadsheetName()) && StringUtils.isNotBlank(settings.getSheetName())) {
            chooseSheetDialog.setSelectedSpreadsheet(settings.getSpreadsheetName());
            chooseSheetDialog.setSelectedSheet(settings.getSheetName());
        }
        chooseSheetDialog.showContinueBtn(true);
        ChooseSheetDialog dialog = UITools.setDialogAttr(chooseSheetDialog);

        dialog.invalidate();
        if (dialog.isOk()) {
            if (CollectionUtils.isNotEmpty(dialog.getSelectedWorksheets())) {
                if (!dialog.getSelectedWorksheets().get(0).equals(selectedWorksheet)) {
                    clearSubmitRange();
                }
                selectedWorksheet = dialog.getSelectedWorksheets().get(0);
            }

            googleSheetTextField.setText(selectedWorksheet.getSheetName());
            googleSheetTextField.setToolTipText(selectedWorksheet.toString());
            settings.setSpreadsheetId(selectedWorksheet.getSpreadsheet().getSpreadsheetId());
            settings.setSpreadsheetName(selectedWorksheet.getSpreadsheet().getTitle());
            settings.setSheetName(selectedWorksheet.getSheetName());
            settings.setAdvancedSubmitSetting(new AdvancedSubmitSetting());
            settings.save();

            if(dialog.continueToNext) {
                selectRange();
            }
        }
    }

    public void selectRange() {
        UITools.setDialogAttr(new SelectRangeDialog(null, true, settings.getAdvancedSubmitSetting()), true);
        settings = RuntimeSettings.load();
        selectedRangeLabel.setText(settings.getAdvancedSubmitSetting().toString());
    }
    public void setAccounts4Country() {
        Country currentCountry = (Country) marketplaceComboBox.getSelectedItem();
        Settings.Configuration configuration = Settings.load().getConfigByCountry(currentCountry);

        Account seller = configuration.getSeller();
        Account[] sellers = seller == null ? new Account[0] : new Account[]{seller};
        sellerComboBox.setModel(new DefaultComboBoxModel<>(sellers));

        Account buyer = configuration.getBuyer();
        Account[] buyers = buyer == null ? new Account[0] : new Account[]{buyer};
        bookBuyerComboBox.setModel(new DefaultComboBoxModel<>(buyers));

        Account primeBuyer = configuration.getPrimeBuyer();
        Account[] primeBuyers = primeBuyer == null ? new Account[0] : new Account[]{primeBuyer};
        primeBookBuyerComboBox.setModel(new DefaultComboBoxModel<>(primeBuyers));

        Account prodBuyer = configuration.getProdBuyer();
        Account[] prodBuyers = prodBuyer == null ? new Account[0] : new Account[]{prodBuyer};
        prodBuyerComboBox.setModel(new DefaultComboBoxModel<>(prodBuyers));

        Account prodPrimeBuyer = configuration.getProdPrimeBuyer();
        Account[] prodPrimeBuyers = prodPrimeBuyer == null ? new Account[0] : new Account[]{prodPrimeBuyer};
        primeProdBuyerComboBox.setModel(new DefaultComboBoxModel<>(prodPrimeBuyers));
    }

    public void setOrderFinder() {
        Country currentCountry = (Country) marketplaceComboBox.getSelectedItem();
        Settings.Configuration configuration = Settings.load().getConfigByCountry(currentCountry);

        finderCodeTextField.setText(configuration.getUserCode());
    }

    private void clearGoogleSheet() {
        googleSheetTextField.setText(null);
        googleSheetTextField.setToolTipText(null);

    }

    /**
     * 切换国家的时候，将之前选择的做单范围也全部清空
     */
    private void clearSubmitRange() {
        selectedRangeLabel.setText("All");
    }

    private void noInvoiceTextFieldActionPerformed() {
        settings.setNoInvoiceText(noInvoiceTextField.getText());
        settings.save();
    }

    private void finderCodeTextFieldActionPerformed() {
        settings.setFinderCode(finderCodeTextField.getText());
        settings.save();
    }


    // Variables declaration - do not modify
    private JComboBox<Country> marketplaceComboBox;
    private JComboBox<Account> sellerComboBox;
    private JComboBox<Account> bookBuyerComboBox;
    private JComboBox<Account> primeBookBuyerComboBox;
    private JComboBox<Account> prodBuyerComboBox;
    private JComboBox<Account> primeProdBuyerComboBox;
    private JButton selectRangeButton;
    private JComboBox<String> lostLimitComboBox;
    private JComboBox<String> priceLimitComboBox;
    private JLabel marketplaceLabel;
    private JLabel sellerLabel;
    private JLabel bookBuyerLabel;
    private JLabel primeBookBuyerLabel;
    private JLabel prodBuyerLabel;
    private JLabel primeProdBuyerLabel;
    private JLabel selectRangeLabel;
    private JLabel lostLimitLabel;
    private JLabel priceLimitLabel;
    private JLabel googleSheetLabel;
    private JLabel noInvoiceLabel;
    private JLabel codeFinderLabel;
    private JLabel selectedRangeLabel;
    private JTextField googleSheetTextField;
    private JTextField noInvoiceTextField;
    private JTextField finderCodeTextField;

    private JButton huntSupplierButton;
    private JButton markStatusButton;
    private JButton submitButton;

    private JLabel maxEddLabel;
    private JComboBox<String> maxDaysOverEddComboBox;

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("Runtime Settings")));

        marketplaceLabel = new JLabel();
        marketplaceComboBox = new JComboBox<>();

        sellerLabel = new JLabel();
        sellerComboBox = new JComboBox<>();

        bookBuyerLabel = new JLabel();
        bookBuyerComboBox = new JComboBox<>();
        primeBookBuyerLabel = new JLabel();
        primeBookBuyerComboBox = new JComboBox<>();
        prodBuyerLabel = new JLabel();
        prodBuyerComboBox = new JComboBox<>();
        primeProdBuyerLabel = new JLabel();
        primeProdBuyerComboBox = new JComboBox<>();
        googleSheetLabel = new JLabel();
        googleSheetTextField = new JTextField();
        selectRangeLabel = new JLabel();
        selectRangeButton = new JButton();
        lostLimitLabel = new JLabel();
        lostLimitComboBox = new JComboBox<>();
        priceLimitLabel = new JLabel();
        priceLimitComboBox = new JComboBox<>();
        noInvoiceLabel = new JLabel();
        noInvoiceTextField = new JTextField();
        codeFinderLabel = new JLabel();
        finderCodeTextField = new JTextField();
        selectedRangeLabel = new JLabel();
        selectedRangeLabel.setForeground(Color.BLUE);

        maxEddLabel = new JLabel();
        maxDaysOverEddComboBox = new JComboBox<>();

        huntSupplierButton = new JButton();
        huntSupplierButton.setText("Hunt Supplier");
        huntSupplierButton.setIcon(UITools.getIcon("find.png"));
        markStatusButton = new JButton();
        markStatusButton.setText("Mark Status");
        markStatusButton.setIcon(UITools.getIcon("status.png"));
        submitButton = new JButton();
        submitButton.setText("Submit");
        submitButton.setIcon(UITools.getIcon("start.png"));


        marketplaceLabel.setText("Marketplace");
        sellerLabel.setText("Seller");
        bookBuyerLabel.setText("Book Buyer");
        primeBookBuyerLabel.setText("Prime Book Buyer");
        prodBuyerLabel.setText("Prod Buyer");
        primeProdBuyerLabel.setText("Prime Prod Buyer");
        googleSheetLabel.setText("Google Sheet");
        selectRangeLabel.setText("Select Range");
        lostLimitLabel.setText("Lost Limit");
        priceLimitLabel.setText("Price Limit");
        noInvoiceLabel.setText("No Invoice");
        codeFinderLabel.setText("Finder Code");
        maxEddLabel.setText("EDD Limit");


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        int fieldWidth = 180;
        int labelMinWidth = 100;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)

                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(sellerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(bookBuyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(primeBookBuyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(prodBuyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(primeProdBuyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(googleSheetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(selectRangeLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lostLimitLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(priceLimitLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(maxEddLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(noInvoiceLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(codeFinderLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                )


                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(sellerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(bookBuyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(primeBookBuyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(prodBuyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(primeProdBuyerComboBox, 0, fieldWidth, fieldWidth)
                                                        .addComponent(googleSheetTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(selectRangeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(selectedRangeLabel)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))


                                                        .addComponent(lostLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(priceLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(maxDaysOverEddComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(noInvoiceTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(finderCodeTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                )


                                        ).addGroup(layout.createSequentialGroup()
                                                .addComponent(huntSupplierButton)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(markStatusButton)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(submitButton))

                                )

                        )
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(marketplaceLabel)
                                        .addComponent(marketplaceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sellerLabel)
                                        .addComponent(sellerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(bookBuyerLabel)
                                        .addComponent(bookBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(primeBookBuyerLabel)
                                        .addComponent(primeBookBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(prodBuyerLabel)
                                        .addComponent(prodBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(primeProdBuyerLabel)
                                        .addComponent(primeProdBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(googleSheetLabel)
                                        .addComponent(googleSheetTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(selectRangeLabel)
                                        .addComponent(selectRangeButton)
                                        .addComponent(selectedRangeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lostLimitLabel)
                                        .addComponent(lostLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(priceLimitLabel)
                                        .addComponent(priceLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(maxEddLabel)
                                        .addComponent(maxDaysOverEddComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(noInvoiceLabel)
                                        .addComponent(noInvoiceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(codeFinderLabel)
                                        .addComponent(finderCodeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(huntSupplierButton)
                                        .addComponent(markStatusButton)
                                        .addComponent(submitButton)))
        );
    }


    public int getMinWidth() {
        return huntSupplierButton.getWidth() + markStatusButton.getWidth() + submitButton.getWidth() + 2 * 15;
    }

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Runtime Settings");
        frame.setSize(400, 580);
        frame.getContentPane().add(new RuntimeSettingsPanel());
        frame.setVisible(true);
    }

}
