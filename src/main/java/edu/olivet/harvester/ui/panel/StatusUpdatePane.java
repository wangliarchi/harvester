package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * 简易Status更新面板，通常可以与内存条并排放置
 * 
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 10/18/2017 4:27 PM
 */
public class StatusUpdatePane implements MessagePanel {
    private final JTextPane textPane;

    public void clearIfNotBlank() {
        if (StringUtils.isNotBlank(textPane.getText())) {
            textPane.setText(null);
        }
    }

    public StatusUpdatePane(JTextPane textPane) {
        this.textPane = textPane;
        this.textPane.setContentType("text/html");
    }

    @Override
    public synchronized void clearPrevious() {
        textPane.setText(null);
    }

    @Override
    public synchronized void displayMsg(String msg, InformationLevel... infoLevels) {
        if (ArrayUtils.isNotEmpty(infoLevels)) {
            if (ArrayUtils.contains(infoLevels, InformationLevel.Negative)) {
                textPane.setText("<html><font color=red><strong>" + msg + "</strong></font></html>");
            } else {
                textPane.setText("<html><font color=green><strong>" + msg + "</strong></font></html>");
            }
        } else {
            textPane.setText("<html>" + msg + "</html>");
        }
    }

    @Override
    public void addMsgSeparator() {

    }
}
