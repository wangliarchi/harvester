package edu.olivet.harvester.ui.utils;

import edu.olivet.harvester.fulfill.model.OrderTaskStatus;

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
        value = getValue(status);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }


    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String status = table.getModel().getValueAt(row, column - 1).toString();
        value = getValue(status);

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }


    public Object getValue(String status) {
        Object value;
        OrderTaskStatus taskStatus = OrderTaskStatus.valueOf(status);
        switch (taskStatus) {
            case Stopped:
                value = "Resume";
                break;
            case Processing:
            case Queued:
                value = "Stop";
                break;
            case Scheduled:
                value = "Delete";
                break;
            case Completed:
                value = "Retry";
                break;
            default:
                value = "";
        }

        return value;
    }


}