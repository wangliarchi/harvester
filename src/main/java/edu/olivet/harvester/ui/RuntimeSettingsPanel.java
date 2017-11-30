package edu.olivet.harvester.ui;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.ProgressUpdator;
import edu.olivet.harvester.fulfill.utils.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.utils.OrderValidator;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.dialog.SelectRangeDialog;
import edu.olivet.harvester.ui.events.MarkStatusEvent;
import edu.olivet.harvester.ui.events.SubmitOrdersEvent;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 7:32 PM
 */
public class RuntimeSettingsPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeSettingsPanel.class);

    private Worksheet selectedWorksheet;
    private RuntimeSettings settings;

    private static RuntimeSettingsPanel instance;

    private Thread psEventListenerThread;

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
    }


    public void initData() {

        //RuntimeSettings
        settings = RuntimeSettings.load();
        Settings systemSettings = Settings.load();
        List<Country> countries = systemSettings.listAllCountries();
        settings.setSid(systemSettings.getSid());

        marketplaceComboBox.setModel(new DefaultComboBoxModel<>(countries.toArray(new Country[countries.size()])));
        if (settings.getMarketplaceName() == null) {
            assert marketplaceComboBox.getSelectedItem() != null;
            settings.setMarketplaceName(((Country) marketplaceComboBox.getSelectedItem()).name());
        } else {
            marketplaceComboBox.setSelectedItem(Country.valueOf(settings.getMarketplaceName()));
        }

        setAccounts4Country();

        if (StringUtils.isNotBlank(settings.getSheetName())) {
            googleSheetTextField.setText(settings.getSheetName());
            googleSheetTextField.setToolTipText(settings.getSpreadsheetName() + " - " + settings.getSheetName());
            loadSheetTabButton.setEnabled(true);
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


        skipCheckComboBox.setModel(new DefaultComboBoxModel<>(OrderValidator.SkipValidation.values()));
        if (settings.getSkipValidation() != null) {
            skipCheckComboBox.setSelectedItem(settings.getSkipValidation());
        }

        loadBudget();
        settings.save();
    }

    public void loadBudget() {
        if (StringUtils.isNotBlank(settings.getSpreadsheetId())) {
            Map<String, Float> budgets = ApplicationContext.getBean(DailyBudgetHelper.class).getData(settings.getSpreadsheetId(), new Date());
            todayBudgetTextField.setText(budgets.get("budget").toString());
            todayUsedTextField.setText(budgets.get("cost").toString());
        }

        todayBudgetTextField.setEnabled(false);
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
                if (googleSheetTextField.isEnabled()) {
                    selectGoogleSheet();
                }
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

        finderCodeTextField.addActionListener(e -> finderCodeTextFieldActionPerformed());


        noInvoiceTextField.addActionListener(e -> noInvoiceTextFieldActionPerformed());


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


        skipCheckComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem() != OrderValidator.SkipValidation.None) {
                    skipCheckComboBox.setToolTipText(UIText.tooltip("tooltip.skip.selected", skipCheckComboBox.getSelectedItem()));
                } else {
                    skipCheckComboBox.setToolTipText(null);
                }

                settings.setSkipValidation((OrderValidator.SkipValidation) e.getItem());
                settings.save();
            }
        });

        markStatusButton.addActionListener(evt -> markStatus());

        submitButton.addActionListener(evt -> submitOrders());

        pauseButton.addActionListener(evt -> {
            if (PSEventListener.status == PSEventListener.Status.Paused) {
                PSEventListener.resume();
                resetPauseBtn();
            } else {
                PSEventListener.pause();
                paused();
            }
        });

        stopButton.addActionListener(evt -> {
            if (UITools.confirmed("Please confirm you want to stop this process.")) {
                PSEventListener.stop();
            }
        });

        loadSheetTabButton.addActionListener(evt -> {
            if (StringUtils.isBlank(settings.getSpreadsheetId())) {
                return;
            }
            loadSheetTabButton.setEnabled(false);
            Country country = Country.fromCode(settings.getMarketplaceName());
            Account sellerEmail = Settings.load().getConfigByCountry(country).getSellerEmail();
            BuyerPanel panel = TabbedBuyerPanel.getInstance().addSheetTab(country, sellerEmail);
            TabbedBuyerPanel.getInstance().setSelectedIndex(panel.getId());
            JXBrowserHelper.loadSpreadsheet(panel.getBrowserView().getBrowser(), sellerEmail, settings.getSpreadsheetId());
        });

        psEventListenerThread = new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                switch (PSEventListener.status) {
                    case Paused:
                    case Running:
                        showPauseBtn();
                        break;
                    case Stopped:
                    case Ended:
                        hidePauseBtn();
                        break;
                    case NotRuning:
                        hidePauseBtn();
                        break;
                    default:
                        hidePauseBtn();
                        break;
                }
                WaitTime.Shortest.execute();
            }
        }, "PSEventListener");

    }


    public void markStatus() {
        new Thread(() -> {
            try {
                disableAllBtns();
                ApplicationContext.getBean(MarkStatusEvent.class).excute();
            } catch (Exception e) {
                UITools.error("ERROR while marking order status:" + e.getMessage(), UIText.title("title.code_error"));
                LOGGER.error("ERROR while marking order status:", e);
            } finally {
                restAllBtns();
            }
        }).start();
    }

    public void submitOrders() {
        PSEventListener.reset();
        new Thread(() -> {
            try {
                disableAllBtns();
                ApplicationContext.getBean(SubmitOrdersEvent.class).excute();
            } catch (Exception e) {
                UITools.error(UIText.message("message.submit.exception", e.getMessage()), UIText.title("title.code_error"));
                LOGGER.error("做单过程中出现异常:", e);
            } finally {
                restAllBtns();
            }
        }).start();

        try {
            psEventListenerThread.start();
        } catch (Exception e) {
            //ignore
        }
    }

    public void updateBudget() {
        if (StringUtils.isNotBlank(settings.getSpreadsheetId())) {
            Float budget = Float.parseFloat(todayBudgetTextField.getText());
            ApplicationContext.getBean(DailyBudgetHelper.class).updateBudget(settings.getSpreadsheetId(), new Date(), budget);
        }
    }

    public void showPauseBtn() {
        pauseButton.setVisible(true);
        stopButton.setVisible(true);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);

        huntSupplierButton.setVisible(false);
        markStatusButton.setVisible(false);
        submitButton.setVisible(false);

        progressTextLabel.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setVisible(true);

    }

    public void hidePauseBtn() {
        pauseButton.setVisible(false);
        stopButton.setVisible(false);
        huntSupplierButton.setVisible(true);
        markStatusButton.setVisible(true);
        submitButton.setVisible(true);


    }

    public void paused() {
        pauseButton.setIcon(UITools.getIcon("resume.png"));
        pauseButton.setText("Resume");
    }

    public void resetPauseBtn() {
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setText("Pause");
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
        skipCheckComboBox.setEnabled(false);
        loadSheetTabButton.setEnabled(false);
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
        skipCheckComboBox.setEnabled(true);
        loadSheetTabButton.setEnabled(true);
    }

    public void selectGoogleSheet() {

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

            setAccounts4Country();
            loadSheetTabButton.setEnabled(true);

            loadBudget();

            if (dialog.continueToNext) {
                selectRange();
            }
        }
    }

    public void selectRange() {
        //spreadsheet should be selected first.
        if (StringUtils.isBlank(settings.getSheetName())) {
            return;
        }
        SelectRangeDialog dialog = UITools.setDialogAttr(new SelectRangeDialog(null, true, settings.getAdvancedSubmitSetting()));
        settings = RuntimeSettings.load();
        selectedRangeLabel.setText(settings.getAdvancedSubmitSetting().toString());

        if (dialog.continueToSubmit) {
            submitOrders();
        } else if (dialog.continueToMarkStatus) {
            markStatus();
        }
    }

    public void resetAfterSettingUpdated() {
        if (!Settings.load().listAllSpreadsheets().contains(settings.getSpreadsheetId())) {
            selectedWorksheet = null;
            settings.setSpreadsheetId("");
            settings.setSpreadsheetName("");
            settings.setSheetName("");
            settings.save();

            googleSheetTextField.setText("");
            googleSheetTextField.setToolTipText("");
            loadSheetTabButton.setEnabled(false);
            selectedRangeLabel.setText("");
        }
        setAccounts4Country();
    }

    public void setAccounts4Country() {
        Country currentCountry = (Country) marketplaceComboBox.getSelectedItem();
        Settings.Configuration configuration;
        try {
            configuration = Settings.load().getConfigByCountry(currentCountry);
        } catch (Exception e) {
            settings = new RuntimeSettings();
            settings.save();
            marketplaceComboBox.setSelectedIndex(0);
            return;
        }

        String spreadsheetId = settings.getSpreadsheetId();


        Account seller = configuration.getSeller();
        Account[] sellers = seller == null ? new Account[0] : new Account[]{seller};
        sellerComboBox.setModel(new DefaultComboBoxModel<>(sellers));

        //default to book
        OrderEnums.OrderItemType type;
        if (StringUtils.isBlank(spreadsheetId)) {
            type = OrderItemType.BOOK;
        } else {
            try {
                type = Settings.load().getSpreadsheetType(spreadsheetId);
            } catch (Exception e) {
                type = OrderItemType.BOOK;
            }
        }


        Account primeBuyer;
        Account buyer;
        if (type == OrderEnums.OrderItemType.BOOK) {
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
    private JComboBox<Account> buyerComboBox;
    private JComboBox<Account> primeBuyerComboBox;

    private JButton selectRangeButton;
    private JComboBox<String> lostLimitComboBox;
    private JComboBox<String> priceLimitComboBox;
    private JLabel marketplaceLabel;
    private JLabel sellerLabel;
    private JLabel buyerLabel;
    private JLabel primeBuyerLabel;
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
    private JButton pauseButton;
    private JButton stopButton;

    private JLabel maxEddLabel;
    private JComboBox<String> maxDaysOverEddComboBox;

    private JLabel skipCheckLabel;
    private JComboBox<OrderValidator.SkipValidation> skipCheckComboBox;

    private JLabel progressLabel;
    public JProgressBar progressBar;
    public JLabel progressTextLabel;

    private JLabel todayBudgetLabel;
    private JTextField todayBudgetTextField;
    public JTextField todayUsedTextField;

    private JButton loadSheetTabButton;

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

        pauseButton = new JButton();
        pauseButton.setText("Pause");
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setVisible(false);
        pauseButton.setEnabled(false);
        stopButton = new JButton();
        stopButton.setIcon(UITools.getIcon("stop.png"));
        stopButton.setText("Stop");
        stopButton.setVisible(false);
        stopButton.setEnabled(false);

        loadSheetTabButton = new JButton();
        loadSheetTabButton.setText("Load Sheet");
        loadSheetTabButton.setEnabled(false);

        marketplaceLabel.setText("Marketplace");
        sellerLabel.setText("Seller");
        buyerLabel.setText("Buyer");
        primeBuyerLabel.setText("Prime Buyer");
        googleSheetLabel.setText("Google Sheet");
        selectRangeLabel.setText("Select Range");
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


        todayBudgetLabel = new JLabel();
        todayBudgetLabel.setText("Today's Budget");
        JLabel todayUsedLabel = new JLabel();
        todayUsedLabel.setText("Used");
        todayBudgetTextField = new JTextField();
        todayUsedTextField = new JTextField();
        todayUsedTextField.setEnabled(false);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        int fieldWidth = 200;
        int labelMinWidth = 100;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(sellerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(buyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(primeBuyerLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(googleSheetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(selectRangeLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lostLimitLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(priceLimitLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(maxEddLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(noInvoiceLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(codeFinderLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(skipCheckLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(todayBudgetLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(progressLabel, labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

                                                )
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(sellerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(buyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(primeBuyerComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(googleSheetTextField)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(loadSheetTabButton)

                                                        )
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(selectRangeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(selectedRangeLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                        .addComponent(lostLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(priceLimitComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(maxDaysOverEddComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(noInvoiceTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(finderCodeTextField, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addComponent(skipCheckComboBox, labelMinWidth, fieldWidth, fieldWidth)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(todayBudgetTextField)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(todayUsedLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(todayUsedTextField)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED))
                                                        .addComponent(progressBar, labelMinWidth, fieldWidth, fieldWidth)

                                                )
                                        )
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)

                                                .addGroup(layout.createSequentialGroup()
                                                        .addContainerGap()
                                                        .addComponent(progressTextLabel)
                                                        .addContainerGap()
                                                )
                                                .addGroup(layout.createSequentialGroup()
                                                        .addContainerGap()
                                                        .addComponent(huntSupplierButton)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(markStatusButton)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(submitButton)
                                                        .addContainerGap()
                                                )
                                                .addGroup(layout.createSequentialGroup()
                                                        .addContainerGap()
                                                        .addComponent(pauseButton)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(stopButton)
                                                        .addContainerGap()
                                                ))
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
                                        .addComponent(marketplaceComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sellerLabel)
                                        .addComponent(sellerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerLabel)
                                        .addComponent(buyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(primeBuyerLabel)
                                        .addComponent(primeBuyerComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(googleSheetLabel)
                                        .addComponent(googleSheetTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(loadSheetTabButton)
                                )
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
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(skipCheckLabel)
                                        .addComponent(skipCheckComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))

                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(todayBudgetLabel)
                                        .addComponent(todayBudgetTextField)
                                        .addComponent(todayUsedLabel)
                                        .addComponent(todayUsedTextField)
                                )

                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(huntSupplierButton)
                                        .addComponent(markStatusButton)
                                        .addComponent(submitButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(progressLabel)
                                        .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(progressTextLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(pauseButton)
                                        .addComponent(stopButton))
                                .addContainerGap()

                        )
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
        RuntimeSettingsPanel runtimeSettingsPanel = RuntimeSettingsPanel.getInstance();
        frame.getContentPane().add(runtimeSettingsPanel);
        frame.setVisible(true);
        runtimeSettingsPanel.showPauseBtn();
        ProgressUpdator.success();

    }

}
