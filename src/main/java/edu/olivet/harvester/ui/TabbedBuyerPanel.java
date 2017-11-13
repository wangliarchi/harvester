package edu.olivet.harvester.ui;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 9:14 AM
 */
public class TabbedBuyerPanel extends JTabbedPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabbedBuyerPanel.class);

    private static TabbedBuyerPanel instance = new TabbedBuyerPanel(-1);

    private final Map<String, BuyerPanel> buyerPanels = new HashMap<>();
    private final Map<Integer, String> buyerPanelIndexes = new HashMap<>();

    private double zoomLevel;

    public static TabbedBuyerPanel getInstance() {
        return instance;
    }

    private TabbedBuyerPanel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public TabbedBuyerPanel(Country country, Account buyer, double zoomLevel) {
        this(zoomLevel);
        this.setVisible(true);
        this.addTab(country, buyer);
    }

    public void addAllBuyerAccountTabs() {
        Settings settings = Settings.load();
        settings.getConfigs().forEach(config -> {

            if (config.getBuyer() != null) {
                this.addTab(config.getCountry(), config.getBuyer());
            }
            if (config.getPrimeBuyer() != null) {
                this.addTab(config.getCountry(), config.getPrimeBuyer());
            }
        });
    }

    public BuyerPanel getOrAddTab(Country country, Account account) {
        return addTab(country, account);
    }


    public BuyerPanel addTab(Country country, Account account) {
        String tabKey = getTabKey(country, account);
        if (buyerPanels.containsKey(tabKey)) {
            LOGGER.info("Buyer account {} already initialized. ", tabKey);
            return buyerPanels.get(tabKey);
        }

        final long start = System.currentTimeMillis();

        BuyerPanel buyerPanel = new BuyerPanel(buyerPanels.size(), country, account, Math.max(-1, zoomLevel));

        this.addTab(tabKey, UITools.getIcon(country.name().toLowerCase() + "Flag.png"), buyerPanel);
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

    public BuyerPanel getBuyerPanel(Integer index) {
        if (buyerPanelIndexes.containsKey(index)) {
            return buyerPanels.get(buyerPanelIndexes.get(index));
        }

        throw new BusinessException(String.format("No buyer panel found for index %d", index));
    }

    public BuyerPanel getSelectedBuyerPanel() {
        return getBuyerPanel(getSelectedIndex());
    }


    public void highlight(BuyerPanel buyerPanel) {
        highlight(buyerPanel.getId());
    }

    public void highlight(int index) {
        try {
            this.setSelectedIndex(index);
        } catch (Exception e) {
            // -> Ignore
        }
    }

    public void resetZoomLevel() {
        zoomLevel = (getWidth() - 1000) > 0 ? 1 : (getWidth() / 1000 - 2.1);
        buyerPanels.forEach((index, buyerPanel) -> {
            buyerPanel.getBrowserView().getBrowser().setZoomLevel(zoomLevel);
        });
    }

    private String getTabKey(Country country, Account account) {
        return country.name() + "-" + account.getEmail();
    }

    public static void main(String[] args) {
        Tools.switchLogMode(Configs.LogMode.Development);

        JFrame frame = new JFrame("TabbedPaneFrame");
        TabbedBuyerPanel panel = new TabbedBuyerPanel(-1);

        Settings settings = Settings.load();
        settings.getConfigs().forEach(config -> {

            if (config.getBuyer() != null) {
                panel.addTab(config.getCountry(), config.getBuyer());
            }
            if (config.getPrimeBuyer() != null) {
                panel.addTab(config.getCountry(), config.getPrimeBuyer());
            }

        });
        panel.getBuyerPanel(0).toHomePage();
        panel.setVisible(true);
        frame.getContentPane().add(panel);

        frame.setSize(800, 768);


        UITools.setDialogAttr(frame, true);

    }
}
