package edu.olivet.harvester.ui.panel;


import com.google.inject.Singleton;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.logger.OrderSubmissionLogger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/6/17 7:32 PM
 */
@Singleton
public class ProgressLogsPanel extends JPanel implements MessagePanel {

    private JSplitPane progressSplitPane1;
    private JTextPane successRecordsTextPanel;
    private JTextPane failedRecordsTextPanel;

    private static ProgressLogsPanel instance = new ProgressLogsPanel();

    public static ProgressLogsPanel getInstance() {
        return instance;
    }

    private ProgressLogsPanel() {
        initComponents();
        initEventListeners();
    }

    private void initComponents() {
        setBorder(null);

        progressSplitPane1 = new JSplitPane();

        successRecordsTextPanel = new JTextPane();
        failedRecordsTextPanel = new JTextPane();



        JScrollPane jScrollPane1 = new JScrollPane(successRecordsTextPanel);
        JScrollPane jScrollPane2 = new JScrollPane(failedRecordsTextPanel);

        UITools.setAutoScroll(successRecordsTextPanel);
        UITools.setAutoScroll(failedRecordsTextPanel);

        jScrollPane1.setPreferredSize(new Dimension(200, 150));
        jScrollPane2.setPreferredSize(new Dimension(200, 150));

        jScrollPane1.setBorder(BorderFactory.createTitledBorder("Success Records"));
        successRecordsTextPanel.setEditable(false);

        jScrollPane2.setBorder(BorderFactory.createTitledBorder("Failed Records"));
        failedRecordsTextPanel.setEditable(false);


        //UITools.setAutoScroll(successRecordsTextPanel);
        //UITools.setAutoScroll(failedRecordsTextPanel);

        progressSplitPane1.setDividerLocation(0.5);
        progressSplitPane1.setLeftComponent(jScrollPane1);
        progressSplitPane1.setRightComponent(jScrollPane2);
        progressSplitPane1.setBorder(null);


        if (SystemUtils.IS_OS_WINDOWS) {
            failedRecordsTextPanel.setFont(new Font(failedRecordsTextPanel.getFont().getName(), Font.PLAIN, Constants.TEXT_FONT_SIZE - 2));
        } else if (SystemUtils.IS_OS_LINUX) {
            failedRecordsTextPanel.setFont(new Font(Constants.LINUX_TEXT_FONT, Font.PLAIN, Constants.TEXT_FONT_SIZE - 2));
        }

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(progressSplitPane1)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(progressSplitPane1)
        );


    }

    private void initEventListeners() {
        this.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                progressSplitPane1.setDividerLocation(0.5);
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
    }


    @Override
    public void clearPrevious() {
        successRecordsTextPanel.setText(null);
        failedRecordsTextPanel.setText(null);
    }

    @Override
    public void displayMsg(String msg, InformationLevel... infoLevels) {
        this.append(msg + System.lineSeparator(), infoLevels);
    }


    @Override
    public void addMsgSeparator() {
        this.append(SPLIT_LINE + SPLIT_LINE + System.lineSeparator());
    }

    private synchronized void append(String msg, InformationLevel... infoLevels) {
        JTextPane textArea;
        if (ArrayUtils.isNotEmpty(infoLevels)) {
            if (ArrayUtils.contains(infoLevels, InformationLevel.Negative)) {
                textArea = failedRecordsTextPanel;
            } else {
                textArea = successRecordsTextPanel;
            }
        } else {
            textArea = successRecordsTextPanel;
        }

        Document doc = textArea.getDocument();
        AttributeSet as = InformationLevel.style(infoLevels);
        try {
            int length = doc.getLength();
            doc.insertString(length, msg, as);
        } catch (BadLocationException e) {
            // -> Ignore
        }

        if (textArea == successRecordsTextPanel) {
            OrderSubmissionLogger.info(msg);
        } else {
            OrderSubmissionLogger.error(msg);
        }


    }
}
