package edu.olivet.harvester.ui.utils;

import edu.olivet.harvester.fulfill.model.OrderTaskStatus;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;


public class OrderTaskButtonColumn extends ButtonColumn {

    /**
     * Create the ButtonColumn to be used as a renderer and editor. The
     * renderer and editor will automatically be installed on the TableColumn
     * of the specified column.
     *
     * @param table the table containing the button renderer/editor
     * @param action the Action to be invoked when the button is invoked
     * @param column the column to which the button renderer/editor is added
     */
    public OrderTaskButtonColumn(JTable table, Action action, int column) {
        super(table, action, column);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        String status = table.getModel().getValueAt(row, column - 1).toString();
        if (status.equalsIgnoreCase(OrderTaskStatus.Stopped.name())) {
            value = "Resume";
        }
//        else if (status.equalsIgnoreCase(OrderTaskStatus.Completed.name())) {
//            value = "Retry";
//        }

        if (StringUtils.equalsAnyIgnoreCase(status, OrderTaskStatus.Scheduled.name(), OrderTaskStatus.Error.name(), OrderTaskStatus.Stopped.name())) {
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        return getEmptyRendererComponent();
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String status = table.getModel().getValueAt(row, column - 1).toString();
        if (status.equalsIgnoreCase(OrderTaskStatus.Stopped.name())) {
            value = "Resume";
        }

        if (StringUtils.equalsAnyIgnoreCase(status, OrderTaskStatus.Scheduled.name(), OrderTaskStatus.Error.name(), OrderTaskStatus.Stopped.name())) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            return getEmptyRendererComponent();
        }
    }

    public Component getEmptyRendererComponent() {

        renderButton.setText("");
        renderButton.setVisible(false);
        renderButton.setBorder(null);
        renderButton.setForeground(null);
        return renderButton;

    }


}