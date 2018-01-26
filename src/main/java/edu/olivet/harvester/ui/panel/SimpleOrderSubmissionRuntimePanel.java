package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.fulfill.model.setting.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.*;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator;
import edu.olivet.harvester.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.ui.dialog.SelectRangeDialog;
import edu.olivet.harvester.ui.events.MarkStatusEvent;
import edu.olivet.harvester.ui.events.SubmitOrdersEvent;
import edu.olivet.harvester.utils.FinderCodeUtils;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
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
public class SimpleOrderSubmissionRuntimePanel extends JPanel implements PSEventHandler, RuntimePanelObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOrderSubmissionRuntimePanel.class);

    private Worksheet selectedWorksheet;
    private RuntimeSettings settings;

    private static SimpleOrderSubmissionRuntimePanel instance;

    public static SimpleOrderSubmissionRuntimePanel getInstance() {
        if (instance == null) {
            instance = new SimpleOrderSubmissionRuntimePanel();
        }

        return instance;
    }

    private SimpleOrderSubmissionRuntimePanel() {
        initComponents();
        initData();
        initEvents();
    }


    private void initData() {

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


        lostLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"5", "7"}));
        if (StringUtils.isNotBlank(settings.getLostLimit())) {
            lostLimitComboBox.setSelectedItem(settings.getLostLimit());
        }

        priceLimitComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"3", "5"}));
        if (StringUtils.isNotBlank(settings.getPriceLimit())) {
            priceLimitComboBox.setSelectedItem(settings.getPriceLimit());
        }

        setOrderFinder();
        if (StringUtils.isBlank(settings.getFinderCode())) {
            settings.setFinderCode(finderCodeTextField.getText());
        }
        finderCodeTextField.setText(settings.getFinderCode());


        maxDaysOverEddComboBox.setModel(new DefaultComboBoxModel<>(
                new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"}
        ));
        if (StringUtils.isNotBlank((settings.getEddLimit()))) {
            maxDaysOverEddComboBox.setSelectedItem(settings.getEddLimit());
        }


        skipCheckComboBox.setModel(new DefaultComboBoxModel<>(OrderValidator.SkipValidation.values()));
        if (settings.getSkipValidation() != null) {
            skipCheckComboBox.setSelectedItem(settings.getSkipValidation());
        }

        //loadBudget();
        settings.save();

        todayBudgetTextField.setEnabled(false);
    }

    @Override
    public void updateSpending(String spending) {
        todayUsedTextField.setText(spending);
    }

    @Override
    public void updateBudget(String budget) {
        todayBudgetTextField.setText(budget);
    }

    public void loadBudget() {
        if (StringUtils.isNotBlank(settings.getSpreadsheetId())) {
            Map<String, Float> budgets = ApplicationContext.getBean(DailyBudgetHelper.class)
                    .getData(settings.getSpreadsheetId(), new Date());
            todayBudgetTextField.setText(budgets.get("budget").toString());
            todayUsedTextField.setText(budgets.get("cost").toString());
        }


    }

    private void initEvents() {
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

        buyerComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                settings.setBuyerEmail(((Account) e.getItem()).getEmail());
                settings.save();
            }
        });
        primeBuyerComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                settings.setPrimeBuyerEmail(((Account) e.getItem()).getEmail());
                settings.save();
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
            if (PSEventListener.paused()) {
                PSEventListener.resume();
            } else {
                PSEventListener.pause();
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


    }

    private void resetSkipSetting() {
        skipCheckComboBox.setSelectedIndex(0);
        settings.setSkipValidation(OrderValidator.SkipValidation.None);
        settings.save();
    }

    private void markStatus() {
        new Thread(() -> {
            try {
                disableAllBtns();
                ApplicationContext.getBean(MarkStatusEvent.class).execute();
            } catch (Exception e) {
                UITools.error("ERROR while marking order status:" + e.getMessage(), UIText.title("title.code_error"));
                LOGGER.error("ERROR while marking order status:", e);
            } finally {
                restAllBtns();
            }
        }).start();
    }

    private void submitOrders() {
        new Thread(() -> {
            if (PSEventListener.isRunning()) {
                UITools.error("Other task is running!");
                return;
            }
            try {
                disableAllBtns();
                saveRuntimeSettings();
                PSEventListener.reset(this);
                ApplicationContext.getBean(SubmitOrdersEvent.class).execute();
            } catch (Exception e) {
                UITools.error(UIText.message("message.submit.exception", e.getMessage()), UIText.title("title.code_error"));
                LOGGER.error("做单过程中出现异常:", e);
            } finally {
                resetSkipSetting();
                PSEventListener.end();
            }
        }).start();


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

    public void showProgressBar() {
        progressTextLabel.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setVisible(true);
    }

    @Override
    public void disableStartButton() {
        disableAllBtns();
    }

    @Override
    public void enableStartButton() {
        restAllBtns();
    }

    public void paused() {
        pauseButton.setIcon(UITools.getIcon("resume.png"));
        pauseButton.setText("Resume");
    }

    public void resetPauseBtn() {
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setText("Pause");
    }

    private void disableAllBtns() {
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
        buyerComboBox.setEnabled(false);
        primeBuyerComboBox.setEnabled(false);
        buyerComboBox.setEnabled(false);
    }

    private void restAllBtns() {
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
        buyerComboBox.setEnabled(true);
        primeBuyerComboBox.setEnabled(true);
        buyerComboBox.setEnabled(true);
    }

    private void selectGoogleSheet() {

        AppScript appScript = new AppScript();
        Country selectedCountry = (Country) marketplaceComboBox.getSelectedItem();
        List<Spreadsheet> spreadsheets = Settings.load().listSpreadsheets(selectedCountry, appScript);

        if (CollectionUtils.isEmpty(spreadsheets)) {
            UITools.error("No order update sheet found. Please make sure it's configured and shared with " + Constants.RND_EMAIL, "Error");
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

    private void selectRange() {
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
        Settings systemSettings = Settings.load();
        List<Country> countries = systemSettings.listAllCountries();
        settings.setSid(systemSettings.getSid());

        marketplaceComboBox.setModel(new DefaultComboBoxModel<>(countries.toArray(new Country[countries.size()])));

        if (!systemSettings.listAllSpreadsheets().contains(settings.getSpreadsheetId())) {
            selectedWorksheet = null;
            settings.setSpreadsheetId("");
            settings.setSpreadsheetName("");
            settings.setSheetName("");
            settings.setBuyerEmail("");
            settings.setPrimeBuyerEmail("");
            settings.save();

            googleSheetTextField.setText("");
            googleSheetTextField.setToolTipText("");
            loadSheetTabButton.setEnabled(false);
            selectedRangeLabel.setText("");
        }
        setAccounts4Country();
    }

    private void setAccounts4Country() {
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
        Account[] sellers = seller == null ? new Account[0] : new Account[] {seller};
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

        if (StringUtils.isNotBlank(settings.getBuyerEmail()) && settings.getCurrentCountry() == currentCountry) {
            try {
                buyer = BuyerAccountSettingUtils.load().getByEmail(settings.getBuyerEmail()).getBuyerAccount();
            } catch (Exception e) {
                //buyer from setting file may be removed from buyer settings
            }
        }

        if (StringUtils.isNotBlank(settings.getPrimeBuyerEmail()) && settings.getCurrentCountry() == currentCountry) {
            try {
                primeBuyer = BuyerAccountSettingUtils.load().getByEmail(settings.getPrimeBuyerEmail()).getBuyerAccount();
            } catch (Exception e) {
                //buyer from setting file may be removed from buyer settings
            }
        }

        List<Account> buyerAccounts = BuyerAccountSettingUtils.load().getAccounts(currentCountry, type, false);
        buyerComboBox.setModel(new DefaultComboBoxModel<>(buyerAccounts.toArray(new Account[buyerAccounts.size()])));
        buyerComboBox.setSelectedItem(buyer);

        List<Account> primeBuyerAccounts = BuyerAccountSettingUtils.load().getAccounts(currentCountry, type, true);
        primeBuyerComboBox.setModel(new DefaultComboBoxModel<>(primeBuyerAccounts.toArray(new Account[primeBuyerAccounts.size()])));
        primeBuyerComboBox.setSelectedItem(primeBuyer);


    }

    private void setOrderFinder() {
        Country currentCountry = (Country) marketplaceComboBox.getSelectedItem();
        Settings.Configuration configuration = Settings.load().getConfigByCountry(currentCountry);
        if (FinderCodeUtils.validate(configuration.getUserCode())) {
            finderCodeTextField.setText(configuration.getUserCode());
        } else {
            finderCodeTextField.setText(FinderCodeUtils.generate());
        }
    }

    private void saveRuntimeSettings() {
        assert (buyerComboBox.getSelectedItem()) != null;
        settings.setBuyerEmail(((Account) buyerComboBox.getSelectedItem()).getEmail());
        assert (primeBuyerComboBox.getSelectedItem()) != null;
        settings.setPrimeBuyerEmail(((Account) primeBuyerComboBox.getSelectedItem()).getEmail());
        settings.setFinderCode(finderCodeTextField.getText());
        settings.setNoInvoiceText(noInvoiceTextField.getText());
        assert (marketplaceComboBox.getSelectedItem()) != null;
        settings.setMarketplaceName(((Country) marketplaceComboBox.getSelectedItem()).name());

        if (selectedWorksheet != null) {
            settings.setSheetName(selectedWorksheet.getSheetName());
            settings.setSpreadsheetId(selectedWorksheet.getSpreadsheet().getSpreadsheetId());
            settings.setSpreadsheetName(selectedWorksheet.getSpreadsheet().getTitle());
        }

        settings.setSkipValidation((OrderValidator.SkipValidation) skipCheckComboBox.getSelectedItem());
        settings.setEddLimit((String) maxDaysOverEddComboBox.getSelectedItem());
        settings.setLostLimit((String) lostLimitComboBox.getSelectedItem());
        settings.setPriceLimit((String) priceLimitComboBox.getSelectedItem());
        settings.save();
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
        if (!FinderCodeUtils.validate(finderCodeTextField.getText())) {
            String defaultCode = Settings.load().getConfigByCountry((Country) marketplaceComboBox.getSelectedItem()).getUserCode();
            if (FinderCodeUtils.validate(defaultCode)) {
                finderCodeTextField.setText(defaultCode);
                UITools.error("Finder code is invalid. User code in setting is used.");
            } else {
                finderCodeTextField.setText(FinderCodeUtils.generate());
                UITools.error("Finder code is invalid. System generated one for you.");
            }
        }
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
    private JLabel selectedRangeLabel;
    private JTextField googleSheetTextField;
    private JTextField noInvoiceTextField;
    private JTextField finderCodeTextField;

    private JButton huntSupplierButton;
    private JButton markStatusButton;
    private JButton submitButton;
    private JButton pauseButton;
    private JButton stopButton;

    private JComboBox<String> maxDaysOverEddComboBox;

    private JComboBox<OrderValidator.SkipValidation> skipCheckComboBox;

    private JLabel progressLabel;
    public JProgressBar progressBar;
    public JLabel progressTextLabel;

    private JTextField todayBudgetTextField;
    private JTextField todayUsedTextField;

    private JButton loadSheetTabButton;

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("Runtime Settings")));

        JLabel marketplaceLabel = new JLabel("Marketplace");
        marketplaceComboBox = new JComboBox<>();

        JLabel sellerLabel = new JLabel("Seller");
        sellerComboBox = new JComboBox<>();

        JLabel buyerLabel = new JLabel("Buyer");
        buyerComboBox = new JComboBox<>();
        JLabel primeBuyerLabel = new JLabel("Prime Buyer");
        primeBuyerComboBox = new JComboBox<>();

        JLabel googleSheetLabel = new JLabel("Google Sheet");
        googleSheetTextField = new JTextField();
        JLabel selectRangeLabel = new JLabel("Select Range");
        selectRangeButton = new JButton();
        JLabel lostLimitLabel = new JLabel("Lost Limit");
        lostLimitComboBox = new JComboBox<>();
        JLabel priceLimitLabel = new JLabel("Price Limit");
        priceLimitComboBox = new JComboBox<>();
        JLabel noInvoiceLabel = new JLabel("No Invoice");
        noInvoiceTextField = new JTextField();
        JLabel codeFinderLabel = new JLabel("Finder Code");
        finderCodeTextField = new JTextField();
        selectedRangeLabel = new JLabel();
        selectedRangeLabel.setForeground(Color.BLUE);

        JLabel maxEddLabel = new JLabel("EDD Limit");
        maxDaysOverEddComboBox = new JComboBox<>();

        huntSupplierButton = new JButton("Hunt Supplier");
        huntSupplierButton.setIcon(UITools.getIcon("find.png"));
        markStatusButton = new JButton("Mark Status");
        markStatusButton.setIcon(UITools.getIcon("status.png"));
        submitButton = new JButton("Submit");
        submitButton.setIcon(UITools.getIcon("start.png"));

        pauseButton = new JButton("Pause");
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setVisible(false);
        pauseButton.setEnabled(false);
        stopButton = new JButton("Stop");
        stopButton.setIcon(UITools.getIcon("stop.png"));
        stopButton.setVisible(false);
        stopButton.setEnabled(false);

        loadSheetTabButton = new JButton("Load Sheet");
        loadSheetTabButton.setEnabled(false);

        JLabel skipCheckLabel = new JLabel("Skip Check");
        skipCheckLabel.setForeground(Color.RED);


        skipCheckComboBox = new JComboBox<>();
        Font font = skipCheckLabel.getFont();
        if (SystemUtils.IS_OS_WINDOWS) {
            skipCheckComboBox.setFont(new Font(font.getName(), Font.BOLD, font.getSize() - 1));
        }

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressLabel = new JLabel("Progress");

        progressTextLabel = new JLabel("progress text placeholder");
        progressTextLabel.setForeground(Color.BLUE);
        progressTextLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 2));

        progressTextLabel.setVisible(false);
        progressLabel.setVisible(false);
        progressBar.setVisible(false);


        JLabel todayBudgetLabel = new JLabel("Today's Budget");
        JLabel todayUsedLabel = new JLabel("Used");
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
                                                        .addComponent(marketplaceLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(sellerLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(buyerLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(primeBuyerLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(googleSheetLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(selectRangeLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lostLimitLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(priceLimitLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(maxEddLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(noInvoiceLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(codeFinderLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(skipCheckLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(todayBudgetLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(progressLabel,
                                                                labelMinWidth, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)

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
                                                                .addComponent(selectRangeButton,
                                                                        GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(selectedRangeLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        )
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
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)

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
                                        .addComponent(marketplaceComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sellerLabel)
                                        .addComponent(sellerComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(buyerLabel)
                                        .addComponent(buyerComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(primeBuyerLabel)
                                        .addComponent(primeBuyerComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(googleSheetLabel)
                                        .addComponent(googleSheetTextField)
                                        .addComponent(loadSheetTabButton)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(selectRangeLabel)
                                        .addComponent(selectRangeButton)
                                        .addComponent(selectedRangeLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lostLimitLabel)
                                        .addComponent(lostLimitComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(priceLimitLabel)
                                        .addComponent(priceLimitComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(maxEddLabel)
                                        .addComponent(maxDaysOverEddComboBox))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(noInvoiceLabel)
                                        .addComponent(noInvoiceTextField))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(codeFinderLabel)
                                        .addComponent(finderCodeTextField))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(skipCheckLabel)
                                        .addComponent(skipCheckComboBox))

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
                                        .addComponent(progressBar))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(progressTextLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(pauseButton)
                                        .addComponent(stopButton))
                                .addContainerGap()

                        )
        );


    }


    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("Runtime Settings");
        frame.setSize(400, 580);
        SimpleOrderSubmissionRuntimePanel runtimeSettingsPanel = SimpleOrderSubmissionRuntimePanel.getInstance();
        frame.getContentPane().add(runtimeSettingsPanel);
        frame.setVisible(true);
        runtimeSettingsPanel.showPauseBtn();
        ProgressUpdater.success();

    }

}