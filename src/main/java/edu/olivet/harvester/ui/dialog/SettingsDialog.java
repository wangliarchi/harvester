package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.panel.ConfigurationPanel;
import edu.olivet.harvester.ui.panel.SimpleOrderSubmissionRuntimePanel;
import edu.olivet.harvester.utils.Migration;
import edu.olivet.harvester.utils.SettingValidator;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Harvester fulfillment metadata configuration dialog
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 9:41 PM
 */
public class SettingsDialog extends BaseDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

    @Getter
    private Settings settings;

    private final List<Country> marketplaces =
            Arrays.asList(Country.US, Country.CA, Country.UK, Country.JP, Country.IN, Country.MX, Country.AU);
    private Map<Country, JCheckBox> checkBoxes = new HashMap<>();

    private JTextField sidFld;
    private JTabbedPane tabbedPane;
    private SettingValidator settingValidator;

    public SettingsDialog(SettingValidator settingValidator) {
        super(null, true);

        this.settingValidator = settingValidator;

        this.setMinimumSize(new Dimension(720, 600));
        this.setTitle("Harvester: Fulfillment Configuration");
        sidFld = new JTextField();
        sidFld.setToolTipText("Input assigned account number. For example: 18, 24, 710");

        Map<Country, Configuration> map = new HashMap<>();
        try {
            settings = Settings.load();
        } catch (IllegalStateException e) {
            if (Migration.hasMigrationFile() && Migration.isUseMigration()) {
                try {
                    //load setting migrated from orderman
                    settings = Migration.loadSettings();
                } catch (Exception e1) {
                    LOGGER.error("Error loading migration configuration file.", e1);
                    // -> Ignore
                }
            }
        }

        if (settings != null && CollectionUtils.isNotEmpty(settings.getConfigs())) {
            for (Configuration config : settings.getConfigs()) {
                map.put(config.getCountry(), config);
            }
            this.sidFld.setText(settings.getSid());

        }


        this.tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> {
            ConfigurationPanel configurationPanel = (ConfigurationPanel) tabbedPane.getSelectedComponent();
            configurationPanel.loadBuyerAccounts();
        });
        this.initButtons();
        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        for (Country country : marketplaces) {
            final JCheckBox checkBox = new JCheckBox(country.label(), settings == null || map.containsKey(country));
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    this.addPanel(country, map.get(country));
                } else {
                    Component[] tabs = tabbedPane.getComponents();
                    int i = 0;
                    for (Component comp : tabs) {
                        if (comp instanceof ConfigurationPanel && ((ConfigurationPanel) comp).getCountry() == country) {
                            break;
                        }
                        i++;
                    }
                    tabbedPane.remove(i);
                }
            });
            checkBoxes.put(country, checkBox);
            if (checkBox.isSelected()) {
                this.addPanel(country, map.get(country));
            }
        }

        JPanel panel = this.initSidPanel();
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        final int buttonHeight = 30;
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(panel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addGroup(Alignment.LEADING, layout.createSequentialGroup().addGap(20).addComponent(aboutBtn))
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH))
                .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(panel).addComponent(tabbedPane)
                        .addGap(5)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(aboutBtn, buttonHeight, buttonHeight, buttonHeight)
                                .addComponent(cancelBtn, buttonHeight, buttonHeight, buttonHeight)
                                .addComponent(okBtn, buttonHeight, buttonHeight, buttonHeight))));

        getRootPane().setDefaultButton(okBtn);
        pack();
    }

    private ConfigurationPanel addPanel(Country country, Configuration cfg) {
        ConfigurationPanel cfgPanel = new ConfigurationPanel(country, cfg);
        tabbedPane.addTab(country.label(), UITools.getIcon(country.name().toLowerCase() + "Flag.png"), cfgPanel);
        return cfgPanel;
    }

    private JPanel initSidPanel() {
        JPanel pane = new JPanel();
        final JLabel sidLbl = new JLabel(UIText.label("Account Number:"));
        final JLabel checkboxLbl = new JLabel(UIText.label("Marketplace Selection:"));

        GroupLayout layout = new GroupLayout(pane);
        pane.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap()
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(sidLbl)
                                .addComponent(checkboxLbl))
                        .addGap(28)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(sidFld, 240, 240, 240))
                                .addGroup(this.addCheckBoxesHorizontally(layout.createSequentialGroup())))));

        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(5)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(sidLbl).addComponent(sidFld, 28, 28, 28))
                        .addGap(5)
                        .addGroup(this.addCheckBoxesVertically(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(checkboxLbl)))));
        return pane;
    }

    private Group addCheckBoxesHorizontally(Group group) {
        for (Country country : marketplaces) {
            group.addComponent(checkBoxes.get(country)).addGap(8);
        }
        return group;
    }

    private Group addCheckBoxesVertically(Group group) {
        for (Country country : marketplaces) {
            group.addComponent(checkBoxes.get(country));
        }
        return group;
    }

    @Override
    public void ok() {
        String sid = sidFld.getText().trim();
        if (!Regex.ACC_NO.isMatched(sid)) {
            UITools.error(String.format("%s is not a valid account number. Please input 2-3 digits number.", sid));
            return;
        }

        Component[] tabs = tabbedPane.getComponents();
        if (tabs == null || tabs.length == 0) {
            UITools.error("Please configure at least one marketplace.");
            return;
        }

        List<Configuration> configs = new ArrayList<>(tabs.length);
        for (Component comp : tabs) {
            if (comp instanceof ConfigurationPanel) {
                ConfigurationPanel cfgPanel = (ConfigurationPanel) comp;
                if (!checkBoxes.get(cfgPanel.getCountry()).isSelected()) {
                    continue;
                }

                Configuration config = cfgPanel.collect();
                //LOGGER.info(config.toString());
                List<String> errors = config.validate();
                if (CollectionUtils.isEmpty(errors)) {
                    config.setAccountCode(sid + config.getCountry().name());
                    configs.add(config);
                } else {
                    UITools.error(String.format("Please fix %d configuration error(s) of %s marketplace:%n%n%s",
                            errors.size(), config.getCountry().name(), concatErrorMessages(errors)));
                    return;
                }
            }
        }
        if (CollectionUtils.isEmpty(configs)) {
            UITools.error("Please configure at least one marketplace.");
        }

        //validate
        List<String> errors = settingValidator.validate(sid, configs);
        if (!CollectionUtils.isEmpty(errors) && errors.size() > 0) {
            StringBuilder sb = new StringBuilder("\n");
            errors.forEach(error -> sb.append(error).append("\n\n"));
            UITools.error(sb.toString(), "Error");
            return;
        }


        this.settings = new Settings(sid, configs);
        settings.saveToFile();
        //clear cache

        //update info on runtime settings panel
        SimpleOrderSubmissionRuntimePanel.getInstance().resetAfterSettingUpdated();

        //UITools.info(String.format("Congratulations! Harvester configuration successfully saved into%n%s", file.getAbsolutePath()));
        UITools.info("Configuration has been saved successfully.");
        ok = true;
        doClose();
    }

    private String concatErrorMessages(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            String error = errors.get(i);
            String end = i < errors.size() ? (";" + StringUtils.LF) : ".";

            sb.append(i + 1).append(". ").append(error).append(end);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new SettingsDialog(new SettingValidator(new AppScript(), ApplicationContext.getBean(SheetAPI.class))), true);
        System.exit(0);
    }
}
