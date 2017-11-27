package edu.olivet.harvester.ui.dialog;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 1:01 PM
 */
public class LogViewDialog extends JDialog {
    /**
     * 成功日志各列枚举定义
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Jan 5, 2015 8:26:18 AM
     */
    enum SuccessLogColumn {
        Row("行号", Integer.class, 1),
        OrderId("原单号", String.class, 2),
        ISBN("ISBN", String.class, 3),
        SellerName("Seller名称", String.class, 4),
        SellerId("SellerID", String.class, 5),
        Cost("费用", Float.class, 6),
        OrderNumber("订单号", String.class, 7),
        Account("做单账号", String.class, 8),
        LastCode("运费差值", Float.class, 9),
        Profit("估计利润", Float.class, 10),
        Remark("批注", String.class, 11),
        Timestamp("时间", Date.class, 12),
        Context("环境", String.class, 13);

        private String label;
        private Class<?> type;
        private int sortNo;
        SuccessLogColumn(String label, Class<?> type, int sortNo) {
            this.label = label;
            this.type = type;
            this.sortNo = sortNo;
        }
        public String getLabel() {
            return label;
        }
        public Class<?> getType() {
            return type;
        }
        public int getSortNo() {
            return sortNo;
        }

        public static String[] columnNames() {
            SuccessLogColumn[] values = SuccessLogColumn.values();
            String[] result = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i].getLabel();
            }
            return result;
        }

        public static Class<?>[] columnTypes() {
            SuccessLogColumn[] values = SuccessLogColumn.values();
            Class<?>[] result = new Class<?>[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i].getType();
            }
            return result;
        }
    }
    /**
     * 统计日志各列枚举定义
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Jan 5, 2015 8:26:28 AM
     */
    enum StatisticLogColumn {
        Date("统计日期", String.class, 1),
        Count("订单总数", Integer.class, 2),
        Success("成功记录", Integer.class, 3),
        Fail("失败记录", Integer.class, 4),
        Skip("略过记录", Integer.class, 5),
        TimeCost("平均每单耗时/秒", Float.class, 6),
        SuccessRate("成功率", String.class, 7);

        private String label;
        private Class<?> type;
        private int sortNo;
        StatisticLogColumn(String label, Class<?> type, int sortNo) {
            this.label = label;
            this.type = type;
            this.sortNo = sortNo;
        }
        public String getLabel() {
            return label;
        }
        public Class<?> getType() {
            return type;
        }
        public int getSortNo() {
            return sortNo;
        }

        public static String[] columnNames() {
            StatisticLogColumn[] values = StatisticLogColumn.values();
            String[] result = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i].getLabel();
            }
            return result;
        }

        public static Class<?>[] columnTypes() {
            StatisticLogColumn[] values = StatisticLogColumn.values();
            Class<?>[] result = new Class<?>[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i].getType();
            }
            return result;
        }
    }

    private static final long serialVersionUID = 8527571063009083949L;
    private static String[] SUCCESS_COLUMN_NAMES;
    private static String[] STAT_COLUMN_NAMES;
    private DefaultTableModel successLogTableModel;
    private DefaultTableModel statLogTableModel;
    private final String context;
    private DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

    public LogViewDialog(Frame parent, boolean modal, String context) {
        super(parent, modal);
        this.context = context;
        centerRenderer.setHorizontalAlignment( JLabel.CENTER );
        initComponents();
    }

    private void renderSuccessLogs() {
        TableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = -3019658361622151644L;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Date) {
                    value = Dates.format((Date) value, DateFormat.DATE_TIME.pattern());
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

            @Override
            protected void setValue(Object value) {
                if (value instanceof Date) {
                    value = Dates.format((Date) value, DateFormat.DATE_TIME.pattern());
                }
                super.setValue(value);
            }
        };

        SUCCESS_COLUMN_NAMES = SuccessLogColumn.columnNames();
        Object[][] data = getSuccessLogData();
        int rowCount = data.length;
        int colColunt = SUCCESS_COLUMN_NAMES.length;
        successLogTableModel = new DefaultTableModel(rowCount, colColunt) {
            private static final long serialVersionUID = 3728339236601985972L;

            Class<?>[] types = SuccessLogColumn.columnTypes();

            public Class<?> getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        };

        successLogTableModel.setDataVector(data, SUCCESS_COLUMN_NAMES);
        successLogTable =  new JTable(successLogTableModel) {
            private static final long serialVersionUID = -7996493405307614317L;

            @Override
            public String getToolTipText(@NotNull MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);

                if (realColumnIndex == SuccessLogColumn.OrderId.ordinal() ||
                        realColumnIndex == SuccessLogColumn.SellerName.ordinal() ||
                        realColumnIndex == SuccessLogColumn.Account.ordinal() ||
                        realColumnIndex == SuccessLogColumn.Remark.ordinal() ||
                        realColumnIndex == SuccessLogColumn.Timestamp.ordinal() ||
                        realColumnIndex == SuccessLogColumn.Context.ordinal()) {
                    String tip = this.getModel().getValueAt(rowIndex, realColumnIndex).toString();
                    if (StringUtils.isNotBlank(tip)) {
                        return tip;
                    }
                }
                return super.getToolTipText(e);
            }
        };
        this.setSuccessColWidths();
        successLogTable.getColumnModel().getColumn(SuccessLogColumn.Timestamp.ordinal()).setCellRenderer(tableCellRenderer);
    }

    private void renderStatLogs() {
        STAT_COLUMN_NAMES = StatisticLogColumn.columnNames();
        Object[][] statData = getStatLogData();
        statLogTableModel = new DefaultTableModel(statData.length, STAT_COLUMN_NAMES.length) {
            private static final long serialVersionUID = 3688940866473584300L;
            Class<?>[] types = StatisticLogColumn.columnTypes();
            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        };
        statLogTableModel.setDataVector(statData, STAT_COLUMN_NAMES);
        statisticLogTable = new JTable(statLogTableModel);
        this.setStatColWidths();
    }

    private void centered() {
        for (int i = 0; i < SUCCESS_COLUMN_NAMES.length; i++) {
            successLogTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        for (int i = 0; i < STAT_COLUMN_NAMES.length; i++) {
            statisticLogTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void setSuccessColWidths() {
        successLogTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        successLogTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        successLogTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        successLogTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        successLogTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        successLogTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        successLogTable.getColumnModel().getColumn(6).setPreferredWidth(210);
        successLogTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        successLogTable.getColumnModel().getColumn(8).setPreferredWidth(80);
    }

    private void setStatColWidths() {
        statisticLogTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        statisticLogTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        statisticLogTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        statisticLogTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        statisticLogTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        statisticLogTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        statisticLogTable.getColumnModel().getColumn(6).setPreferredWidth(100);
    }

    private Object[][] getStatLogData() {
        java.util.List<String> logs;
        try {
            File file = ConfigEnums.Log.Statistic.file();
            if (!file.exists()) {
                return new Object[0][STAT_COLUMN_NAMES.length];
            }

            logs = FileUtils.readLines(file, Constants.UTF8);
            if (CollectionUtils.isEmpty(logs)) {
                return new Object[0][STAT_COLUMN_NAMES.length];
            }
        } catch (IOException e) {
            UITools.error(UIText.message("message.error.read", ConfigEnums.Log.Statistic.desc(), e.getMessage()), UIText.title("title.code_error"));
            return new Object[0][STAT_COLUMN_NAMES.length];
        }

        Collections.reverse(logs);

        java.util.List<Object[]> list = new ArrayList<>(logs.size());
        for (String s : logs) {
            String[] arr = StringUtils.splitPreserveAllTokens(s, '\t');
            if (arr.length != STAT_COLUMN_NAMES.length - 1) {
                continue;
            }

            Object[] row = new Object[STAT_COLUMN_NAMES.length];
            for (int i = 0; i < arr.length; i++) {
                if (i == StatisticLogColumn.Date.ordinal()) {
                    row[i] = arr[i];
                } else if (i == StatisticLogColumn.TimeCost.ordinal()) {
                    row[i] = Float.parseFloat(arr[i]);
                } else {
                    row[i] = Integer.parseInt(arr[i]);
                }
            }

            int success = (int) row[2];
            int fail = (int) row[3];
            int skip = (int) row[4];
            int realCount = success + fail + skip;
            if (realCount > 0) {
                row[6] = success * 100 / realCount + "%";
            } else {
                row[6] = StringUtils.EMPTY;
            }

            if (success == 0 && fail == 0 && skip == 0) {
                continue;
            }
            list.add(row);
        }

        return list.toArray(new Object[list.size()][STAT_COLUMN_NAMES.length]);
    }

    /**
     * 按照context筛选过滤日志记录，只显示当前环境下的成功日志记录
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Dec 18, 2014 5:21:31 PM
     */
    class ContextPredicate implements Predicate {
        @Override
        public boolean evaluate(Object object) {
            String line = (String)object;
            String[] arr = StringUtils.splitPreserveAllTokens(line, '\t');
            if (arr.length >= 14) {
                // 对过往的日志记录作兼容处理
                String srcContext = arr[13].replace("-", StringUtils.EMPTY);
                // 目标context有效的前提下进行筛选过滤
                if (StringUtils.isNotBlank(context) && !StringUtils.contains(srcContext, context)) {
                    return false;
                }
            }
            return true;
        }
    }
    private ContextPredicate contextPredicate = new ContextPredicate();

    private Object[][] getSuccessLogData() {
        List<String> logs;
        try {
            File file = ConfigEnums.Log.Success.file();
            if (!file.exists()) {
                return new Object[0][SUCCESS_COLUMN_NAMES.length];
            }

            logs = FileUtils.readLines(file, Constants.UTF8);
            CollectionUtils.filter(logs, contextPredicate);
            if (CollectionUtils.isEmpty(logs)) {
                return new Object[0][SUCCESS_COLUMN_NAMES.length];
            }
        } catch (IOException e) {
            UITools.error(UIText.message("message.error.read", ConfigEnums.Log.Success.desc(), e.getMessage()), UIText.title("title.code_error"));
            return new Object[0][SUCCESS_COLUMN_NAMES.length];
        }

        Collections.reverse(logs);
        Object[][] data = new Object[logs.size()][SUCCESS_COLUMN_NAMES.length];
        int index = 0;
        for (String s : logs) {
            String[] arr = StringUtils.splitPreserveAllTokens(s, '\t');
            Object[] row = new Object[SUCCESS_COLUMN_NAMES.length];
            // 略过status列
            for (int i = 0; i < arr.length; i++) {
                if (i - 1 >= SUCCESS_COLUMN_NAMES.length) {
                    break;
                }

                if (i == 0) {
                    row[i] = Integer.parseInt(arr[i]);
                } else if (i - 1 == SuccessLogColumn.Cost.ordinal() ||
                        i - 1 == SuccessLogColumn.LastCode.ordinal() ||
                        i - 1 == SuccessLogColumn.Profit.ordinal()) {
                    row[i - 1] = Float.parseFloat(arr[i]);
                } else if (i - 1 == SuccessLogColumn.Timestamp.ordinal()) {
                    try {
                        row[i - 1] = Dates.parseDate(arr[i]);
                    } catch (Exception e) {
                        // -> Ignore
                    }
                } else if (i > 1) {
                    row[i - 1] = arr[i];
                }
            }
            data[index++] = row;
        }

        return data;
    }

    private void initComponents() {
        logPane = new JTabbedPane();
        successLogPanel = new JPanel();
        successScroll = new JScrollPane();
        statDataPanel = new JPanel();
        statScroll = new JScrollPane();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                setVisible(false);
                dispose();
            }
        });

        logPane.setBorder(BorderFactory.createTitledBorder("查看日志"));

        renderStatLogs();
        renderSuccessLogs();
        centered();
        successScroll.setViewportView(successLogTable);

        GroupLayout paneLayout = new GroupLayout(successLogPanel);
        successLogPanel.setLayout(paneLayout);
        paneLayout.setHorizontalGroup(
                paneLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(successScroll, GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE)
        );
        paneLayout.setVerticalGroup(
                paneLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(successScroll, GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
        );

        logPane.addTab("成功记录详情", successLogPanel);
        statScroll.setViewportView(statisticLogTable);

        GroupLayout pane2Layout = new GroupLayout(statDataPanel);
        statDataPanel.setLayout(pane2Layout);
        pane2Layout.setHorizontalGroup(
                pane2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(statScroll, GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE)
        );
        pane2Layout.setVerticalGroup(
                pane2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(statScroll, GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
        );

        logPane.addTab("历史统计数据", statDataPanel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(logPane)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(logPane)
        );

        pack();
    }

    public static void main(String args[]) {
        UITools.setTheme();
        LogViewDialog dialog = new LogViewDialog(null, true, StringUtils.EMPTY);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        UITools.setIconAndPosition(dialog);
        dialog.setVisible(true);
    }

    private JTabbedPane logPane;
    private JPanel successLogPanel;
    private JPanel statDataPanel;
    private JScrollPane successScroll;
    private JScrollPane statScroll;
    private JTable successLogTable;
    private JTable statisticLogTable;
}