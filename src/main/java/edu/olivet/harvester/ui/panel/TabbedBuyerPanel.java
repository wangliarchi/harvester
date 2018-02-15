package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.dialog.AddBuyerTabDialog;
import edu.olivet.harvester.ui.utils.JTabbedPaneCloseButton;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 9:14 AM
 */
public class TabbedBuyerPanel extends JTabbedPaneCloseButton {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabbedBuyerPanel.class);

    private static TabbedBuyerPanel instance = null;

    private final Map<String, BuyerPanel> buyerPanels = new HashMap<>();
    private final Map<Integer, String> buyerPanelIndexes = new HashMap<>();

    private double zoomLevel;

    public static TabbedBuyerPanel getInstance() {
        if (instance == null) {
            instance = new TabbedBuyerPanel(-1);
        }
        return instance;
    }

    private TabbedBuyerPanel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public void addFirstBuyerAccountTab() {
        Settings settings = Settings.load();
        for (Configuration config : settings.getConfigs()) {
            if (config.getPrimeBuyer() != null && StringUtils.isNotBlank(config.getPrimeBuyer().getEmail())) {
                this.addTab(config.getCountry(), config.getPrimeBuyer());
                return;
            }
            if (config.getBuyer() != null && StringUtils.isNotBlank(config.getBuyer().getEmail())) {
                this.addTab(config.getCountry(), config.getBuyer());
                return;
            }

        }
    }

    public BuyerPanel reInitTabForOrder(Order order, Account buyer) {
        LOGGER.error("JXBrowser crashed, trying to recreate buyer panel");
        BuyerPanel buyerPanel = initTabForOrder(order, buyer);
        this.removeTab(buyerPanel);
        buyerPanel = initTabForOrder(order, buyer);

        buyerPanel.toWelcomePage();
        return buyerPanel;
    }


    private BuyerPanel initTabForOrder(Order order, Account buyer) {
        Country country = OrderCountryUtils.getFulfillmentCountry(order);
        return getOrAddTab(country, buyer);
    }

    public BuyerPanel getOrAddTab(Country country, Account account) {
        //highlight(buyerPanel);
        return addTab(country, account);
    }


    public void removeTab(BuyerPanel buyerPanel) {
        String tabKey = getTabKey(buyerPanel.getCountry(), buyerPanel.getBuyer());
        try {
            buyerPanel.getBrowserView().getBrowser().dispose();
        } catch (Exception e) {
            //
        }
        for (int i = 0; i < getTabCount(); i++) {
            if (getTitleAt(i).equalsIgnoreCase(tabKey)) {
                this.remove(i);
            }
            //other stuff
        }
        int index = buyerPanel.getId();
        buyerPanels.remove(tabKey);
        buyerPanelIndexes.remove(index);
    }

    private BuyerPanel addTab(Country country, Account account) {
        String tabKey = getTabKey(country, account);
        if (buyerPanels.containsKey(tabKey)) {
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            BuyerPanel buyerPanel = buyerPanels.get(tabKey);
            buyerPanel.getBuyer().setPassword(account.getPassword());
            return buyerPanel;
        }

        final long start = System.currentTimeMillis();

        BuyerPanel buyerPanel = new BuyerPanel(buyerPanels.size(), country, account, Math.max(-1, zoomLevel));

        this.addTab(tabKey, UITools.getIcon(country.name().toLowerCase() + "Flag.png"), buyerPanel);
        buyerPanelIndexes.put(buyerPanels.size(), tabKey);
        buyerPanels.put(tabKey, buyerPanel);

        LOGGER.info("Finished init buyer {} account {} panel，took{}", country.name(), account.getKey(), Strings.formatElapsedTime(start));

        return buyerPanel;
    }

    public BuyerPanel addSheetTab(Country country, Account account) {
        String tabKey = country.name() + " Spreadsheet";
        if (buyerPanels.containsKey(tabKey)) {
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            return buyerPanels.get(tabKey);
        }

        final long start = System.currentTimeMillis();

        BuyerPanel buyerPanel = new BuyerPanel(buyerPanels.size(), country, account, Math.max(-1, zoomLevel));

        this.addTab(tabKey, UITools.getIcon("sheet.png"), buyerPanel);
        buyerPanelIndexes.put(buyerPanels.size(), tabKey);
        buyerPanels.put(tabKey, buyerPanel);

        LOGGER.info("Finished init buyer {} account {} panel，took{}", country.name(), account.getKey(), Strings.formatElapsedTime(start));

        return buyerPanel;
    }


    public BuyerPanel getBuyerPanel(Country country, Account buyer) {
        String tabKey = getTabKey(country, buyer);
        if (buyerPanels.containsKey(tabKey)) {
            return buyerPanels.get(tabKey);
        }
        throw new BusinessException("No buyer panel found for buyer account " + tabKey);
    }

    private BuyerPanel getBuyerPanel(Integer index) {
        if (buyerPanelIndexes.containsKey(index)) {
            return buyerPanels.get(buyerPanelIndexes.get(index));
        }

        throw new BusinessException(String.format("No buyer panel found for index %d", index));
    }

    public BuyerPanel getSelectedBuyerPanel() {
        return getBuyerPanel(getSelectedIndex());
    }

    public void setRunningIcon(BuyerPanel buyerPanel) {
        setIconAt(getBuyerPanelIndex(buyerPanel), UITools.getIcon("loading.gif"));
    }

    public void setNormalIcon(BuyerPanel buyerPanel) {
        setIconAt(getBuyerPanelIndex(buyerPanel), UITools.getIcon(buyerPanel.getCountry().name().toLowerCase() + "Flag.png"));
    }

    public int getBuyerPanelIndex(BuyerPanel buyerPanel) {
        String key = getTabKey(buyerPanel.getCountry(), buyerPanel.getBuyer());
        for (int i = 0; i < getTabCount(); i++) {
            if (key.equalsIgnoreCase(getTitleAt(i))) {
                return i;
            }
        }

        return buyerPanel.getId();
    }

    public void highlight(BuyerPanel buyerPanel) {
        highlight(getBuyerPanelIndex(buyerPanel));
    }

    private void highlight(int index) {
        try {
            this.setSelectedIndex(index);
        } catch (Exception e) {
            // -> Ignore
        }
    }


    private String getTabKey(Country country, Account account) {
        return country.name() + "-" + account.getEmail();
    }

    @Override
    public void addNewTab() {
        if (this.getTabCount() == 1) {
            return;
        }

        AddBuyerTabDialog dialog = UITools.setDialogAttr(new AddBuyerTabDialog());
        if (dialog.isOk()) {
            Country country = dialog.getSelectedCountry();
            Account buyer = dialog.getSelectedAccount();

            String tabKey = getTabKey(country, buyer);
            if (buyerPanels.containsKey(tabKey)) {
                this.highlight(buyerPanels.get(tabKey));
                return;
            }

            BuyerPanel buyerPanel = addTab(country, buyer);
            //buyerPanel.toHomePage();
            new Thread(() -> buyerPanel.toHomePage()).start();
        }
    }

    @Override
    public boolean beforeTabRemoved(Component tab) {
        if (this.getTabCount() == 2) {
            return false;
        }
        BuyerPanel buyerPanel = (BuyerPanel) tab;
        if (buyerPanel.running()) {
            UITools.error("Task is running on this tab. Stop task before closing the tab.");
            return false;
        }
        String tabKey = getTabKey(buyerPanel.getCountry(), buyerPanel.getBuyer());
        try {
            buyerPanel.killBrowser();
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        int index = buyerPanel.getId();
        buyerPanels.remove(tabKey);
        buyerPanelIndexes.remove(index);

        return true;
    }

    public static void main(String[] args) {
        Tools.switchLogMode(Configs.LogMode.Development);

        JFrame frame = new JFrame("TabbedPaneFrame");
        TabbedBuyerPanel panel = new TabbedBuyerPanel(-1);

        Account buyer = Settings.load().getConfigByCountry(Country.US).getBuyer();
        panel.addTab(Country.US, buyer);

        panel.getBuyerPanel(0).toHomePage();

        panel.setVisible(true);
        frame.getContentPane().add(panel);

        frame.setSize(800, 768);

        UITools.setDialogAttr(frame, true);


        WaitTime.Short.execute();

        BuyerPanel buyerPanel = panel.getBuyerPanel(0);
        buyerPanel.toHomePage();

        buyerPanel.recreateBrowser();

        //panel.removeTab(panel.getBuyerPanel(0));

        //BuyerPanel buyerPanel = panel.addTab(Country.US,buyer);
        buyerPanel.toHomePage();
        WaitTime.Long.execute();


    }
}
