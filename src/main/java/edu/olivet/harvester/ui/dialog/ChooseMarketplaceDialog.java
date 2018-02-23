package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;

import edu.olivet.harvester.utils.Settings;
import lombok.Getter;


import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.*;

import java.util.List;

/**
 * Data source spreadsheet selection dialog
 *
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2014年10月14日 下午4:51:29
 */
public class ChooseMarketplaceDialog extends BaseDialog {
    private static final long serialVersionUID = -1L;
    private JList<Country> marketplaceList;
    @Getter
    private List<Country> selectedMarketplaceNames;


    public ChooseMarketplaceDialog() {
        super(null, true);
        this.initComponents();
        this.initEvents();
        this.setResizable(false);
    }


    private void initComponents() {
        final JPanel spreadPane = new JPanel();
        final JScrollPane spreadScrollPane = new JScrollPane();

        marketplaceList = new JList<>();

        this.initButtons();


        setTitle(UIText.title("Select Marketplace"));

        Country[] countries = new Country[Settings.load().listAllCountries().size()];
        int[] indexes = new int[countries.length];
        for (int i = 0; i < Settings.load().listAllCountries().size(); i++) {
            Country country = Settings.load().listAllCountries().get(i);
            countries[i] = country;
            indexes[i] = i;
        }

        spreadPane.setBorder(BorderFactory.createTitledBorder(UIText.title("Select Marketplaces")));
        this.marketplaceList.setListData(countries);
        this.marketplaceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //this.marketplaceList.setSelectedIndices(indexes);


        spreadScrollPane.setViewportView(marketplaceList);

        GroupLayout spreadLayout = new GroupLayout(spreadPane);
        spreadPane.setLayout(spreadLayout);
        spreadLayout.setHorizontalGroup(
                spreadLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(spreadScrollPane));
        spreadLayout.setVerticalGroup(
                spreadLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(spreadScrollPane));


        GroupLayout layout = new GroupLayout(getContentPane());


        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addComponent(spreadPane, 380, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)

                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addContainerGap()
                )
        );


        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(spreadPane, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)

                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(cancelBtn)
                                .addComponent(okBtn))
                        .addContainerGap()));

        getContentPane().setLayout(layout);
        getRootPane().setDefaultButton(okBtn);
        pack();

    }

    private void initEvents() {
        //setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    ok();
                }
            }
        };

        marketplaceList.addMouseListener(mouseListener);

    }


    @Override
    public void ok() {

        selectedMarketplaceNames = this.marketplaceList.getSelectedValuesList();

        if (selectedMarketplaceNames.size() == 0) {
            UITools.error(UIText.message("No marketplace selected"), UIText.title("title.conf_error"));
            return;
        }

        this.setVisible(false);
        ok = true;
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();

        ChooseMarketplaceDialog dialog = UITools.setDialogAttr(new ChooseMarketplaceDialog());
        dialog.pack();
        System.out.println(dialog.getSelectedMarketplaceNames());

    }
}
