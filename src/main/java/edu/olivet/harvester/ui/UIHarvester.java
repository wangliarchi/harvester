package edu.olivet.harvester.ui;

import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.*;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.utils.Settings;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:59 AM
 */
public class UIHarvester extends AbstractApplicationUI {
    private JTextPane statusPane;

    public UIHarvester() {
        this.initComponents();
        UIElements.getInstance().registerListener(new ActionController(this, UIElements.getInstance()));
        this.settings = Settings.load();
    }

    private void initComponents() {
        this.setResizable(true);
        this.setTitle("Harvester: Automated Order Fulfillment");
        UITools.setIcon(this, "harvester.png");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final JMenuBar menuBar = UIElements.getInstance().createMenuBar();
        this.setJMenuBar(menuBar);
        final JToolBar toolbar = UIElements.getInstance().createToolBar();
        final MemoryUsageBar memoryUsageBar = new MemoryUsageBar();
        statusPane = new JTextPane();
        statusPane.setEditable(false);

        JLabel icon = new JLabel(UITools.getIcon("harvester.jpg"));
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(toolbar))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(icon))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(statusPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                    .addComponent(memoryUsageBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, 200))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(toolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(icon, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(memoryUsageBar, 24, 24, 24).addComponent(statusPane))
                ));
        pack();
    }

    @UIEvent public void submitOrder() {

    }

    @UIEvent public void findSupplier() {

    }

    @UIEvent public void confirmShipment() {

    }

    private Settings settings;

    @UIEvent public void settings() {
        SettingsDialog dialog = UITools.setDialogAttr(new SettingsDialog());
        if (dialog.isOk()) {
            this.settings = dialog.getSettings();
        }
    }

    @Override
    public String getApplication() {
        return Harvester.APP_NAME;
    }

    @Override
    public void cleanUp() {

    }

    @Inject private TaskScheduler taskScheduler;

    void startBackgroundJobs() {
        for (BackgroundJob job : BackgroundJob.values()) {
            taskScheduler.startJob(job.getCron(), job.getClazz());
        }

        taskScheduler.createOSTask();
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new UIHarvester(), true);
    }
}
