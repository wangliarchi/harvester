package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.Action;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Constants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/23/17 1:05 PM
 */
public class DisplayContentDialog extends JDialog {
    private static final long serialVersionUID = -2630321609944005356L;

    public DisplayContentDialog(Frame parent, boolean modal, Action action) {
        super(parent, modal);
        initComponents();

        this.setTitle(action.label());
        try {
            String content;

            Language lang = Language.current();
            String postfix = lang == Language.ZH_CN ? StringUtils.EMPTY : ("_" + lang.locale().toString());
            content = IOUtils.toString(
                    DisplayContentDialog.class.getResourceAsStream("/" + action.name().toLowerCase() + postfix + ".txt"), Constants.UTF8);

            textArea.setText(content);
        } catch (Exception e) {
            UITools.error(UIText.message("message.error.read", action.label(), e.getMessage()), UIText.title("title.code_error"));
        }
    }

    private DisplayContentDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public void displayMsg(String msg) {
        this.textArea.append(msg + Constants.NEW_LINE);
    }

    private void initComponents() {
        this.setResizable(false);
        JButton okButton = new JButton();
        JScrollPane scrollPane = new JScrollPane();
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                doClose();
            }
        });

        okButton.setText(UIText.label("label.ok"));
        okButton.addActionListener(evt -> doClose());

        if (SystemUtils.IS_OS_WINDOWS) {
            textArea.setFont(new Font(okButton.getFont().getName(), Font.PLAIN, Constants.TEXT_FONT_SIZE));
        } else if (SystemUtils.IS_OS_LINUX) {
            textArea.setFont(new Font(Constants.LINUX_TEXT_FONT, Font.PLAIN, Constants.TEXT_FONT_SIZE));
        }
        scrollPane.setViewportView(textArea);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 546, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(okButton)
                                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        pack();
    }



    private void doClose() {
        setVisible(false);
        dispose();
    }

    private JTextArea textArea;
}
