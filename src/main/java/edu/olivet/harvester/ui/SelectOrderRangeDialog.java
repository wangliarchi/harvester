package edu.olivet.harvester.ui;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.spreadsheet.OrderRange;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import java.util.*;

/**
 * Order range selection dialog
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
class SelectOrderRangeDialog extends BaseDialog {
    @Getter private List<OrderRange> orderRanges;
    private final Map<Integer, JComboBox<String>> sheetNameMap = new LinkedHashMap<>();
    private final Map<Integer, JTextField> beginRowMap = new HashMap<>();
    private final Map<Integer, JTextField> endRowMap = new HashMap<>();
    private static final int MAX_COUNT = 7;

    SelectOrderRangeDialog(String[] sheetNames) {
        super(null, true);

        JPanel panel = new JPanel();
        panel.setBorder(UITools.createTitledBorder(null));
        this.setTitle("Order Fulfillment Range Selection Dialog");

        this.initButtons();

        GroupLayout paneLayout = new GroupLayout(panel);
        ParallelGroup hPG = paneLayout.createParallelGroup(Alignment.LEADING);
        SequentialGroup vSG = paneLayout.createSequentialGroup();

        for (int i = 0; i < MAX_COUNT; i++) {
            JLabel sheetNameLbl = new JLabel("Sheet " + (i + 1) + ": ");
            JLabel beginRowLbl = new JLabel("From Row: ");
            JLabel endRowLbl = new JLabel("To Row: ");
            JComboBox<String> sheetNameBox = new JComboBox<>(new DefaultComboBoxModel<>(sheetNames));
            if (i == 0 && sheetNames.length > 1) {
                sheetNameBox.setSelectedIndex(1);
            }
            JTextField beginRow = new JTextField();
            JTextField endRow = new JTextField();

            sheetNameMap.put(i, sheetNameBox);
            beginRowMap.put(i, beginRow);
            endRowMap.put(i, endRow);

            hPG.addGroup(paneLayout.createSequentialGroup()
                .addComponent(sheetNameLbl).addGap(5).addComponent(sheetNameBox, 150, 150, 150).addGap(5)
                .addComponent(beginRowLbl).addGap(5).addComponent(beginRow, 100, 100, 100).addGap(5)
                .addComponent(endRowLbl).addGap(5).addComponent(endRow, 100, 100, 100));

            vSG.addGroup(paneLayout.createParallelGroup(Alignment.BASELINE)
                .addComponent(sheetNameLbl).addComponent(sheetNameBox, 30, 30, 30)
                .addComponent(beginRowLbl).addComponent(beginRow, 30, 30, 30)
                .addComponent(endRowLbl).addComponent(endRow, 30, 30, 30))
                .addGap(5);
        }

        paneLayout.setHorizontalGroup(paneLayout.createParallelGroup(Alignment.LEADING).addGap(10).addGroup(hPG));
        paneLayout.setVerticalGroup(paneLayout.createParallelGroup(Alignment.LEADING).addGroup(vSG));
        panel.setLayout(paneLayout);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                    .addGap(10)
                    .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH))
                .addComponent(panel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGap(5)
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(cancelBtn, 30, 30, 30).addComponent(okBtn, 30, 30, 30)))
        );
        UITools.addListener2Textfields(panel);
        getRootPane().setDefaultButton(okBtn);
        pack();
    }

    public void ok() {
        List<OrderRange> ranges = new ArrayList<>();
        for (int i = 0; i < MAX_COUNT; i++) {
            String sheetName = (String) sheetNameMap.get(i).getSelectedItem();
            if (StringUtils.isNotBlank(sheetName)) {
                String beginRowText = beginRowMap.get(i).getText().trim();
                Integer beginRow = StringUtils.isNotBlank(beginRowText) ? Integer.parseInt(beginRowText) : null;

                String endRowText = endRowMap.get(i).getText().trim();
                Integer endRow = StringUtils.isNotBlank(endRowText) ? Integer.parseInt(endRowText) : null;

                ranges.add(new OrderRange(sheetName, beginRow, endRow));
            }
        }

        if (CollectionUtils.isNotEmpty(ranges)) {
            this.orderRanges = ranges;
            this.doClose();
        } else {
            UITools.error("Please select at least one sheet for order fulfillment.");
        }
    }
    
    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        SelectOrderRangeDialog selectOrderRangeDialog = new SelectOrderRangeDialog(new String[] {"", "09/14", "09/13", "09/12", "09/11"});
        System.out.println(UITools.setDialogAttr(selectOrderRangeDialog, true).getOrderRanges());
        System.exit(0);
    }
}
