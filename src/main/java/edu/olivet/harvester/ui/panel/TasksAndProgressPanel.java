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
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.service.PSEventHandler;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.spreadsheet.service.AppScript;
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
public class TasksAndProgressPanel extends JPanel implements PSEventHandler {

    private static TasksAndProgressPanel instance;
    private DBManager dbManager;
    private OrderSubmissionTaskService orderSubmissionTaskService;

    public static TasksAndProgressPanel getInstance() {
        if (instance == null) {
            instance = new TasksAndProgressPanel();
        }
        return instance;
    }

    private TasksAndProgressPanel() {
        dbManager = ApplicationContext.getBean(DBManager.class);
        orderSubmissionTaskService = ApplicationContext.getBean(OrderSubmissionTaskService.class);
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
            if (PSEventListener.paused()) {
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
                int modelRow = Integer.valueOf(e.getActionCommand());
                OrderSubmissionTask task = taskList.get(modelRow);

                if (task.getStatus().equalsIgnoreCase(OrderTaskStatus.Stopped.name())) {
                    task.setStatus(OrderTaskStatus.Scheduled.name());
                    orderSubmissionTaskService.saveTask(task);
                } else if (task.getStatus().equalsIgnoreCase(OrderTaskStatus.Completed.name())) {
                    OrderSubmissionTask newTask = task.copy();
                    newTask.setStatus(OrderTaskStatus.Scheduled.name());
                    orderSubmissionTaskService.saveTask(newTask);
                } else if (UITools.confirmed("Please confirm that you want to delete this task.")) {
                    task.setStatus(OrderTaskStatus.Deleted.name());
                    orderSubmissionTaskService.saveTask(task);
                }

            }
        };

        taskList = orderSubmissionTaskService.todayTasks();

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
            if (PSEventListener.isRunning()) {
                UITools.error("Other task is running!");
                return;
            }

            startButton.setEnabled(false);
            while (true) {
                try {
                    List<OrderSubmissionTask> scheduledTasks = orderSubmissionTaskService.todayScheduledTasks();

                    if (CollectionUtils.isEmpty(scheduledTasks)) {
                        startButton.setEnabled(true);
                        UITools.info("No more tasks to run");
                        break;
                    }

                    PSEventListener.reset(this);

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
                    PSEventListener.reset(this);
                    loadTasksToTable();
                }
            }

            startButton.setEnabled(true);
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
                        .addComponent(jScrollPane1, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
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
                                .addComponent(jScrollPane1, 150, GroupLayout.PREFERRED_SIZE, 300)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(runtimeSettingsPanel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(pauseButton)
                                        .addComponent(stopButton)
                                        .addComponent(addTaskButton)
                                        .addComponent(startButton))
                                .addContainerGap()
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

