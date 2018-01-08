package edu.olivet.harvester.ui;

import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.bugreport.service.ReportBugEvent;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.ui.dialog.BankCardConfigDialog;
import edu.olivet.harvester.ui.dialog.BuyerAccountConfigDialog;
import edu.olivet.harvester.ui.dialog.SystemSettingsDialog;
import edu.olivet.harvester.ui.events.*;
import edu.olivet.harvester.utils.LogViewer;
import edu.olivet.harvester.utils.Settings;
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

        this.setResizable(true);
        this.setMinimumSize(new Dimension(800, 600));
        //maximized by default
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        try {
            this.setTitle(String.format(APP_TITLE, Settings.load().getSid()));
        } catch (Exception e) {
            this.setTitle(APP_TITLE);
        }
        UITools.setIcon(this, "harvester.png");

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
                System.exit(0);
            }
        });

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


    @Inject
    ConfirmShipmentEvent confirmShipmentEvent;

    @UIEvent
    public void confirmShipment() {
        confirmShipmentEvent.execute();
    }

    @Inject
    OrderConfirmationHistoryEvent orderConfirmationHistoryEvent;

    @UIEvent
    public void orderConfirmationHistory() {
        orderConfirmationHistoryEvent.execute();
    }

    @Inject
    OrderSubmissionLogEvent orderSubmissionLogEvent;

    @UIEvent
    public void orderSubmissionLog() {
        orderSubmissionLogEvent.execute();
    }

    @Inject
    LogViewer logViewer;

    @UIEvent
    public void orderSuccessLog() {
        logViewer.displayLogs(ConfigEnums.Log.Success);
    }

    @UIEvent
    public void orderStatisticLog() {
        logViewer.displayLogs(ConfigEnums.Log.Statistic);
    }

    @Inject
    ListOrderSubmissionTasks listOrderSubmissionTasks;

    @UIEvent
    public void orderSubmissionTasks() {
        listOrderSubmissionTasks.execute();
    }

    @Inject
    SettingEvent settingEvent;

    @UIEvent
    public void settings() {
        settingEvent.execute();
        this.setTitle(String.format(APP_TITLE, Settings.load().getSid()));
    }

    @UIEvent
    public void systemSettings() {
        SystemSettingsDialog dialog = UITools.setDialogAttr(new SystemSettingsDialog());
        if (dialog.isOk()) {
            //
        }
    }

    @UIEvent
    public void configBankCard() {
        BankCardConfigDialog dialog = UITools.setDialogAttr(new BankCardConfigDialog());
        if (dialog.isOk()) {
            UITools.info("Credit card info has been saved successfully.");
        }
    }


    @Inject
    ReportBugEvent reportBugEvent;

    @UIEvent
    public void reportBug() {
        reportBugEvent.execute();
    }


    @Inject
    AddOrderSubmissionTaskEvent addOrderSubmissionTaskEvent;

    @UIEvent
    public void addOrderTask() {
        addOrderSubmissionTaskEvent.execute();
    }

    @UIEvent
    public void submitOrder() {
        addOrderSubmissionTaskEvent.execute();
    }

    @Inject
    ExportOrderEvent exportOrderEvent;

    @UIEvent
    public void exportOrders() {
        exportOrderEvent.execute();
    }

    @Inject
    TitleCheckerEvent titleCheckerEvent;

    @UIEvent
    public void titleChecker() {
        titleCheckerEvent.execute();
    }

    @UIEvent
    public void configBuyerAccount() {
        BuyerAccountConfigDialog dialog = UITools.setDialogAttr(new BuyerAccountConfigDialog());
        if (dialog.isOk()) {
            UITools.info("Buyer account settings have been saved successfully.");
        }
    }

    @Override
    public String getApplication() {
        return Harvester.APP_NAME;
    }

    @Override
    public void cleanUp() {

    }

    @Inject
    private TaskScheduler taskScheduler;

    void startBackgroundJobs() {
        for (BackgroundJob job : BackgroundJob.values()) {
            Date nextTriggerTime = taskScheduler.startJob(job.getCron(), job.getClazz());
            try {
                AbstractBackgroundJob bg = ApplicationContext.getBean(job.getClazz());
                bg.runIfMissed(nextTriggerTime);
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
