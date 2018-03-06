package edu.olivet.harvester.ui.panel;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.google.GoogleServiceProvider;
import edu.olivet.foundations.google.SpreadService;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.common.model.SystemSettings;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 2:31 PM
 */
public class SelfOrderSettingPanel extends JPanel {
    private final SpreadService spreadService;

    public SelfOrderSettingPanel() {
        spreadService = ApplicationContext.getBean(SpreadService.class);
        initComponents();
    }

    private JTextField spreadsheetIdField;
    private JTextField costLimitField;
    private JList<String> searchResultList;
    private JButton searchButton;
    private List<File> searchResult = new ArrayList<>();
    private JButton selectButton;
    private JButton cancelButton;
    private JScrollPane searchResultScrollPane;

    private void initComponents() {

        SystemSettings systemSettings = SystemSettings.load();

        searchResultScrollPane = new JScrollPane();

        final JLabel spreadsheetIdLabel = new JLabel("Spreadsheet Id");
        spreadsheetIdField = new JTextField(systemSettings.getSelfOrderSpreadsheetId());

        final JLabel costLimitLabel = new JLabel("Cost Limit");
        costLimitField = new JTextField(String.valueOf(systemSettings.getSelfOrderCostLimit()));

        searchResultList = new JList<>();
        searchResultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //searchResultList.setVisible(false);
        searchButton = new JButton("Search");
        selectButton = new JButton("Select");
        cancelButton = new JButton("Cancel");

        searchResultScrollPane.setViewportView(searchResultList);
        hideSearchList();
        searchButton.addActionListener(evt -> {
            showSearchList();
            String q;
            if (StringUtils.isEmpty(spreadsheetIdField.getText())) {
                q = "self order";
            } else {
                q = spreadsheetIdField.getText().trim();
            }
            String query = String.format("mimeType='application/vnd.google-apps.spreadsheet' and name contains '%s' ", q);
            try {
                searchResult = spreadService.query(Constants.RND_EMAIL, query);
            } catch (Exception e) {
                //
            }
            if (CollectionUtils.isNotEmpty(searchResult)) {
                String[] list = searchResult.stream().map(File::getName).toArray(String[]::new);
                searchResultList.setListData(list);
                searchResultList.setSelectedIndex(0);
                selectButton.setEnabled(true);
            } else {
                String[] list = new String[] {"No result found"};
                searchResultList.setListData(list);
            }
            searchButton.setEnabled(true);
        });

        selectButton.addActionListener(e -> {
            if (CollectionUtils.isEmpty(searchResult)) {
                return;
            }

            if (searchResultList.getSelectedIndex() < 0) {
                return;
            }
            File selectedFile = searchResult.get(searchResultList.getSelectedIndex());
            spreadsheetIdField.setText(selectedFile.getId());

            hideSearchList();
        });

        cancelButton.addActionListener(e -> hideSearchList());

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        int labelWidth = 100;
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(spreadsheetIdLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup()
                                        .addComponent(spreadsheetIdField, 320, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(searchResultScrollPane, 320, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(selectButton)
                                                .addGap(10)
                                                .addComponent(cancelButton)
                                        )
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(searchButton)

                                .addContainerGap()
                        )
                        .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(costLimitLabel, labelWidth, labelWidth, labelWidth)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(costLimitField, 100, 100, 100)
                                .addContainerGap()
                        ));


        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(spreadsheetIdLabel)
                                        .addComponent(spreadsheetIdField)
                                        .addComponent(searchButton)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(searchResultScrollPane, 80, 80, 80)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(selectButton)
                                        .addComponent(cancelButton)
                                )
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(costLimitLabel)
                                        .addComponent(costLimitField)
                                )
                                .addContainerGap()));

        UITools.addListener2Textfields(this);
    }

    public void hideSearchList() {
        searchResultScrollPane.setVisible(false);
        selectButton.setVisible(false);
        cancelButton.setVisible(false);
        searchButton.setEnabled(true);
        spreadsheetIdField.setEnabled(true);
    }

    public void showSearchList() {
        searchButton.setEnabled(false);
        spreadsheetIdField.setEnabled(false);
        searchResultScrollPane.setVisible(true);
        selectButton.setVisible(true);
        cancelButton.setVisible(true);
        selectButton.setEnabled(false);
    }

    public void collectData() {
        SystemSettings systemSettings = SystemSettings.reload();
        systemSettings.setSelfOrderSpreadsheetId(AppScript.getSpreadId(spreadsheetIdField.getText().trim()));
        systemSettings.setSelfOrderCostLimit(FloatUtils.parseFloat(costLimitField.getText(), 1));
        systemSettings.save();
    }

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("SelfOrder Settings");
        frame.setSize(500, 180);
        frame.getContentPane().add(new SelfOrderSettingPanel());
        frame.setVisible(true);
    }
}
