package edu.olivet.harvester.ui;

import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.ui.dialog.BankCardConfigDialog;
import edu.olivet.harvester.ui.events.ConfirmShipmentEvent;
import edu.olivet.harvester.ui.events.OrderConfirmationHistoryEvent;
import edu.olivet.harvester.ui.events.SettingEvent;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
        this.setMinimumSize(new Dimension(800,600));
        //maximized by default
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        this.setTitle(String.format(APP_TITLE,Settings.load().getSid()));
        UITools.setIcon(this, "harvester.png");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
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

        JPanel mainPanel = new MainPanel();
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        //.addGroup(layout.createSequentialGroup()
                        //        .addComponent(toolbar))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(mainPanel))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(statusPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addComponent(memoryUsageBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, 200))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.CENTER)
                        .addGroup(layout.createSequentialGroup()
                                //.addComponent(toolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(memoryUsageBar, 20, 20, 20).addComponent(statusPane, 20, 20, 20))
                        ));
        pack();
    }


    @UIEvent
    public void submitOrder() {

    }



    @UIEvent
    public void findSupplier() {

    }


    @Inject
    ConfirmShipmentEvent confirmShipmentEvent;

    @UIEvent
    public void confirmShipment() {
        confirmShipmentEvent.excute();
    }

    @Inject
    OrderConfirmationHistoryEvent orderConfirmationHistoryEvent;

    @UIEvent
    public void orderConfirmationHistory() {
        orderConfirmationHistoryEvent.excute();
    }


    @Inject
    SettingEvent settingEvent;

    @UIEvent
    public void settings() {
        settingEvent.excute();
        this.setTitle(String.format(APP_TITLE,Settings.load().getSid()));
    }


    @UIEvent
    public void configBankCard() {
        BankCardConfigDialog dialog = UITools.setDialogAttr(new BankCardConfigDialog());

        if (dialog.isOk()) {
            UITools.info("Credit card info has been saved successfully.");
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
            taskScheduler.startJob(job.getCron(), job.getClazz());
            try {
                AbstractBackgroundJob bg = ApplicationContext.getBean(job.getClazz());
                bg.runIfMissed();
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
