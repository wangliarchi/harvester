package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.utils.JXBrowserHelper;

import javax.swing.*;
import java.awt.*;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/13/2018 11:04 AM
 */
public class GeneralWebPanel extends WebPanel {
    protected String key;

    public GeneralWebPanel(String key) {
        super(new BorderLayout());
        this.key = key;
        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel);
        initComponents();
    }


    @Override
    public void toHomePage() {

    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public boolean running() {
        return false;
    }

    @Override
    public String profilePathName() {
        return getKey();
    }


    protected void initComponents() {
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(browserView, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)

        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(browserView, 250, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        )


        );
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setTitle("Seller Panel Demo");
        frame.setVisible(true);

        GeneralWebPanel webPanel = new GeneralWebPanel("test");
        frame.getContentPane().add(webPanel);

        UITools.setDialogAttr(frame, true);
        webPanel.toWelcomePage();
    }
}
