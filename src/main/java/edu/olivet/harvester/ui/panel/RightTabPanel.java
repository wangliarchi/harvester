package edu.olivet.harvester.ui.panel;

import com.google.inject.Singleton;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/14/17 3:24 PM
 */
@Singleton
public class RightTabPanel extends JTabbedPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(RightTabPanel.class);

    private static RightTabPanel instance;

    public static RightTabPanel getInstance() {
        if (instance == null) {
            instance = new RightTabPanel();
        }

        return instance;
    }

    private RightTabPanel() {
        this.addTab("Simple Task", SimpleOrderSubmissionRuntimePanel.getInstance());
        this.addTab("Multiple Sheet Tasks", TasksAndProgressPanel.getInstance());
    }


    public static void main(String[] args) {
        Tools.switchLogMode(Configs.LogMode.Development);

        JFrame frame = new JFrame("TabbedPaneFrame");
        RightTabPanel panel = new RightTabPanel();


        frame.getContentPane().add(panel);

        frame.setSize(400, 368);


        UITools.setDialogAttr(frame, true);

    }
}
