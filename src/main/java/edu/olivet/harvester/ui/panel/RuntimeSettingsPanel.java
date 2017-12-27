package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.service.ProgressUpdater;
import edu.olivet.harvester.fulfill.service.RuntimePanelObserver;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.utils.SheetUtils;
import edu.olivet.harvester.ui.MainPanel;
import edu.olivet.harvester.utils.FinderCodeUtils;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.common.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 7:32 PM
 */
public class RuntimeSettingsPanel extends JPanel implements RuntimePanelObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSettingsPanel.class);

    private RuntimeSettings settings;

    private static RuntimeSettingsPanel instance;

    public static RuntimeSettingsPanel getInstance() {
        if (instance == null) {
            instance = new RuntimeSettingsPanel();
        }

        return instance;
    }

    private RuntimeSettingsPanel() {
        initComponents();
        initData();
        initEvents();
        hideDetails();
    }


    public void initData() {
        //RuntimeSettings
        settings = RuntimeSettings.load();
        marketplaceTextField.setText(settings.getMarketplaceName());

        setAccounts4Country();

        if (StringUtils.isNotBlank(settings.getSheetName())) {
            googleSheetTextField.setText(settings.getSpreadsheetName());
            googleSheetTextField.setToolTipText(settings.getSpreadsheetName() + " - " + settings.getSheetName());
        }

        selectedRangeLabel.setText(settings.getSheetName() + " " + settings.getAdvancedSubmitSetting().toString());

        if (StringUtils.isBlank(settings.getNoInvoiceText())) {
            settings.setNoInvoiceText("{No Invoice}");
        }

        noInvoiceTextField.setText(settings.getNoInvoiceText());


        lostLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"5", "7"}));
        if (StringUtils.isNotBlank(settings.getLostLimit())) {
            lostLimitComboBox.setSelectedItem(settings.getLostLimit());
        }

        priceLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"3", "5"}));
        priceLimitComboBox.setSelectedItem(settings.getPriceLimit());

        setOrderFinder();
        if (StringUtils.isBlank(settings.getFinderCode())) {
            settings.setFinderCode(finderCodeTextField.getText());
        }
        finderCodeTextField.setText(settings.getFinderCode());


        maxDaysOverEddComboBox.setModel(new DefaultComboBoxModel<>(
                new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"}
        ));
        maxDaysOverEddComboBox.setSelectedItem(settings.getEddLimit());


        skipCheckComboBox.setModel(new DefaultComboBoxModel<>(OrderValidator.SkipValidation.values()));
        skipCheckComboBox.setSelectedItem(settings.getSkipValidation());

        //loadBudget();

        disableAllBtns();
    }

    @Override
    public void updateSpending(String spending) {
        todayUsedTextField.setText(spending);
    }

    @Override
    public void updateBudget(String budget) {
        todayBudgetTextField.setText(budget);
    }



    public void initEvents() {

        showHideDetailButton.addActionListener(evt -> {
            if (showHideDetailButton.getText().equalsIgnoreCase("Show Details")) {
                showHideDetailButton.setText("Hide Details");
                showDetails();
            } else {
                showHideDetailButton.setText("Show Details");
                hideDetails();
            }

            try {
                MainPanel.getInstance().resetSplitPanelSizes();
            } catch (Exception e) {
                //
            }
        });
    }

    public void showProgressBar() {
        progressTextLabel.setVisible(true);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
    }


    public void disableAllBtns() {
        marketplaceTextField.setEnabled(false);
        buyerTextField.setEnabled(false);
        sellerTextField.setEnabled(false);
        primeBuyerTextField.setEnabled(false);
        googleSheetTextField.setEnabled(false);
        lostLimitComboBox.setEnabled(false);
        maxDaysOverEddComboBox.setEnabled(false);
        priceLimitComboBox.setEnabled(false);

        noInvoiceTextField.setEnabled(false);
        finderCodeTextField.setEnabled(false);
        skipCheckComboBox.setEnabled(false);
    }

    public void setAccounts4Country() {
        if (StringUtils.isBlank(marketplaceTextField.getText())) {
            return;
        }
        Country currentCountry = Country.valueOf(marketplaceTextField.getText());
        Settings.Configuration configuration;
        try {
            configuration = Settings.load().getConfigByCountry(currentCountry);
        } catch (Exception e) {
            return;
        }

        Account seller = configuration.getSeller();
        sellerTextField.setText(seller.getEmail());

        //default to book
        OrderEnums.OrderItemType type = SheetUtils.getTypeFromSpreadsheetName(settings.getSpreadsheetName());
        Account primeBuyer;
        Account buyer;
        if (type == OrderEnums.OrderItemType.BOOK) {
            buyer = configuration.getBuyer();
            primeBuyer = configuration.getPrimeBuyer();
        } else {
            buyer = configuration.getProdBuyer();
            primeBuyer = configuration.getProdPrimeBuyer();
        }

        buyerTextField.setText(buyer == null ? "" : buyer.getEmail());
        primeBuyerTextField.setText(primeBuyer == null ? "" : primeBuyer.getEmail());

    }

    public void setOrderFinder() {
        Country currentCountry = Country.valueOf(marketplaceTextField.getText());
        Settings.Configuration configuration = Settings.load().getConfigByCountry(currentCountry);
        if (FinderCodeUtils.validate(configuration.getUserCode())) {
            finderCodeTextField.setText(configuration.getUserCode());
        } else {
            finderCodeTextField.setText(FinderCodeUtils.generate());
        }
    }

    public void hideDetails() {
        buyerLabel.setVisible(false);
        buyerTextField.setVisible(false);
        primeBuyerLabel.setVisible(false);
        primeBuyerTextField.setVisible(false);
        lostLimitLabel.setVisible(false);
        lostLimitComboBox.setVisible(false);
        priceLimitLabel.setVisible(false);
        priceLimitComboBox.setVisible(false);
        maxEddLabel.setVisible(false);
        maxDaysOverEddComboBox.setVisible(false);
        noInvoiceLabel.setVisible(false);
        noInvoiceTextField.setVisible(false);
        codeFinderLabel.setVisible(false);
        finderCodeTextField.setVisible(false);
        skipCheckLabel.setVisible(false);
        skipCheckComboBox.setVisible(false);
    }

    public void showDetails() {
        buyerLabel.setVisible(true);
        buyerTextField.setVisible(true);
        primeBuyerLabel.setVisible(true);
        primeBuyerTextField.setVisible(true);
        lostLimitLabel.setVisible(true);
        lostLimitComboBox.setVisible(true);
        priceLimitLabel.setVisible(true);
        priceLimitComboBox.setVisible(true);
        maxEddLabel.setVisible(true);
        maxDaysOverEddComboBox.setVisible(true);
        noInvoiceLabel.setVisible(true);
        noInvoiceTextField.setVisible(true);
        codeFinderLabel.setVisible(true);
        finderCodeTextField.setVisible(true);
        skipCheckLabel.setVisible(true);
        skipCheckComboBox.setVisible(true);
    }

    private JTextField marketplaceTextField;
    private JLabel sellerLabel;
    private JTextField sellerTextField;
    private JLabel buyerLabel;
    private JTextField buyerTextField;
    private JLabel primeBuyerLabel;
    private JTextField primeBuyerTextField;

    private JLabel maxEddLabel;
    private JComboBox<String> lostLimitComboBox;
    private JComboBox<String> priceLimitComboBox;
    private JLabel selectedRangeLabel;
    private JTextField googleSheetTextField;
    private JTextField noInvoiceTextField;
    private JTextField finderCodeTextField;

    private JComboBox<String> maxDaysOverEddComboBox;

    private JComboBox<OrderValidator.SkipValidation> skipCheckComboBox;

    private JLabel lostLimitLabel;
    private JLabel priceLimitLabel;
    private JLabel noInvoiceLabel;
    private JLabel codeFinderLabel;
    private JLabel skipCheckLabel;
    private JLabel progressLabel;
    public JProgressBar progressBar;
    public JLabel progressTextLabel;
    private JTextField todayBudgetTextField;
    public JTextField todayUsedTextField;
    private JButton showHideDetailButton;

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("Runtime Settings")));

        JLabel marketplaceLabel = new JLabel();
        marketplaceTextField = new JTextField();

        sellerLabel = new JLabel();
        sellerTextField = new JTextField();

        buyerLabel = new JLabel();
        buyerTextField = new JTextField();
        primeBuyerLabel = new JLabel();
        primeBuyerTextField = new JTextField();

        JLabel googleSheetLabel = new JLabel();
        googleSheetTextField = new JTextField();
        JLabel selectRangeLabel = new JLabel();
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

        marketplaceLabel.setText("Country");
        sellerLabel.setText("Seller");
        buyerLabel.setText("Buyer");
        primeBuyerLabel.setText("Prime Buyer");
        googleSheetLabel.setText("Sheet");
        selectRangeLabel.setText("Range");
        lostLimitLabel.setText("Lost Limit");
        priceLimitLabel.setText("Price Limit");
        noInvoiceLabel.setText("No Invoice");
        codeFinderLabel.setText("Finder Code");
        maxEddLabel.setText("EDD Limit");


        skipCheckLabel = new JLabel();
        skipCheckLabel.setText("Skip Check");
        skipCheckLabel.setForeground(Color.RED);


        skipCheckComboBox = new JComboBox<>();
        Font font = skipCheckLabel.getFont();
        if (SystemUtils.IS_OS_WINDOWS) {
            skipCheckComboBox.setFont(new Font(font.getName(), Font.BOLD, font.getSize() - 1));
        }


        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressLabel = new JLabel();
        progressLabel.setText("Progress");

        progressTextLabel = new JLabel();
        progressTextLabel.setText("progress text placeholder");
        progressTextLabel.setForeground(Color.BLUE);
        progressTextLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 2));

        progressTextLabel.setVisible(false);
        progressLabel.setVisible(false);
        progressBar.setVisible(false);


        JLabel todayBudgetLabel = new JLabel();
        todayBudgetLabel.setText("Budget");
        JLabel todayUsedLabel = new JLabel();
        todayUsedLabel.setText("Used");
        todayBudgetTextField = new JTextField();
        todayUsedTextField = new JTextField();
        todayUsedTextField.setEnabled(false);

        showHideDetailButton = new JButton();
        showHideDetailButton.setText("Show Details");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        int fieldWidth = 150;
        int labelMinWidth = 55;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(buyerLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(selectRangeLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lostLimitLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(maxEddLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(codeFinderLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)

                                                )
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(buyerTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(selectedRangeLabel)
                                                        .addComponent(lostLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(maxDaysOverEddComboBox, labelMinWidth, fieldWidth, fieldWidth)

                                                        .addComponent(finderCodeTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                )
                                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(sellerLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(primeBuyerLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addGap(labelMinWidth)
                                                        .addComponent(priceLimitLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(noInvoiceLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(skipCheckLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                )
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(sellerTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(primeBuyerTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addGap(labelMinWidth)
                                                        .addComponent(priceLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(noInvoiceTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(skipCheckComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                )

                                        )
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(googleSheetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(googleSheetTextField, labelMinWidth, 200, Short.MAX_VALUE)
                                        )
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(todayBudgetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(todayBudgetTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                                .addComponent(todayUsedLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(todayUsedTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(progressLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(progressBar, labelMinWidth, fieldWidth * 2, Short.MAX_VALUE)
                                        )
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGap(labelMinWidth)
                                                .addComponent(progressTextLabel)
                                                .addContainerGap()
                                        )
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGap(labelMinWidth)
                                                .addComponent(showHideDetailButton)
                                        )

                                )
                                .addContainerGap()
                        )
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(marketplaceLabel)
                                        .addComponent(marketplaceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sellerLabel)
                                        .addComponent(sellerTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))

                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerLabel)
                                        .addComponent(buyerTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(primeBuyerLabel)
                                        .addComponent(primeBuyerTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(googleSheetLabel)
                                        .addComponent(googleSheetTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(selectRangeLabel)
                                        .addComponent(selectedRangeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lostLimitLabel)
                                        .addComponent(lostLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(priceLimitLabel)
                                        .addComponent(priceLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(maxEddLabel)
                                        .addComponent(maxDaysOverEddComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(noInvoiceLabel)
                                        .addComponent(noInvoiceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(codeFinderLabel)
                                        .addComponent(finderCodeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(skipCheckLabel)
                                        .addComponent(skipCheckComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(todayBudgetLabel)
                                        .addComponent(todayBudgetTextField)
                                        .addComponent(todayUsedLabel)
                                        .addComponent(todayUsedTextField)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(progressLabel)
                                        .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addComponent(progressTextLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(showHideDetailButton)
                                .addContainerGap()
                        )
        );
    }


    public int getMinWidth() {
        return 120;
    }

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Runtime Settings");
        frame.setSize(400, 580);
        RuntimeSettingsPanel runtimeSettingsPanel = RuntimeSettingsPanel.getInstance();
        frame.getContentPane().add(runtimeSettingsPanel);
        frame.setVisible(true);
        ProgressUpdater.success();
        runtimeSettingsPanel.showProgressBar();
    }

}
