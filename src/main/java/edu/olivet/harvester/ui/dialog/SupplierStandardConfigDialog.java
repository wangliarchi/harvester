package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.hunt.model.HuntStandard;
import edu.olivet.harvester.ui.panel.HuntStandardPanel;

import javax.swing.*;
import javax.swing.LayoutStyle.ComponentPlacement;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 11:29 AM
 */
public class SupplierStandardConfigDialog extends BaseDialog {

    public SupplierStandardConfigDialog() {
        super(null, true);

        initComponents();

        UITools.addListener2Textfields(this.getContentPane());
    }


    private void initComponents() {
        this.setTitle(UIText.title("title.standard.settings"));
        this.initButtons();

        HuntStandardPanel bookcdPanel = new HuntStandardPanel(HuntStandard.newBookDefault(), "New Book/CD Seller Rating Standard");
        HuntStandardPanel usedBookcdPanel = new HuntStandardPanel(HuntStandard.usedBookDefault(), UIText.title("Used Book/CD Seller Rating Standard"));
        HuntStandardPanel productPanel = new HuntStandardPanel(HuntStandard.newProductDefault(), UIText.title("Product Seller Rating Standard"));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(okBtn, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                .addComponent(cancelBtn, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
                                        //.addComponent(modePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        //.addComponent(profitPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(productPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(usedBookcdPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(bookcdPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                //.addComponent(modePanel, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
                                .addComponent(bookcdPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(usedBookcdPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(productPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                //.addComponent(profitPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(okBtn)
                                        .addComponent(cancelBtn)))
        );

        getRootPane().setDefaultButton(okBtn);
        pack();
    }




    @Override
    public void ok() {
        doClose();
    }


    public static void main(String args[]) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        UITools.setDialogAttr(new SupplierStandardConfigDialog(), true);
    }


}