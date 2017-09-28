package edu.olivet.harvester.ui;

import com.google.api.services.drive.model.File;
import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.ui.Action;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:59 AM
 */
public class UIHarvester extends AbstractApplicationUI {
    private static final Logger LOGGER = LoggerFactory.getLogger(UIHarvester.class);
    private JTextPane statusPane;

//    @Getter
//    private JTextArea successLogTextArea;

    //private static final int DEFAULT_HEIGHT = 820;

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

//        int screenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
//        int height = screenHeight >= DEFAULT_HEIGHT ? DEFAULT_HEIGHT : screenHeight;
//        int sucHeight = height < DEFAULT_HEIGHT ? 180 : 220;
//        int logLayoutHeight = height < DEFAULT_HEIGHT ? 210 : 250;

//        successLogTextArea = UIElements.getInstance().createLogTextArea(Color.BLACK);
//        successLogTextArea.append("Welcome to Harvester!"+Constants.NEW_LINE);
//        final JPanel successLogPanel = UIElements.getInstance().createLogPanel(UIText.title("title.record.success"),sucHeight,successLogTextArea);

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
//                .addGroup(layout.createSequentialGroup()
//                        .addComponent(successLogPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(statusPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)

                    .addComponent(memoryUsageBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, 200))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(toolbar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(icon, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
//                    .addComponent(successLogPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)

                        .addComponent(memoryUsageBar, 24, 24, 24).addComponent(statusPane))
                ));
        pack();
    }

    @UIEvent public void submitOrder() {

    }

    @UIEvent public void findSupplier() {

    }

    @Inject
    private ConfirmShipments confirmShipments;
    @Inject
    private AppScript appScript;

    @UIEvent
    public void confirmShipment() {

        long start = System.currentTimeMillis();

        LOGGER.info("Confirm shipment button clicked at {}",start);

        List<String> spreadsheetIds = Settings.load().listAllSpreadsheets();
        List<edu.olivet.harvester.spreadsheet.Spreadsheet> spreadsheets = new ArrayList<>();

        for(String spreadsheetId : spreadsheetIds) {
            try {
                Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
                spreadsheets.add(spreadsheet);
            } catch (Exception e) {
                LOGGER.warn( "{} is invalid. {}",spreadsheetId,e.getMessage());
            }
        }

        LOGGER.info("All spreadsheets loaded in {}", Strings.formatElapsedTime(start));

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets,appScript));


        long start1 = System.currentTimeMillis();

        if (dialog.isOk()) {

            confirmShipments.setMessagePanel(new ProgressDetail(Actions.ConfirmShipment));

            List<Worksheet> selectedWorksheets = dialog.getSelectedSheets();
            List<String> sheetNames = new ArrayList<>();
            selectedWorksheets.forEach(it -> {sheetNames.add(it.getSheetName());});

            confirmShipments.getMessagePanel().displayMsg(selectedWorksheets.size()+" worksheets from "+selectedWorksheets.get(0).getSpreadsheet().getTitle()
                    +" selected to confirm shipments - "
                    + String.join(",", sheetNames), LOGGER,InformationLevel.Information);


            confirmShipments.confirmShipmentForWorksheets(selectedWorksheets);
        }
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
