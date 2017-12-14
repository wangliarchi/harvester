package edu.olivet.harvester.ui.panel;

import com.google.inject.Singleton;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.OrderSubmitter;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.ui.events.AddOrderSubmissionTaskEvent;
import edu.olivet.harvester.ui.utils.ButtonColumn;
import edu.olivet.harvester.ui.utils.OrderTaskButtonColumn;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/12/17 9:49 AM
 */
@Singleton
public class TasksAndProgressPanel extends JPanel {

    private static TasksAndProgressPanel instance;
    private DBManager dbManager;

    public static TasksAndProgressPanel getInstance() {
        if (instance == null) {
            instance = new TasksAndProgressPanel();
        }
        return instance;
    }

    private TasksAndProgressPanel() {
        dbManager = ApplicationContext.getBean(DBManager.class);
        initComponents();
        initEvents();
    }


    public void initEvents() {
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        addTaskButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                addTaskButtonActionPerformed(evt);
            }
        });

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

        new Thread(() -> {
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
                    case NotRunning:
                        hidePauseBtn();
                        break;
                    default:
                        hidePauseBtn();
                        break;
                }
                WaitTime.Shortest.execute();
            }
        }, "PSEventListener").start();


        taskTable.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int lastIndex = taskTable.getRowCount() - 1;
                taskTable.changeSelection(lastIndex, 0, false, false);
            }
        });

    }


    public void loadTasksToTable() {
        delete = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JTable table = (JTable) e.getSource();
                int modelRow = Integer.valueOf(e.getActionCommand());
                OrderSubmissionTask task = taskList.get(modelRow);

                if (task.getStatus().equalsIgnoreCase(OrderTaskStatus.Stopped.name())) {
                    task.setStatus(OrderTaskStatus.Scheduled.name());
                    task.save(dbManager);
                } else if (UITools.confirmed("Please confirm that you want to delete this task.")) {
                    task.setStatus(OrderTaskStatus.Deleted.name());
                    task.save(dbManager);
                }

            }
        };

        taskList = dbManager.query(OrderSubmissionTask.class,
                Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                        .asc("dateCreated"));

        ListModel<OrderSubmissionTask> listModel = new ListModel<>("Order Submission Tasks", taskList, OrderSubmissionTask.COLUMNS, null, OrderSubmissionTask.WIDTHS);

        taskTable.setModel(new DefaultTableModel(listModel.toTableData(), listModel.getColumns()));

        if (listModel.getWidths() != null) {
            for (int i = 0; i < listModel.getWidths().length; i++) {
                taskTable.getColumnModel().getColumn(i).setPreferredWidth(listModel.getWidths()[i]);
            }
        }

        ButtonColumn buttonColumn = new OrderTaskButtonColumn(taskTable, delete, 8);
        buttonColumn.setMnemonic(KeyEvent.VK_D);
    }

    private void addTaskButtonActionPerformed(ActionEvent evt) {
        ApplicationContext.getBean(AddOrderSubmissionTaskEvent.class).execute();
    }


    private void startButtonActionPerformed(ActionEvent evt) {
        new Thread(() -> {
            startButton.setEnabled(false);
            while (true) {
                if (PSEventListener.stopped()) {
                    break;
                }

                try {
                    List<OrderSubmissionTask> scheduledTasks = dbManager.query(OrderSubmissionTask.class,
                            Cnd.where("dateCreated", ">=", Dates.beginOfDay(new DateTime()).toDate())
                                    .and("status", "=", OrderTaskStatus.Scheduled.name())
                                    .asc("dateCreated"));
                    if (CollectionUtils.isEmpty(scheduledTasks)) {
                        startButton.setEnabled(true);
                        UITools.info("No more tasks to run");
                        break;
                    }

                    PSEventListener.reset();

                    OrderSubmissionTask task = scheduledTasks.get(0);
                    RuntimeSettings runtimeSettings = task.convertToRuntimeSettings();
                    runtimeSettings.save();
                    runtimeSettingsPanel.initData();
                    runtimeSettingsPanel.setVisible(true);
                    ApplicationContext.getBean(OrderSubmitter.class).execute(task);

                } catch (Exception e) {
                    UITools.error(e.getMessage());
                    break;
                } finally {
                    startButton.setEnabled(true);
                    loadTasksToTable();
                }
            }
        }).start();
    }


    public void showPauseBtn() {
        pauseButton.setVisible(true);
        stopButton.setVisible(true);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        runtimeSettingsPanel.showProgressBar();
    }

    public void hidePauseBtn() {
        pauseButton.setVisible(false);
        stopButton.setVisible(false);
    }

    public void paused() {
        pauseButton.setIcon(UITools.getIcon("resume.png"));
        pauseButton.setText("Resume");
    }

    public void resetPauseBtn() {
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setText("Pause");
    }


    // Variables declaration - do not modify
    private JButton addTaskButton;
    private JButton startButton;
    private JScrollPane jScrollPane1;
    private JTable taskTable;
    private JButton pauseButton;
    private JButton stopButton;
    private Action delete;
    private RuntimeSettingsPanel runtimeSettingsPanel = RuntimeSettingsPanel.getInstance();
    private List<OrderSubmissionTask> taskList;
    // End of variables declaration

    private void initComponents() {

        setBorder(BorderFactory.createTitledBorder("Tasks & Progress"));

        jScrollPane1 = new JScrollPane();
        taskTable = new JTable();
        addTaskButton = new JButton();
        startButton = new JButton();
        pauseButton = new JButton();
        stopButton = new JButton();

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.LEADING);
        taskTable.setFont(new Font(addTaskButton.getFont().getName(), Font.PLAIN, 11));
        taskTable.setColumnSelectionAllowed(false);
        taskTable.setRowSelectionAllowed(true);
        loadTasksToTable();


        jScrollPane1.setViewportView(taskTable);

        addTaskButton.setText("Add Task");

        startButton.setText("Start");
        startButton.setIcon(UITools.getIcon("start.png"));


        pauseButton.setText("Pause");
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setVisible(false);
        pauseButton.setEnabled(false);

        stopButton.setIcon(UITools.getIcon("stop.png"));
        stopButton.setText("Stop");
        stopButton.setVisible(false);
        stopButton.setEnabled(false);


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                        .addComponent(runtimeSettingsPanel)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addGroup(layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(pauseButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(stopButton)
                                        .addComponent(startButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(addTaskButton)
                                        .addContainerGap()))

        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(runtimeSettingsPanel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(pauseButton)
                                        .addComponent(stopButton)
                                        .addComponent(addTaskButton)
                                        .addComponent(startButton))
                        )

        );
    }


    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("");
        frame.setSize(400, 580);
        TasksAndProgressPanel tasksAndProgressPanel = new TasksAndProgressPanel();
        frame.getContentPane().add(tasksAndProgressPanel);
        frame.setVisible(true);
        tasksAndProgressPanel.showPauseBtn();
    }
}

