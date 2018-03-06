package edu.olivet.harvester.ui.utils;

import edu.olivet.foundations.ui.UITools;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author 6dc
 * A class which creates a JTabbedPane and auto sets a close button when you add a tab
 */
public abstract class JTabbedPaneCloseButton extends JTabbedPane {

    public JTabbedPaneCloseButton() {
        super();

        FlowLayout f = new FlowLayout(FlowLayout.CENTER, 5, 0);

        // Make a small JPanel with the layout and make it non-opaque
        JPanel pnlTab = new JPanel(f);
        pnlTab.setOpaque(false);

        pnlTab.setName(" + ");
        super.add(pnlTab, 0);
    }

    @Override
    public void setSelectedIndex(int index) {
        String name = this.getComponentAt(index).getName();
        if (" + ".equalsIgnoreCase(name)) {
            addNewTab();
            return;
        }
        super.setSelectedIndex(index);
    }

    public abstract void addNewTab();

    /* Override Addtab in order to add the close Button everytime */
    @Override
    public void addTab(String title, Icon icon, Component component, String tip) {
        int count = this.getTabCount() - 1;
        super.insertTab(title, icon, component, tip, count);
        setTabComponentAt(count, new CloseButtonTab(this, component, title, icon));
        setSelectedIndex(count);
    }

    @Override
    public void addTab(String title, Icon icon, Component component) {
        addTab(title, icon, component, null);
    }

    @Override
    public void addTab(String title, Component component) {
        addTab(title, null, component);
    }

    /* addTabNoExit */
    public void addTabNoExit(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
    }

    public void addTabNoExit(String title, Icon icon, Component component) {
        addTabNoExit(title, icon, component, null);
    }

    public void addTabNoExit(String title, Component component) {
        addTabNoExit(title, null, component);
    }


    public void addTabButton() {
        FlowLayout f = new FlowLayout(FlowLayout.CENTER, 5, 0);

        // Make a small JPanel with the layout and make it non-opaque
        JPanel pnlTab = new JPanel(f);
        pnlTab.setOpaque(false);
        // Create a JButton for adding the tabs
        JButton addTab = new JButton(" + ");
        addTab.setOpaque(false); //
        addTab.setBorder(null);
        addTab.setContentAreaFilled(false);
        addTab.setFocusPainted(false);
        addTab.setFocusable(false);

        pnlTab.add(addTab);
        pnlTab.setName(" + ");
        this.add(pnlTab, 0);
        addTab.setFocusable(false);
        this.setVisible(true);
    }

    @Override
    public void setIconAt(int index, Icon icon) {
        Component component = this.getTabComponentAt(index);
        if (component instanceof CloseButtonTab) {
            ((CloseButtonTab) component).jLabel.setIcon(icon);
        }
        super.setIconAt(index, icon);
    }

    /* Button */
    public class CloseButtonTab extends JPanel {
        private Component tab;
        @Getter
        private JLabel jLabel;

        public CloseButtonTab(final JTabbedPaneCloseButton parent, final Component tab, String title, Icon icon) {
            this.tab = tab;
            setOpaque(false);
            FlowLayout flowLayout = new FlowLayout(FlowLayout.CENTER, 3, 3);
            setLayout(flowLayout);
            jLabel = new JLabel(title);
            jLabel.setIcon(icon);
            add(jLabel);

            JButton button = new JButton(UITools.getIcon("cancel_10x10.png"));
            Dimension closeButtonSize = new Dimension(12, 12);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setPreferredSize(closeButtonSize);
            button.addMouseListener(new CloseListener(parent, tab));
            button.setOpaque(false); //
            button.setBorder(null);
            add(button);
        }
    }

    public abstract boolean beforeTabRemoved(Component tab);

    /* ClickListener */
    public class CloseListener implements MouseListener {
        private Component tab;
        private JTabbedPaneCloseButton parent;

        public CloseListener(final JTabbedPaneCloseButton parent, Component tab) {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getSource() instanceof JButton) {
                JButton clickedButton = (JButton) e.getSource();
                JTabbedPane tabbedPane = (JTabbedPane) clickedButton.getParent().getParent().getParent();
                if (parent.beforeTabRemoved(tab)) {
                    tabbedPane.remove(tab);
                }

                if (parent.getSelectedIndex() == getTabCount() - 1) {
                    setSelectedIndex(getTabCount() - 2);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }
}
