package edu.olivet.harvester.ui;

import com.amazonaws.mws.model.FeedSubmissionInfo;
import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.job.TaskScheduler;
import edu.olivet.foundations.ui.*;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.ConfirmShipments;
import edu.olivet.harvester.feeds.model.OrderConfirmationLog;
import edu.olivet.harvester.job.BackgroundJob;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.utils.SettingValidator;
import edu.olivet.harvester.utils.Settings;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:59 AM
 */
public class UIHarvester extends AbstractApplicationUI {
    private static final Logger LOGGER = LoggerFactory.getLogger(UIHarvester.class);
    @SuppressWarnings("FieldCanBeLocal")
    private JTextPane statusPane;
    @Inject
    private DBManager dbManager;

    @Inject
    private SheetAPI sheetAPI;

    public UIHarvester() {
        this.initComponents();
        UIElements.getInstance().registerListener(new ActionController(this, UIElements.getInstance()));
        this.settings = Settings.load();
    }


    private void initComponents() {
        this.setResizable(true);
        this.setTitle("Harvester: Automated Order Fulfillment");
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

    @UIEvent
    public void submitOrder() {

    }

    @UIEvent
    public void findSupplier() {

    }

    @Inject
    private ConfirmShipments confirmShipments;
    @Inject
    private AppScript appScript;

    @UIEvent
    public void confirmShipment() {

        long start = System.currentTimeMillis();

        LOGGER.info("Confirm shipment button clicked");

        List<String> spreadsheetIds = Settings.load().listAllSpreadsheets();
        List<edu.olivet.harvester.spreadsheet.Spreadsheet> spreadsheets = new ArrayList<>();

        StringBuilder spreadsheetIdError = new StringBuilder();
        for (String spreadsheetId : spreadsheetIds) {
            try {
                Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
                spreadsheets.add(spreadsheet);
            } catch (Exception e) {
                LOGGER.error("{} is invalid. {}", spreadsheetId, e.getMessage());
                spreadsheetIdError.append(String.format("%s is not a valid spreadsheet id, or it's not shared to %s \n",
                    spreadsheetId, Constants.RND_EMAIL));
            }
        }

        if (!spreadsheetIdError.toString().isEmpty()) {
            UITools.error(spreadsheetIdError.toString(), "Error");
        }

        LOGGER.info("All spreadsheets loaded in {}", Strings.formatElapsedTime(start));

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));

        if (dialog.isOk()) {


            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            List<String> sheetNames = selectedWorksheets.stream().map(Worksheet::getSheetName).collect(Collectors.toList());

            try {
                List<FeedSubmissionInfo> submissionInfo = confirmShipments.getUnprocessedFeedSubmission(selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetCountry());


                if (submissionInfo.size() > 0) {
                    StringBuilder submissions = new StringBuilder();
                    submissionInfo.forEach(it -> submissions.append(String.format("FeedSubmissionId %s submitted at %s, current status %s \n", it.getFeedSubmissionId(), it.getSubmittedDate(), it.getFeedProcessingStatus())));
                    String msg = String.format("Unprocessed/processing order confirmation feed(s) found.  \n\n %s \n\n" +
                        " Are you sure to submit again?", submissions.toString());
                    if (!UITools.confirmed(msg)) {
                        return;
                    }

                }
            } catch (Exception e) {
                LOGGER.error("Failed to load unprocessed feed submissions for {} - {}", selectedWorksheets.get(0).getSpreadsheet().getSpreadsheetCountry(), e.getMessage());
            }

            confirmShipments.setMessagePanel(new ProgressDetail(Actions.ConfirmShipment));


            confirmShipments.getMessagePanel().displayMsg(
                selectedWorksheets.size() + " worksheets from " + selectedWorksheets.get(0).getSpreadsheet().getTitle() +
                    " selected to confirm shipments - " +
                    String.join(",", sheetNames), LOGGER, InformationLevel.Information);


            confirmShipments.confirmShipmentForWorksheets(selectedWorksheets);
        }
    }


    @UIEvent
    public void orderConfirmationHistory() {
        List<OrderConfirmationLog> list = dbManager.query(OrderConfirmationLog.class,
            Cnd.where("context", "!=", "").desc("uploadTime"));

        ListModel<OrderConfirmationLog> dialog = new ListModel<>(Actions.OrderConfirmationHistory.label(), list, OrderConfirmationLog.COLUMNS, null, OrderConfirmationLog.WIDTHS);
        UITools.displayListDialog(dialog);
    }


    private Settings settings;

    @UIEvent
    public void settings() {
        SettingsDialog dialog = UITools.setDialogAttr(new SettingsDialog(new SettingValidator(new AppScript(), sheetAPI)));
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

    @Inject
    private TaskScheduler taskScheduler;

    void startBackgroundJobs() {
        for (BackgroundJob job : BackgroundJob.values()) {
            taskScheduler.startJob(job.getCron(), job.getClazz());
            try {
                AbstractBackgroundJob bg = ApplicationContext.getBean(job.getClazz());
                bg.runIfMissed();
            } catch (Exception e) {
                LOGGER.error("Cant initialize job {}", job.getClazz().getName(),e);
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
