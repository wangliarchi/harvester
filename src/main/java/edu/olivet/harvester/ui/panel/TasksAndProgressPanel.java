package edu.olivet.harvester.ui.panel;

import com.google.inject.Singleton;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.model.OrderSubmissionBuyerAccountTask;
import edu.olivet.harvester.fulfill.model.OrderSubmissionTask;
import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import edu.olivet.harvester.fulfill.service.OrderSubmissionTaskService;
import edu.olivet.harvester.fulfill.service.PSEventHandler;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.ui.events.AddOrderSubmissionTaskEvent;
import edu.olivet.harvester.ui.events.StartOrderSubmissionTasksEvent;
import edu.olivet.harvester.ui.utils.ButtonColumn;
import edu.olivet.harvester.ui.utils.OrderTaskButtonColumn;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/12/17 9:49 AM
 */
@Singleton
public class TasksAndProgressPanel extends JPanel implements PSEventHandler {

    private static TasksAndProgressPanel instance;
    private OrderSubmissionTaskService orderSubmissionTaskService;

    public static TasksAndProgressPanel getInstance() {
        if (instance == null) {
            instance = new TasksAndProgressPanel();
        }
        return instance;
    }

    private TasksAndProgressPanel() {
        orderSubmissionTaskService = ApplicationContext.getBean(OrderSubmissionTaskService.class);
        initComponents();
        initEvents();
    }


    private void initEvents() {
        startButton.addActionListener(this::startButtonActionPerformed);

        addTaskButton.addActionListener(this::addTaskButtonActionPerformed);

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
    }


    private void addTaskButtonActionPerformed(ActionEvent evt) {
        ApplicationContext.getBean(AddOrderSubmissionTaskEvent.class).execute();
    }


    private void startButtonActionPerformed(ActionEvent evt) {
        ApplicationContext.getBean(StartOrderSubmissionTasksEvent.class).execute();
    }


    public void showPauseBtn() {
        pauseButton.setVisible(true);
        stopButton.setVisible(true);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        //runtimeSettingsPanel.showProgressBar();
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

    public void disableStartButton() {
        startButton.setEnabled(false);
    }

    public void enableStartButton() {
        startButton.setEnabled(true);
    }

    public void disableStopButton() {
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
    }
    // Variables declaration - do not modify
    private JButton addTaskButton;
    private JButton startButton;
    private JTable taskTable;
    private JButton pauseButton;
    private JButton stopButton;
    private ProgressBarPanel runtimeSettingsPanel = ProgressBarPanel.getInstance();
    private List<OrderSubmissionTask> taskList;
    private List<OrderSubmissionBuyerAccountTask> buyerTaskList;

    // End of variables declaration

    private void initComponents() {

        setBorder(BorderFactory.createTitledBorder("Tasks & Progress"));

        JScrollPane tableScrollPane1 = new JScrollPane();
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
        tableScrollPane1.setViewportView(taskTable);


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

        runtimeSettingsPanel.setVisible(false);
        loadTasksToTable();

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(tableScrollPane1, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
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
                                .addComponent(tableScrollPane1, 150, GroupLayout.PREFERRED_SIZE, 300)
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


    private final Action delete = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            int modelRow = Integer.valueOf(e.getActionCommand());
            OrderSubmissionTask task = taskList.get(modelRow);
            OrderTaskStatus taskStatus = task.taskStatus();
            switch (taskStatus) {
                case Stopped:
                    task.setTaskStatus(OrderTaskStatus.Scheduled);
                    task.setFailed(0);
                    task.setSuccess(0);
                    orderSubmissionTaskService.saveTask(task);
                    break;
                case Completed:
                    task.setTaskStatus(OrderTaskStatus.Retried);
                    orderSubmissionTaskService.saveTask(task);

                    OrderSubmissionTask newTask = task.copy();
                    orderSubmissionTaskService.saveTask(newTask);
                    break;
                case Scheduled:
                    if (UITools.confirmed("Please confirm that you want to delete this task.")) {
                        orderSubmissionTaskService.deleteTask(task);
                    }
                    break;
                case Processing:
                case Queued:
                    task.setTaskStatus(OrderTaskStatus.Stopped);
                    orderSubmissionTaskService.saveTask(task);
                    break;
                default:
                    break;
            }

            loadTasksToTable();


        }
    };

    public synchronized void loadTasksToTable() {

        try {
            taskList = orderSubmissionTaskService.todayTasks();
            ListModel<OrderSubmissionTask> listModel =
                    new ListModel<>("Order Submission Tasks", taskList, OrderSubmissionTask.COLUMNS, null, OrderSubmissionTask.WIDTHS);
            taskTable.setModel(new DefaultTableModel(listModel.toTableData(), listModel.getColumns()));

            if (listModel.getWidths() != null && listModel.getWidths().length > 0) {
                for (int i = 0; i < listModel.getWidths().length; i++) {
                    taskTable.getColumnModel().getColumn(i).setPreferredWidth(listModel.getWidths()[i]);
                }
            }

            ButtonColumn buttonColumn = new OrderTaskButtonColumn(taskTable, delete, listModel.getColumns().length - 1);
            buttonColumn.setMnemonic(KeyEvent.VK_D);

            taskTable.changeSelection(taskTable.getRowCount() - 1, 0, false, false);
        } catch (Exception e) {
            //
        }

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

