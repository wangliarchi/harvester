package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.spreadsheet.model.OrderRange;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.utils.FinderCodeUtils;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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

    public OrderSubmissionSettingsPanel() {
        initComponents();
        initData();
        initEvents();
    }


    public void initData() {
        Settings systemSettings = Settings.load();
        List<Country> countries = systemSettings.listAllCountries();

        marketplaceComboBox.setModel(new DefaultComboBoxModel<>(countries.toArray(new Country[countries.size()])));
        if (selectedMarketplace == null) {
            selectedMarketplace = marketplaceComboBox.getItemAt(0);
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
        Account[] buyers = buyer == null ? new Account[0] : new Account[]{buyer};
        buyerComboBox.setModel(new DefaultComboBoxModel<>(buyers));

        Account[] primeBuyers = primeBuyer == null ? new Account[0] : new Account[]{primeBuyer};
        primeBuyerComboBox.setModel(new DefaultComboBoxModel<>(primeBuyers));

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
    }


    public void clearSubmitRange() {
        for (int i = 0; i < MAX_COUNT; i++) {
            beginRowMap.get(i).setText("");
            endRowMap.get(i).setText("");
        }
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


    private JComboBox<String> maxDaysOverEddComboBox;

    private JComboBox<OrderValidator.SkipValidation> skipCheckComboBox;


    @Getter
    private List<OrderRange> orderRanges;
    private final Map<Integer, JComboBox<String>> sheetNameMap = new LinkedHashMap<>();
    private final Map<Integer, JTextField> beginRowMap = new HashMap<>();
    private final Map<Integer, JTextField> endRowMap = new HashMap<>();
    private static final int MAX_COUNT = 7;
    @Setter
    @Getter
    private List<String> sheetNames = new ArrayList<>();


    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("Runtime Settings")));

        JLabel marketplaceLabel = new JLabel();
        marketplaceComboBox = new JComboBox<>();

        JLabel sellerLabel = new JLabel();
        sellerComboBox = new JComboBox<>();

        JLabel buyerLabel = new JLabel();
        buyerComboBox = new JComboBox<>();
        JLabel primeBuyerLabel = new JLabel();
        primeBuyerComboBox = new JComboBox<>();

        JLabel googleSheetLabel = new JLabel();

        JLabel lostLimitLabel = new JLabel();
        lostLimitComboBox = new JComboBox<>();
        JLabel priceLimitLabel = new JLabel();
        priceLimitComboBox = new JComboBox<>();
        JLabel noInvoiceLabel = new JLabel();
        noInvoiceTextField = new JTextField();
        JLabel codeFinderLabel = new JLabel();
        finderCodeTextField = new JTextField();

        JLabel maxEddLabel = new JLabel();
        maxDaysOverEddComboBox = new JComboBox<>();

        selectSheetButton = new JButton();
        selectSheetButton.setText("Select");
        selectedSheetName = new JLabel();
        selectedSheetName.setForeground(Color.BLUE);
        Font font = selectedSheetName.getFont();
        selectedSheetName.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 2));

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


        JLabel skipCheckLabel = new JLabel();
        skipCheckLabel.setText("Skip Check");
        skipCheckLabel.setForeground(Color.RED);


        skipCheckComboBox = new JComboBox<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            skipCheckComboBox.setFont(new Font(font.getName(), Font.BOLD, font.getSize() - 1));
        }


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        GroupLayout.ParallelGroup hPG = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup vSG = layout.createSequentialGroup();


        int fieldWidth = 200;
        int labelMinWidth = 100;

        hPG.addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(marketplaceLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(buyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lostLimitLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(maxEddLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(codeFinderLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                )
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(marketplaceComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(buyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(lostLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(maxDaysOverEddComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(finderCodeTextField, labelMinWidth, fieldWidth, fieldWidth)

                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(sellerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(primeBuyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(priceLimitLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(noInvoiceLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(skipCheckLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                )
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(sellerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(primeBuyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(priceLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(noInvoiceTextField, labelMinWidth, fieldWidth, fieldWidth)
                                        .addComponent(skipCheckComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                )
                        )
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(googleSheetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(selectSheetButton, 100, 100, 100)
                                .addComponent(selectedSheetName)
                        )
                ).addContainerGap()
        );


        vSG.addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(marketplaceLabel)
                        .addComponent(marketplaceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(sellerLabel)
                        .addComponent(sellerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(googleSheetLabel)
                        .addComponent(selectSheetButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(selectedSheetName)
                )
                .addContainerGap()
        );


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

            hPG.addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(sheetNameLbl).addGap(5).addComponent(sheetNameBox, 150, 150, 150).addGap(5)
                    .addComponent(beginRowLbl).addGap(5).addComponent(beginRow, rowFieldWidth, rowFieldWidth, rowFieldWidth).addGap(5)
                    .addComponent(endRowLbl).addGap(5).addComponent(endRow, rowFieldWidth, rowFieldWidth, rowFieldWidth));

            vSG.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(sheetNameLbl).addComponent(sheetNameBox, 30, 30, 30)
                    .addComponent(beginRowLbl).addComponent(beginRow, 30, 30, 30)
                    .addComponent(endRowLbl).addComponent(endRow, 30, 30, 30))
                    .addGap(5);
        }


        vSG.addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)

                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(buyerLabel)
                        .addComponent(buyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(primeBuyerLabel)
                        .addComponent(primeBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))

                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(lostLimitLabel)
                        .addComponent(lostLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(priceLimitLabel)
                        .addComponent(priceLimitComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(maxEddLabel)
                        .addComponent(maxDaysOverEddComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(noInvoiceLabel)
                        .addComponent(noInvoiceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(codeFinderLabel)
                        .addComponent(finderCodeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(skipCheckLabel)
                        .addComponent(skipCheckComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                )
                .addContainerGap()
        );

        layout.setHorizontalGroup(hPG);
        layout.setVerticalGroup(vSG);


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

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Runtime Settings");
        frame.setSize(600, 580);
        OrderSubmissionSettingsPanel runtimeSettingsPanel = new OrderSubmissionSettingsPanel();
        frame.getContentPane().add(runtimeSettingsPanel);
        frame.setVisible(true);
    }

}
