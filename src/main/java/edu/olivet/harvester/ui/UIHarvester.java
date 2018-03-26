package edu.olivet.harvester.ui;

import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.bugreport.service.ReportBugEvent;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.common.model.ConfigEnums;
import edu.olivet.harvester.ui.dialog.BankCardConfigDialog;
import edu.olivet.harvester.ui.dialog.BuyerAccountConfigDialog;
import edu.olivet.harvester.ui.dialog.SystemSettingsDialog;
import edu.olivet.harvester.ui.events.*;
import edu.olivet.harvester.ui.menu.Actions;
import edu.olivet.harvester.ui.menu.UIElements;
import edu.olivet.harvester.ui.panel.MainPanel;
import edu.olivet.harvester.utils.LogViewer;
import edu.olivet.harvester.utils.MessageListener;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:59 AM
 */
public class UIHarvester extends AbstractApplicationUI {
    private static final Logger LOGGER = LoggerFactory.getLogger(UIHarvester.class);
    @SuppressWarnings("FieldCanBeLocal")
    private JTextPane statusPane;

    private static final String APP_TITLE = "Harvester: Automated Order Fulfillment - %s";

    public UIHarvester() {
        this.initComponents();
        UIElements.getInstance().registerListener(new ActionController(this, UIElements.getInstance()));
    }


    private void initComponents() {
        setDefaultSizes();
        setTitleAndIcon();
        registerCloseEvent();

        final JMenuBar menuBar = UIElements.getInstance().createMenuBar();
        this.setJMenuBar(menuBar);
        final JToolBar toolbar = UIElements.getInstance().createToolBar();
        final MemoryUsageBar memoryUsageBar = new MemoryUsageBar();
        statusPane = new JTextPane();
        statusPane.setEditable(false);

        JPanel mainPanel = MainPanel.getInstance();
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(toolbar))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(statusPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addComponent(memoryUsageBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, 200))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.CENTER)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(toolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(memoryUsageBar, 20, 20, 20).addComponent(statusPane, 20, 20, 20))
                        ));
        pack();
    }

    @Inject HuntSuppliersEvent huntSuppliersEvent;

    @UIEvent
    public void findSupplier() {
        huntSuppliersEvent.run();
    }

    @Inject private
    ConfirmShipmentEvent confirmShipmentEvent;

    @UIEvent
    public void confirmShipment() {
        confirmShipmentEvent.execute();
    }

    @Inject private
    OrderConfirmationHistoryEvent orderConfirmationHistoryEvent;

    @UIEvent
    public void orderConfirmationHistory() {
        orderConfirmationHistoryEvent.execute();
    }

    @UIEvent
    public void checkStoreName() {
        Settings settings = Settings.reload();

        MessagePanel messagePanel = new ProgressDetail(Actions.CheckStoreName);
        for (Configuration config : settings.getConfigs()) {
            String storeName = config.getStoreNameFromWeb();


            if (StringUtils.isNotBlank(storeName)) {
                if (!storeName.equalsIgnoreCase(config.getStoreName())) {
                    messagePanel.displayMsg(config.getCountry() + " store name: " + config.getStoreName() + ", name from web: " + storeName, InformationLevel.Negative);
                    config.setStoreName(storeName);
                    messagePanel.displayMsg("updated!", InformationLevel.Information);
                } else {
                    messagePanel.displayMsg(config.getCountry() + " store name " + config.getStoreName() + " is correct", InformationLevel.Information);
                }
            } else {
                if (config.getStoreName().length() <= 3) {
                    messagePanel.displayMsg(config.getCountry() + " store name: " + config.getStoreName() + " seems not valid, please double check.", InformationLevel.Negative);
                } else {
                    messagePanel.displayMsg(config.getCountry() + " store name: " + config.getStoreName() + ". Fail to get store name on website.", InformationLevel.Negative);
                }
            }
        }

        settings.saveToFile();
    }

    @Inject private
    OrderSubmissionLogEvent orderSubmissionLogEvent;

    @UIEvent
    public void orderSubmissionLog() {
        orderSubmissionLogEvent.execute();
    }

    @Inject private
    LogViewer logViewer;

    @UIEvent
    public void orderSuccessLog() {
        logViewer.displayLogs(ConfigEnums.Log.Success);
    }

    @UIEvent
    public void orderStatisticLog() {
        logViewer.displayLogs(ConfigEnums.Log.Statistic);
    }

    @Inject CommonLettersEvent commonLettersEvent;

    @UIEvent
    public void commonLetters() {
        commonLettersEvent.execute();
    }

    @Inject CheckPrimeBuyerAccountEvent checkPrimeBuyerAccountEvent;

    @UIEvent
    public void checkPrimeBuyerAccount() {
        checkPrimeBuyerAccountEvent.execute();
    }


    @Inject private
    ListOrderSubmissionTasks listOrderSubmissionTasks;

    @UIEvent
    public void orderSubmissionTasks() {
        listOrderSubmissionTasks.execute();
    }

    @Inject AsyncASINsEvent asyncASINsEvent;

    @UIEvent
    public void syncASINs() {
        asyncASINsEvent.execute();
    }

    @Inject DownloadInventoryEvent downloadInventoryEvent;

    @UIEvent
    public void downloadInventory() {
        downloadInventoryEvent.execute();
    }

    @UIEvent
    public void runDownloadInvoiceTask() {
        downloadInvoiceEvent.runTasks();
    }

    @UIEvent
    public void invoiceTasks() {
        downloadInvoiceEvent.list();
    }

    @Inject private
    SettingEvent settingEvent;

    @UIEvent
    public void settings() {
        settingEvent.execute();
        this.setTitle(String.format(APP_TITLE, Settings.load().getSid()));
    }

    @UIEvent
    public void systemSettings() {
        SystemSettingsDialog dialog = UITools.setDialogAttr(new SystemSettingsDialog());
    }

    @UIEvent
    public void configBankCard() {
        BankCardConfigDialog dialog = UITools.setDialogAttr(new BankCardConfigDialog());
        if (dialog.isOk()) {
            UITools.info("Credit card info has been saved successfully.");
        }
    }


    @Inject private
    ReportBugEvent reportBugEvent;

    @UIEvent
    public void reportBug() {
        reportBugEvent.execute();
    }


    @Inject private
    AddOrderSubmissionTaskEvent addOrderSubmissionTaskEvent;

    @UIEvent
    public void addOrderTask() {
        addOrderSubmissionTaskEvent.execute();
    }

    @UIEvent
    public void submitOrder() {
        addOrderSubmissionTaskEvent.execute();
    }

    @Inject private
    ExportOrderEvent exportOrderEvent;

    @UIEvent
    public void exportOrders() {
        exportOrderEvent.execute();
    }

    @Inject private
    TitleCheckerEvent titleCheckerEvent;

    @UIEvent
    public void titleChecker() {
        titleCheckerEvent.execute();
    }

    @Inject OrderInfoCheckerEvent orderInfoCheckerEvent;

    @UIEvent
    public void orderChecker() {
        orderInfoCheckerEvent.execute();
    }

    @UIEvent
    public void configBuyerAccount() {
        BuyerAccountConfigDialog dialog = UITools.setDialogAttr(new BuyerAccountConfigDialog());
        if (dialog.isOk()) {
            UITools.info("Buyer account settings have been saved successfully.");
        }
    }

    @Inject DownloadInvoiceEvent downloadInvoiceEvent;

    @UIEvent
    public void downloadInvoice() {
        downloadInvoiceEvent.execute();
    }

    protected void setDefaultSizes() {
        this.setResizable(true);
        this.setMinimumSize(new Dimension(800, 600));
        //maximized by default
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    protected void setTitleAndIcon() {
        try {
            this.setTitle(String.format(APP_TITLE, Settings.load().getSid()));
        } catch (Exception e) {
            this.setTitle(APP_TITLE);
        }
        UITools.setIcon(this, "harvester.png");
    }

    protected void registerCloseEvent() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (PSEventListener.isRunning()) {
                    UITools.error("Order submission task is running. Please wait util it's finished, or stop the task first.");
                    return;
                }
                if (!UITools.confirmed("Are you sure to close Harvester? \nBackground jobs may be running.")) {
                    return;
                }

                orderSubmissionTaskService.cleanUp();
                System.exit(0);
            }
        });
    }


    @UIEvent
    public void restart() {

        if (PSEventListener.isRunning()) {
            UITools.error("Order submission task is running. Please wait util it's finished, or stop the task first.");
            return;
        }
        if (!UITools.confirmed("Are you sure to close Harvester? \nBackground jobs may be running.")) {
            return;
        }

        orderSubmissionTaskService.cleanUp();

        UITools.restart(this.getApplication(), this);
    }

    @Inject OrderFulfillmentCheckerEvent orderFulfillmentCheckerEvent;

    @UIEvent
    public void orderFulfillmentChecker() {
        orderFulfillmentCheckerEvent.execute();
    }

    @Inject SubmitSelfOrdersEvent submitSelfOrdersEvent;

    @UIEvent
    public void submitSelfOrders() {
        submitSelfOrdersEvent.execute();
    }

    @Override
    public String getApplication() {
        return Harvester.APP_NAME;
    }

    @Inject private OrderSubmissionTaskService orderSubmissionTaskService;

    @Override
    public void cleanUp() {
        orderSubmissionTaskService.cleanUp();
    }


    @Inject
    private TaskScheduler taskScheduler;

    @Inject MessageListener messageListener;

    void startBackgroundJobs() {
        for (BackgroundJob job : BackgroundJob.values()) {
            Date nextTriggerTime = taskScheduler.startJob(job.getCron(), job.getClazz());
            try {
                AbstractBackgroundJob bg = ApplicationContext.getBean(job.getClazz());
                bg.runIfMissed(nextTriggerTime);
                if (bg.enabled()) {
                    messageListener.addMsg("Next trigger time for background job " + job.name() + " will be " + nextTriggerTime);
                }
            } catch (Exception e) {
                LOGGER.error("Cant initialize job {}", job.getClazz().getName(), e);
            }
        }
        taskScheduler.createOSTask();
    }


    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new UIHarvester(), true);
    }

}
