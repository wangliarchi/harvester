package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.spreadsheet.model.OrderRange;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/11/17 11:45 AM
 */
public class SelectOrderRangePanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectOrderRangePanel.class);

    @Getter
    private List<OrderRange> orderRanges;
    private final Map<Integer, JComboBox<String>> sheetNameMap = new LinkedHashMap<>();
    private final Map<Integer, JTextField> beginRowMap = new HashMap<>();
    private final Map<Integer, JTextField> endRowMap = new HashMap<>();
    private static final int MAX_COUNT = 7;

    @Setter
    @Getter
    private List<String> sheetNames = new ArrayList<>();


    public SelectOrderRangePanel() {
        super(null, true);

        initComponents();


    }

    private JTextField googleSheetTextField;

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createTitledBorder("Select Sheets")));

        GroupLayout paneLayout = new GroupLayout(this);
        GroupLayout.ParallelGroup hPG = paneLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup vSG = paneLayout.createSequentialGroup();


        for (int i = 0; i < MAX_COUNT; i++) {
            JLabel sheetNameLbl = new JLabel("Sheet " + (i + 1) + ": ");
            JLabel beginRowLbl = new JLabel("From Row: ");
            JLabel endRowLbl = new JLabel("To Row: ");
            JComboBox<String> sheetNameBox = new JComboBox<>(new DefaultComboBoxModel<>(sheetNames.toArray(new String[sheetNames.size()])));
            if (i == 0 && sheetNames.size() > 1) {
                sheetNameBox.setSelectedIndex(1);
            }
            JTextField beginRow = new JTextField();
            JTextField endRow = new JTextField();

            sheetNameMap.put(i, sheetNameBox);
            beginRowMap.put(i, beginRow);
            endRowMap.put(i, endRow);

            hPG.addGroup(paneLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(sheetNameLbl).addGap(5).addComponent(sheetNameBox, 150, 150, 150).addGap(5)
                    .addComponent(beginRowLbl).addGap(5).addComponent(beginRow, 100, 100, 100).addGap(5)
                    .addComponent(endRowLbl).addGap(5).addComponent(endRow, 100, 100, 100)
                    .addContainerGap()
            );

            vSG.addGroup(paneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(sheetNameLbl).addComponent(sheetNameBox, 30, 30, 30)
                    .addComponent(beginRowLbl).addComponent(beginRow, 30, 30, 30)
                    .addComponent(endRowLbl).addComponent(endRow, 30, 30, 30))
                    .addGap(5);
        }

        paneLayout.setHorizontalGroup(paneLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(10).addGroup(hPG));
        paneLayout.setVerticalGroup(paneLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(vSG));
        this.setLayout(paneLayout);
    }

    public void resetSheets() {

    }

    public void setSheetOptions(List<String> sheetNames) {
        this.sheetNames = sheetNames;
        for (int i = 0; i < MAX_COUNT; i++) {
            sheetNameMap.get(i).setModel(new DefaultComboBoxModel<>(sheetNames.toArray(new String[sheetNames.size()])));
        }
    }

    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("");
        frame.setSize(400, 580);
        SelectOrderRangePanel selectOrderRangePanel = new SelectOrderRangePanel();
        frame.getContentPane().add(selectOrderRangePanel);
        frame.setVisible(true);
    }


}
