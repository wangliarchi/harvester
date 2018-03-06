package edu.olivet.harvester.ui.dialog;

import com.google.common.collect.Lists;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.ui.panel.SelfOrderRecordPanel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/11/17 10:50 AM
 */
public class SelectSelfOrderDialog extends BaseDialog {
    //private static final Logger LOGGER = LoggerFactory.getLogger(AddOrderSubmissionTaskDialog.class);
    @Getter
    private final List<SelfOrder> selfOrders;
    private Set<SelfOrderRecordPanel> selfOrderRecordPanels = new HashSet<>();

    public SelectSelfOrderDialog(List<SelfOrder> selfOrders) {
        super(null, true);
        this.selfOrders = selfOrders;
        initComponents();
    }


    private void initComponents() {
        String title = "Select self-orders";
        this.setTitle(title);
        this.initButtons();

        JButton aboutBtn = UITools.transparent(new JButton("I Need Help", UITools.getIcon("about.png")));
        aboutBtn.setToolTipText("Access official website to get information, tutorial and community help");
        aboutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));


        JPanel innerPanel = new JPanel();

        GroupLayout innerLayout = new GroupLayout(innerPanel);
        innerPanel.setLayout(innerLayout);

        innerPanel.setBorder(BorderFactory.createTitledBorder(null, null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP));

        GroupLayout.ParallelGroup hParallelGroup = innerLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.ParallelGroup vParallelGroup = innerLayout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup vSequentialGroup = innerLayout.createSequentialGroup();

        vSequentialGroup.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);

        JPanel headerPanel = initHeaderPanel();
        //hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING, innerLayout.createSequentialGroup().addComponent(headerPanel));

        //vSequentialGroup.addGroup(innerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE));
        //.addComponent(headerPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        //.addGap(5);


        for (SelfOrder selfOrder : selfOrders) {
            SelfOrderRecordPanel selfOrderRecordPanel = new SelfOrderRecordPanel(selfOrder);
            selfOrderRecordPanels.add(selfOrderRecordPanel);
            hParallelGroup.addGroup(GroupLayout.Alignment.TRAILING,
                    innerLayout.createSequentialGroup().addComponent(selfOrderRecordPanel));
            vSequentialGroup.addGroup(innerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(selfOrderRecordPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                    .addGap(5);
        }


        innerLayout.setHorizontalGroup(hParallelGroup);
        innerLayout.setVerticalGroup(vParallelGroup.addGroup(vSequentialGroup));


        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);


        final JScrollPane innerScrollPane = new JScrollPane();
        int height = Math.min(360, innerPanel.getPreferredSize().height);
        innerScrollPane.setPreferredSize(new Dimension(innerPanel.getPreferredSize().width + 40, height));
        innerScrollPane.setViewportView(innerPanel);
        final int buttonHeight = 30;
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(headerPanel)
                        .addComponent(innerScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addGap(20).addComponent(aboutBtn))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addContainerGap()
                        )
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(headerPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addComponent(innerScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(aboutBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(cancelBtn, buttonHeight, buttonHeight, buttonHeight)
                                        .addComponent(okBtn, buttonHeight, buttonHeight, buttonHeight))
                                .addContainerGap())
        );


        getRootPane().setDefaultButton(okBtn);

        pack();

    }


    private JPanel initHeaderPanel() {

        JPanel headerPanel = new JPanel();
        //add header
        final JLabel sheetNameLabel = new JLabel("Sheet");
        final JLabel rowLabel = new JLabel("Row");
        final JLabel sellerAccountLabel = new JLabel("Account");
        final JLabel sellerNameLabel = new JLabel("Seller Name");
        final JLabel countryLabel = new JLabel("Country");
        final JLabel typeLabel = new JLabel("ASIN");
        final JLabel primeLabel = new JLabel("Buyer Account");

        GroupLayout layout = new GroupLayout(headerPanel);
        headerPanel.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGap(10)
                                .addComponent(sheetNameLabel, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rowLabel, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sellerAccountLabel, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sellerNameLabel, 120, 120, 120)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(countryLabel, 60, 60, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typeLabel, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(primeLabel, 200, 200, 200)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        ));


        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sheetNameLabel)
                                        .addComponent(rowLabel)
                                        .addComponent(sellerAccountLabel)
                                        .addComponent(sellerNameLabel)
                                        .addComponent(countryLabel)
                                        .addComponent(typeLabel)
                                        .addComponent(primeLabel)
                                )
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)));

        return headerPanel;
    }


    @Override
    public void ok() {
        try {
            for (SelfOrderRecordPanel selfOrderRecordPanel : selfOrderRecordPanels) {
                selfOrderRecordPanel.collectData();
            }
            ok = true;
            doClose();
        } catch (Exception e) {
            UITools.error(e.getMessage());
        }
    }

    public static void main(String[] args) {

        SelfOrder selfOrder = new SelfOrder();
        selfOrder.ownerAccountCode = "22US";
        selfOrder.ownerAccountStoreName = "Onlinebohop";
        selfOrder.country = "US";
        selfOrder.asin = "B01K15AN3W";
        selfOrder.buyerAccountCode = "704";


        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new SelectSelfOrderDialog(Lists.newArrayList(selfOrder, selfOrder)), true);
        System.exit(0);
    }


}
