package edu.olivet.harvester.ui.dialog;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.service.ItemValidator;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * 批量比较书名结果显示对话框，用户可以进行人工校验确定
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Oct 24, 2014 11:46:52 AM
 */
public class ItemCheckResultDialog extends javax.swing.JDialog {
    private static final long serialVersionUID = 4091521789258343113L;
    private final List<ItemCompareResult> results;
    private boolean validReturn;
    private DefaultTableModel tableModel;
    private String[] COLUMN_NAMES;

    public ItemCheckResultDialog(JFrame parent, boolean modal, List<ItemCompareResult> results) {
        super(parent, modal);
        this.results = results;
        COLUMN_NAMES = new String[]{
                UIText.label("label.itemcheck.column.rowno"),
                UIText.label("label.itemcheck.column.isbn"),
                UIText.label("label.itemcheck.column.isbnname"),
                UIText.label("label.itemcheck.column.itemname"),
                UIText.label("label.itemcheck.column.programpass"),
                UIText.label("label.itemcheck.column.checkreport"),
                UIText.label("label.itemcheck.column.humanpass")};
        initTableModel();
        initComponents();
    }

    private void initTableModel() {
        int rowCount = results.size();
        int colColunt = COLUMN_NAMES.length;

        Object[][] data = getTableData();

        tableModel = new DefaultTableModel(rowCount, colColunt) {
            private static final long serialVersionUID = -2054247119434140726L;

            Class<?>[] types = new Class[]{
                    Integer.class, String.class, String.class, String.class, Boolean.class, String.class, Boolean.class
            };
            boolean[] canEdit = new boolean[]{
                    false, false, false, false, false, false, true
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };

        tableModel.setDataVector(data, COLUMN_NAMES);
    }

    private int count = 0, pass = 0, fail = 0;

    public Object[][] getTableData() {
        count = results.size();

        Object[][] data = new Object[count][COLUMN_NAMES.length];
        int i = 0;
        for (ItemCompareResult icr : results) {
            if (icr.isPreCheckPass()) {
                pass++;
            } else {
                fail++;
            }
            data[i++] = new Object[]{icr.getRow(), icr.getIsbn(), icr.getIsbnName(), icr.getItemName(), icr.isPreCheckPass(), icr.getPreCheckReport(), false};
        }
        return data;
    }

    private void initComponents() {
        scrollPanel = new JScrollPane();
        resultTable = new JTable(tableModel) {
            private static final long serialVersionUID = -7996493405307614317L;

            @Override
            public String getToolTipText(@NotNull MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);

                if (realColumnIndex == 2 || realColumnIndex == 3 || realColumnIndex == 5) {
                    return (String) this.getModel().getValueAt(rowIndex, realColumnIndex);
                } else {
                    return super.getToolTipText(e);
                }
            }
        };
        setColumnWidths();

        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        String title = UIText.title("title.itemname.compare.result");
        setTitle(title);

        scrollPanel.setBorder(new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null));
        scrollPanel.setViewportView(resultTable);

        okBtn.setText(UIText.label("label.ok"));
        okBtn.addActionListener(evt -> okBtnActionPerformed(evt));

        cancelBtn.setText(UIText.label("label.cancel"));
        cancelBtn.addActionListener(evt -> {
            setValidReturn(false);
            dispose();
        });

        passAllBtn = new JCheckBox(UIText.label("label.pass.manually.all"));
        passAllBtn.setVisible(false);
        passAllBtn.addActionListener(e -> passAllManual());

        filterCheckbox = new JCheckBox(UIText.label("label.filter.notpass"));
        filterCheckbox.addActionListener(e -> {
            filterResults();
            passAllBtn.setVisible(filterCheckbox.isSelected());
            if (!filterCheckbox.isSelected()) {
                passAllBtn.setSelected(false);
            }
        });

        JScrollPane rulePanel = new JScrollPane();
        rulePanel.setBorder(new TitledBorder(null, UIText.title("title.compare.rules"), TitledBorder.LEADING, TitledBorder.TOP, null, null));

        summaryTxtFld = new JLabel();
        summaryTxtFld.setForeground(Color.BLUE);
        summaryTxtFld.setText(UIText.text("text.compare.summary", count, pass, fail));

        GroupLayout layout = new GroupLayout(getContentPane());
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGap(10)
                                .addComponent(summaryTxtFld, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGap(10)
                                .addComponent(filterCheckbox)
                                .addGap(10)
                                .addComponent(passAllBtn)
                                .addPreferredGap(ComponentPlacement.RELATED, 340, Short.MAX_VALUE)
                                .addComponent(okBtn, 75, 75, 75)
                                .addGap(16)
                                .addComponent(cancelBtn, 75, 75, 75)
                                .addContainerGap())
                        .addComponent(scrollPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(rulePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(rulePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addComponent(scrollPanel, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                .addComponent(okBtn)
                                                .addComponent(cancelBtn))
                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                .addComponent(filterCheckbox)
                                                .addComponent(summaryTxtFld, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(passAllBtn))))
        );

        rulesTextArea = new JTextArea();
        rulesTextArea.setBackground(SystemColor.info);
        rulesTextArea.setForeground(SystemColor.desktop);
        rulesTextArea.setEditable(false);
        rulesTextArea.setText(ItemValidator.nameCompareRules());
        rulePanel.setViewportView(rulesTextArea);
        getContentPane().setLayout(layout);
        pack();
    }

    public void setColumnWidths() {
        int j = 0;
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(60);
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(90);
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(300);
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(300);
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(250);
        resultTable.getColumnModel().getColumn(j++).setPreferredWidth(80);
    }

    private List<ItemCompareResult> isbn2Sync;
    private static final Logger logger = LoggerFactory.getLogger(ItemCheckResultDialog.class);

    private void filterResults() {
        if (this.filterCheckbox.isSelected()) {
            Vector<?> vector = this.tableModel.getDataVector();
            for (Iterator<?> iter = vector.iterator(); iter.hasNext(); ) {
                Vector<?> row = (Vector<?>) iter.next();
                if (Boolean.TRUE.equals(row.elementAt(4))) {
                    iter.remove();
                }
            }
        } else {
            this.tableModel.setDataVector(this.getTableData(), COLUMN_NAMES);
        }
        this.tableModel.fireTableDataChanged();
        setColumnWidths();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void passAllManual() {
        Boolean result = passAllBtn.isSelected();
        Vector vector = this.tableModel.getDataVector();
        for (Object aVector : vector) {
            Vector row = (Vector) aVector;
            if (Boolean.FALSE.equals(row.elementAt(4)) && !result.equals(row.elementAt(6))) {
                row.set(6, result);
            }
        }
        this.tableModel.fireTableDataChanged();
        setColumnWidths();
    }

    private void okBtnActionPerformed(ActionEvent evt) {
        isbn2Sync = new ArrayList<ItemCompareResult>();

        Vector<?> vector = this.tableModel.getDataVector();

        int rowNo = 0;
        for (Object aVector : vector) {
            Vector<?> row = (Vector<?>) aVector;
            if (Boolean.TRUE.equals(row.elementAt(6))) {
                logger.debug("第{}行订单已人工检查通过，确定后订单书名将同步写入", row.elementAt(0));
                ItemCompareResult result = results.get(rowNo);
                result.setManualCheckPass(true);
                isbn2Sync.add(result);
            } else if (!Boolean.TRUE.equals(row.elementAt(4))) {
                logger.debug("第{}行订单程序、人工检查均不通过，确定后该条订单状态将被置为n", row.elementAt(0));
                ItemCompareResult result = results.get(rowNo);
                result.setManualCheckPass(false);
                result.setPreCheckPass(false);
                isbn2Sync.add(result);
            }
            rowNo++;
        }

        if (CollectionUtils.isNotEmpty(isbn2Sync)) {
            this.setValidReturn(true);
        }
        this.dispose();
    }


    private javax.swing.JButton cancelBtn;
    private javax.swing.JButton okBtn;
    private JScrollPane scrollPanel;
    private JTable resultTable;
    private JCheckBox filterCheckbox;
    private JTextArea rulesTextArea;
    private JLabel summaryTxtFld;
    private JCheckBox passAllBtn;

    public boolean isValidReturn() {
        return validReturn;
    }

    public void setValidReturn(boolean validReturn) {
        this.validReturn = validReturn;
    }

    public List<ItemCompareResult> getIsbn2Sync() {
        return isbn2Sync;
    }
}
