package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.spreadsheet.model.OrderRange;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.MainPanel;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.utils.FinderCodeUtils;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 7:32 PM
 */
public class OrderSubmissionSettingsPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmissionSettingsPanel.class);

    private List<Worksheet> selectedWorksheets;
    private String selectedSpreadsheetId;
    private Country selectedMarketplace;
    private final Map<Integer, JComboBox<String>> sheetNameMap = new LinkedHashMap<>();
    private final Map<Integer, JTextField> beginRowMap = new HashMap<>();
    private final Map<Integer, JTextField> endRowMap = new HashMap<>();
    private static final int MAX_COUNT = 7;
    @Getter
    private List<String> sheetNames = new ArrayList<>();

    GroupLayout layout = new GroupLayout(this);
    GroupLayout.ParallelGroup hPG = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
    GroupLayout.SequentialGroup vSG = layout.createSequentialGroup();
    private int fieldWidth = 200;
    private int labelMinWidth = 80;


    public OrderSubmissionSettingsPanel(Window parentFrame) {
        this.parentFrame = parentFrame;
        initComponents();
        initLayout();
        initData();
        initEvents();
    }


    public void initEvents() {
        marketplaceComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem() != selectedMarketplace) {
                    selectedMarketplace = (Country) e.getItem();
                    clearGoogleSheet();
                    clearSubmitRange();
                    initData();
                }


            }
        });

        selectSheetButton.addActionListener(evt -> {
            selectGoogleSheet();
        });

        showHideDetailButton.addActionListener(evt -> {
            if (showHideDetailButton.getText().equalsIgnoreCase("Show More Runtime Settings")) {
                showHideDetailButton.setText("Hide Detail Settings");
                showDetails();
            } else {
                showHideDetailButton.setText("Show More Runtime Settings");
                hideDetails();
            }

            try {
                MainPanel.getInstance().resetSplitPanelSizes();
            } catch (Exception e) {
                //
            }
        });

    }

    public void hideDetails() {
        detailSettingsPanel.setVisible(false);
        parentFrame.pack();
    }

    public void showDetails() {
        detailSettingsPanel.setVisible(true);
        parentFrame.pack();
    }

    public void selectGoogleSheet() {

        AppScript appScript = new AppScript();
        Country selectedCountry = (Country) marketplaceComboBox.getSelectedItem();
        List<Spreadsheet> spreadsheets = Settings.load().listSpreadsheets(selectedCountry, appScript);

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("No order update sheet found. Please make sure it's configured and shared with " + Constants.RND_EMAIL, "Error");
        }

        ChooseSheetDialog chooseSheetDialog = new ChooseSheetDialog(spreadsheets, appScript);
        ChooseSheetDialog dialog = UITools.setDialogAttr(chooseSheetDialog);

        if (dialog.isOk()) {
            selectedWorksheets = dialog.getSelectedWorksheets();
            selectedSheetName.setText(selectedWorksheets.get(0).getSpreadsheet().getTitle());
            selectedSheetName.setToolTipText(selectedWorksheets.get(0).getSpreadsheet().getTitle());
            selectedSpreadsheetId = dialog.getSelectedWorksheets().get(0).getSpreadsheet().getSpreadsheetId();
            sheetNames = selectedWorksheets.stream().map(it -> it.getSheetName()).collect(Collectors.toList());
            sheetNames.add(0, "");
            updateSheetOptions();
        }
    }


    public void setAccounts4Country() {
        Country currentCountry = selectedMarketplace;
        Settings.Configuration configuration;
        try {
            configuration = Settings.load().getConfigByCountry(currentCountry);
        } catch (Exception e) {
            marketplaceComboBox.setSelectedIndex(0);
            return;
        }

        Account seller = configuration.getSeller();
        Account[] sellers = seller == null ? new Account[0] : new Account[]{seller};
        sellerComboBox.setModel(new DefaultComboBoxModel<>(sellers));

        //default to book
        OrderItemType type;
        if (StringUtils.isBlank(selectedSpreadsheetId)) {
            type = OrderItemType.BOOK;
        } else {
            try {
                type = Settings.load().getSpreadsheetType(selectedSpreadsheetId);
            } catch (Exception e) {
                type = OrderItemType.BOOK;
            }
        }


        Account primeBuyer;
        Account buyer;
        if (type == OrderItemType.BOOK) {
            buyer = configuration.getBuyer();
            primeBuyer = configuration.getPrimeBuyer();
        } else {
            buyer = configuration.getProdBuyer();
            primeBuyer = configuration.getProdPrimeBuyer();
        }

        List<Account> buyerAccounts = BuyerAccountSettingUtils.load().getAccounts(currentCountry, type, false);
        buyerComboBox.setModel(new DefaultComboBoxModel<>(buyerAccounts.toArray(new Account[buyerAccounts.size()])));
        buyerComboBox.setSelectedItem(buyer);

        List<Account> primeBuyerAccounts = BuyerAccountSettingUtils.load().getAccounts(currentCountry, type, true);
        primeBuyerComboBox.setModel(new DefaultComboBoxModel<>(primeBuyerAccounts.toArray(new Account[primeBuyerAccounts.size()])));
        primeBuyerComboBox.setSelectedItem(primeBuyer);

    }

    public void setOrderFinder() {
        Country currentCountry = selectedMarketplace;
        Settings.Configuration configuration = Settings.load().getConfigByCountry(currentCountry);
        if (FinderCodeUtils.validate(configuration.getUserCode())) {
            finderCodeTextField.setText(configuration.getUserCode());
        } else {
            finderCodeTextField.setText(FinderCodeUtils.generate());
        }
    }

    private void clearGoogleSheet() {
        selectedSheetName.setText(null);
        selectedSheetName.setToolTipText(null);
        selectedSpreadsheetId = null;
    }


    private void updateSheetOptions() {
        sheetSelectionPanel.setVisible(true);
        showHideDetailButton.setVisible(true);
        Collections.sort(sheetNames);
        String[] sheets = sheetNames.toArray(new String[sheetNames.size()]);
        for (int i = 0; i < MAX_COUNT; i++) {
            JComboBox<String> sheetComboBox = sheetNameMap.get(i);
            sheetComboBox.setModel(new DefaultComboBoxModel<>(sheets));
            if (i == 0) {
                sheetComboBox.setSelectedIndex(1);
            } else {
                sheetComboBox.setSelectedIndex(0);
            }

        }

        parentFrame.pack();


    }

    public List<OrderSubmissionTask> collectData() {
        if (StringUtils.isBlank(selectedSpreadsheetId)) {
            throw new BusinessException("Please select order update sheet first.");
        }
        List<OrderRange> orderRanges = collectOrderRanges();
        if (CollectionUtils.isEmpty(orderRanges)) {
            throw new BusinessException("Please select order range");
        }

        List<OrderSubmissionTask> tasks = new ArrayList<>();
        for (OrderRange orderRange : orderRanges) {
            OrderSubmissionTask orderSubmissionTask = new OrderSubmissionTask();
            orderSubmissionTask.setSid(Settings.load().getSid());
            orderSubmissionTask.setMarketplaceName(((Country) marketplaceComboBox.getSelectedItem()).name());
            orderSubmissionTask.setLostLimit(lostLimitComboBox.getSelectedItem().toString());
            orderSubmissionTask.setPriceLimit(priceLimitComboBox.getSelectedItem().toString());
            orderSubmissionTask.setEddLimit(maxDaysOverEddComboBox.getSelectedItem().toString());
            orderSubmissionTask.setNoInvoiceText(noInvoiceTextField.getText());
            orderSubmissionTask.setFinderCode(finderCodeTextField.getText());
            orderSubmissionTask.setSkipValidation((OrderValidator.SkipValidation) skipCheckComboBox.getSelectedItem());
            orderSubmissionTask.setSpreadsheetId(selectedSpreadsheetId);
            orderSubmissionTask.setSpreadsheetName(selectedSheetName.getText());
            orderSubmissionTask.setOrderRange(orderRange);
            orderSubmissionTask.setBuyerAccount(((Account)buyerComboBox.getSelectedItem()).getEmail());
            orderSubmissionTask.setPrimeBuyerAccount(((Account)primeBuyerComboBox.getSelectedItem()).getEmail());
            tasks.add(orderSubmissionTask);
        }

        return tasks;
    }

    public List<OrderRange> collectOrderRanges() {
        List<OrderRange> ranges = new ArrayList<>();
        for (int i = 0; i < MAX_COUNT; i++) {
            String sheetName = (String) sheetNameMap.get(i).getSelectedItem();
            if (StringUtils.isNotBlank(sheetName)) {
                String beginRowText = beginRowMap.get(i).getText().trim();
                Integer beginRow = StringUtils.isNotBlank(beginRowText) ? Integer.parseInt(beginRowText) : null;

                String endRowText = endRowMap.get(i).getText().trim();
                Integer endRow = StringUtils.isNotBlank(endRowText) ? Integer.parseInt(endRowText) : null;

                if (beginRow != null && endRow != null && beginRow > endRow) {
                    throw new BusinessException("Error on sheet " + (i + 1) + " - End row should be empty or bigger than start row");
                }

                ranges.add(new OrderRange(sheetName, beginRow, endRow));
            }
        }

        return ranges;
    }

    public void clearSubmitRange() {
        for (int i = 0; i < MAX_COUNT; i++) {
            beginRowMap.get(i).setText("");
            endRowMap.get(i).setText("");
        }
    }


    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("Runtime Settings")));

        marketplaceLabel = new JLabel();
        marketplaceComboBox = new JComboBox<>();

        sellerLabel = new JLabel();
        sellerComboBox = new JComboBox<>();

        buyerLabel = new JLabel();
        buyerComboBox = new JComboBox<>();
        primeBuyerLabel = new JLabel();
        primeBuyerComboBox = new JComboBox<>();

        googleSheetLabel = new JLabel();

        lostLimitLabel = new JLabel();
        lostLimitComboBox = new JComboBox<>();
        priceLimitLabel = new JLabel();
        priceLimitComboBox = new JComboBox<>();
        noInvoiceLabel = new JLabel();
        noInvoiceTextField = new JTextField();
        codeFinderLabel = new JLabel();
        finderCodeTextField = new JTextField();
        maxEddLabel = new JLabel();
        maxDaysOverEddComboBox = new JComboBox<>();

        selectSheetButton = new JButton();
        selectSheetButton.setText("Select");
        selectedSheetName = new JLabel();
        selectedSheetName.setForeground(Color.BLUE);

        showHideDetailButton = new JButton();
        showHideDetailButton.setText("Show More Runtime Settings");
        marketplaceLabel.setText("Marketplace");
        sellerLabel.setText("Seller");
        buyerLabel.setText("Buyer");
        primeBuyerLabel.setText("Prime Buyer");
        googleSheetLabel.setText("Google Sheet");
        lostLimitLabel.setText("Lost Limit");
        priceLimitLabel.setText("Price Limit");
        noInvoiceLabel.setText("No Invoice");
        codeFinderLabel.setText("Finder Code");
        maxEddLabel.setText("EDD Limit");
        skipCheckLabel = new JLabel();
        skipCheckLabel.setText("Skip Check");
        skipCheckLabel.setForeground(Color.RED);
        skipCheckComboBox = new JComboBox<>();

        showHideDetailButton.setVisible(false);
    }


    public void initLayout() {

        this.setLayout(layout);

        initSheetSelections();
        initDetailSettingPanel();


        hPG.addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(marketplaceLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sellerLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                )
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(marketplaceComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(sellerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                )

                        )
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(googleSheetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(selectSheetButton, 100, 100, 100)
                                .addComponent(selectedSheetName)
                        )
                        .addComponent(sheetSelectionPanel)
                        .addComponent(showHideDetailButton)
                        .addComponent(detailSettingsPanel)


                ).addContainerGap()
        );


        vSG.addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(marketplaceLabel)
                        .addComponent(marketplaceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(sellerLabel)
                        .addComponent(sellerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(googleSheetLabel)
                        .addComponent(selectSheetButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(selectedSheetName)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sheetSelectionPanel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showHideDetailButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(detailSettingsPanel)
                .addContainerGap()
        );


        layout.setHorizontalGroup(hPG);
        layout.setVerticalGroup(vSG);
    }


    public void initSheetSelections() {
        sheetSelectionPanel = new JPanel();
        sheetSelectionPanel.setVisible(false);

        GroupLayout panelLayout = new GroupLayout(sheetSelectionPanel);
        GroupLayout.ParallelGroup panelLayoutParallelGroup = panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup panelLayoutSequentialGroup = panelLayout.createSequentialGroup();

        int rowFieldWidth = 100;
        for (int i = 0; i < MAX_COUNT; i++) {
            JLabel sheetNameLbl = new JLabel("Sheet " + (i + 1) + ": ");
            JLabel beginRowLbl = new JLabel("From Row: ");
            JLabel endRowLbl = new JLabel("To Row: ");
            JComboBox<String> sheetNameBox = new JComboBox<>(new DefaultComboBoxModel<>(sheetNames.toArray(new String[sheetNames.size()])));
            if (i == 0 && sheetNames.size() > 1) {
                sheetNameBox.setSelectedIndex(1);
            }
            JTextField beginRow = new JTextField();
            JTextField endRow = new JTextField();

            sheetNameMap.put(i, sheetNameBox);
            beginRowMap.put(i, beginRow);
            endRowMap.put(i, endRow);

            panelLayoutParallelGroup.addGroup(panelLayout.createSequentialGroup()
                    .addComponent(sheetNameLbl, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addGap(5).addComponent(sheetNameBox, 150, 150, 150).addGap(5)
                    .addComponent(beginRowLbl).addGap(5).addComponent(beginRow, rowFieldWidth, rowFieldWidth, rowFieldWidth).addGap(5)
                    .addComponent(endRowLbl).addGap(5).addComponent(endRow, rowFieldWidth, rowFieldWidth, rowFieldWidth));

            panelLayoutSequentialGroup.addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(sheetNameLbl).addComponent(sheetNameBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, 30)
                    .addComponent(beginRowLbl).addComponent(beginRow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, 30)
                    .addComponent(endRowLbl).addComponent(endRow, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, 30))
                    .addPreferredGap(ComponentPlacement.RELATED);
        }

        sheetSelectionPanel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(panelLayoutParallelGroup);
        panelLayout.setVerticalGroup(panelLayoutSequentialGroup);
    }

    public void initDetailSettingPanel() {
        detailSettingsPanel = new JPanel();

        GroupLayout panelLayout = new GroupLayout(detailSettingsPanel);
        GroupLayout.ParallelGroup panelLayoutParallelGroup = panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup panelLayoutSequentialGroup = panelLayout.createSequentialGroup();


        panelLayoutParallelGroup.addGroup(panelLayout.createSequentialGroup()
                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(buyerLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lostLimitLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(maxEddLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(codeFinderLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)

                                )
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(buyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(lostLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(maxDaysOverEddComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(finderCodeTextField, labelMinWidth, fieldWidth, fieldWidth)

                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(primeBuyerLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(priceLimitLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(noInvoiceLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(skipCheckLabel, labelMinWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)

                                )
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(primeBuyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(priceLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(noInvoiceTextField, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(skipCheckComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                )
                        )

                ).addContainerGap()
        );


        panelLayoutSequentialGroup.addGroup(panelLayout.createSequentialGroup()
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(buyerLabel)
                        .addComponent(buyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(primeBuyerLabel)
                        .addComponent(primeBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))

                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lostLimitLabel)
                        .addComponent(lostLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(priceLimitLabel)
                        .addComponent(priceLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(maxEddLabel)
                        .addComponent(maxDaysOverEddComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(noInvoiceLabel)
                        .addComponent(noInvoiceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(codeFinderLabel)
                        .addComponent(finderCodeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(skipCheckLabel)
                        .addComponent(skipCheckComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addContainerGap()
        );

        detailSettingsPanel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(panelLayoutParallelGroup);
        panelLayout.setVerticalGroup(panelLayoutSequentialGroup);
    }


    public void initData() {
        Settings systemSettings = Settings.load();
        List<Country> countries = systemSettings.listAllCountries();

        marketplaceComboBox.setModel(new DefaultComboBoxModel<>(countries.toArray(new Country[countries.size()])));
        if (selectedMarketplace == null) {
            selectedMarketplace = marketplaceComboBox.getItemAt(0);
        } else {
            marketplaceComboBox.setSelectedItem(selectedMarketplace);
        }
        setAccounts4Country();

        noInvoiceTextField.setText("{No Invoice}");

        lostLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"5", "7"}));

        priceLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"3", "5"}));

        setOrderFinder();

        maxDaysOverEddComboBox.setModel(new DefaultComboBoxModel<>(
                new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"}
        ));

        maxDaysOverEddComboBox.setSelectedItem("7");

        skipCheckComboBox.setModel(new DefaultComboBoxModel<>(OrderValidator.SkipValidation.values()));

        hideDetails();
    }

    // Variables declaration - do not modify
    private JComboBox<Country> marketplaceComboBox;
    private JComboBox<Account> sellerComboBox;
    private JComboBox<Account> buyerComboBox;
    private JComboBox<Account> primeBuyerComboBox;

    private JComboBox<String> lostLimitComboBox;
    private JComboBox<String> priceLimitComboBox;
    private JTextField noInvoiceTextField;
    private JTextField finderCodeTextField;
    private JButton selectSheetButton;
    private JLabel selectedSheetName;
    private JButton showHideDetailButton;
    private JComboBox<String> maxDaysOverEddComboBox;
    private JComboBox<OrderValidator.SkipValidation> skipCheckComboBox;
    private JLabel buyerLabel;
    private JLabel primeBuyerLabel;
    private JLabel lostLimitLabel;
    private JLabel priceLimitLabel;
    private JLabel noInvoiceLabel;
    private JLabel codeFinderLabel;
    private JLabel maxEddLabel;
    private JLabel skipCheckLabel;
    private JLabel marketplaceLabel;
    private JLabel sellerLabel;
    private JLabel googleSheetLabel;
    private JPanel sheetSelectionPanel;
    private JPanel detailSettingsPanel;

    public Window parentFrame;

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Runtime Settings");
        OrderSubmissionSettingsPanel runtimeSettingsPanel = new OrderSubmissionSettingsPanel(frame);
        frame.getContentPane().add(runtimeSettingsPanel);
        frame.setVisible(true);
        frame.pack();
    }

}
