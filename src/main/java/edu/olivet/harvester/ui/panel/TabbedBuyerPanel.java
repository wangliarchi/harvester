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
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 9:14 AM
 */
public class TabbedBuyerPanel extends JTabbedPaneCloseButton {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabbedBuyerPanel.class);

    private static TabbedBuyerPanel instance = null;

    public final Map<String, WebPanel> buyerPanels = new HashMap<>();

    private double zoomLevel = -1;

    public static TabbedBuyerPanel getInstance() {
        if (instance == null) {
            instance = new TabbedBuyerPanel();
        }
        return instance;
    }

    public void addTab(WebPanel webPanel) {
        String tabKey = webPanel.getKey();
        this.addTab(tabKey, webPanel.getIcon() == null ? null : UITools.getIcon(webPanel.getIcon()), webPanel);
        buyerPanels.put(tabKey, webPanel);
    }


    public WebPanel getWebPanel(String tabKey) {
        if (buyerPanels.containsKey(tabKey)) {
            return buyerPanels.get(tabKey);
        }

        throw new BusinessException("No web panel found for key " + tabKey);
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

    private BuyerPanel initTabForOrder(Order order, Account buyer) {
        Country country = OrderCountryUtils.getFulfillmentCountry(order);
        return getOrAddTab(country, buyer);
    }

    public BuyerPanel getOrAddTab(Country country, Account account) {
        return addTab(country, account);
    }

    public BuyerPanel getBuyerPanel(String tabKey) {
        if (buyerPanels.containsKey(tabKey)) {
            return (BuyerPanel) buyerPanels.get(tabKey);
        }

        throw new BusinessException("No buyer panel found for buyer account " + tabKey);
    }

    public BuyerPanel getBuyerPanel(Country country, Account buyer) {
        String tabKey = getTabKey(country, buyer);
        return getBuyerPanel(tabKey);
    }

    private BuyerPanel addTab(Country country, Account account) {
        String tabKey = getTabKey(country, account);
        BuyerPanel buyerPanel;
        try {
            buyerPanel = getBuyerPanel(tabKey);
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            buyerPanel.getBuyer().setPassword(account.getPassword());
            return buyerPanel;
        } catch (Exception e) {
            //
        }

        buyerPanel = new BuyerPanel(buyerPanels.size(), country, account, Math.max(-1, zoomLevel));
        this.addTab(buyerPanel);

        LOGGER.info("Finished init buyer {} account {} panel", country.name(), account.getKey());
        return buyerPanel;
    }

    public BuyerPanel addSheetTab(Country country, Account account) {
        String tabKey = country.name() + " Spreadsheet";
        if (buyerPanels.containsKey(tabKey)) {
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            return (BuyerPanel) buyerPanels.get(tabKey);
        }

        final long start = System.currentTimeMillis();

        BuyerPanel buyerPanel = new BuyerPanel(buyerPanels.size(), country, account, Math.max(-1, zoomLevel));

        this.addTab(tabKey, UITools.getIcon("sheet.png"), buyerPanel);
        buyerPanels.put(tabKey, buyerPanel);

        LOGGER.info("Finished init buyer {} account {} panel，took{}", country.name(), account.getKey(), Strings.formatElapsedTime(start));

        return buyerPanel;
    }


    public WebPanel getSelectedWebPanel() {
        return (WebPanel) getSelectedComponent();
    }

    public void setRunningIcon(WebPanel webPanel) {
        setIconAt(getPanelIndex(webPanel), UITools.getIcon("loading.gif"));
    }

    public void setNormalIcon(WebPanel webPanel) {
        setIconAt(getPanelIndex(webPanel), UITools.getIcon(webPanel.getIcon()));
    }

    public int getPanelIndex(WebPanel webPanel) {
        String key = webPanel.getKey();
        for (int i = 0; i < getTabCount(); i++) {
            if (key.equalsIgnoreCase(getTitleAt(i))) {
                return i;
            }
        }

        throw new BusinessException("Panel not found");
    }

    public void highlight(WebPanel webPanel) {
        highlight(getPanelIndex(webPanel));
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


    public void removeTab(WebPanel webPanel) {
        //don't remove if it's last tab
        if (instance.getComponents().length == 1) {
            return;
        }
        String tabKey = webPanel.getKey();
        try {
            webPanel.killBrowser();
        } catch (Exception e) {
            //
        }
        for (int i = 0; i < getTabCount(); i++) {
            if (getTitleAt(i).equalsIgnoreCase(tabKey)) {
                this.remove(i);
                break;
            }
            //other stuff
        }
        removeFromIndex(webPanel);
    }

    public BuyerPanel reInitTabForOrder(Order order, Account buyer) {
        LOGGER.error("JXBrowser crashed, trying to recreate buyer panel");
        BuyerPanel buyerPanel = initTabForOrder(order, buyer);
        this.removeTab(buyerPanel);
        buyerPanel = initTabForOrder(order, buyer);

        buyerPanel.toWelcomePage();
        return buyerPanel;
    }

    @Override
    public void addNewTab() {
        if (this.getTabCount() <= 1) {
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
            new Thread(buyerPanel::toHomePage).start();
        }
    }

    @Override
    public boolean beforeTabRemoved(Component tab) {
        if (this.getTabCount() <= 1) {
            return false;
        }
        WebPanel webPanel = (WebPanel) tab;
        if (webPanel.running()) {
            UITools.error("Task is running on this tab. Stop task before closing the tab.");
            return false;
        }

        try {
            webPanel.killBrowser();
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        removeFromIndex(webPanel);

        return true;
    }

    public void removeFromIndex(WebPanel webPanel) {
        String tabKey = webPanel.getKey();
        buyerPanels.remove(tabKey);
    }

    public static void main(String[] args) {
        Tools.switchLogMode(Configs.LogMode.Development);

        JFrame frame = new JFrame("TabbedPaneFrame");
        TabbedBuyerPanel panel = new TabbedBuyerPanel();

        Account buyer = Settings.load().getConfigByCountry(Country.US).getBuyer();
        panel.addTab(Country.US, buyer);

        panel.getSelectedWebPanel().toHomePage();

        panel.setVisible(true);
        frame.getContentPane().add(panel);
        frame.setSize(800, 768);
        UITools.setDialogAttr(frame, true);

        WaitTime.Short.execute();

        WebPanel buyerPanel = panel.getSelectedWebPanel();
        buyerPanel.toHomePage();

        buyerPanel.recreateBrowser();

        buyerPanel.toHomePage();
        WaitTime.Long.execute();
    }
}
