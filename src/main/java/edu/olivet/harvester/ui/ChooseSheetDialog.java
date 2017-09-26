package edu.olivet.harvester.ui;

import com.google.api.services.drive.model.File;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.google.DataSource;
import edu.olivet.foundations.google.DataSource.DataSourceCategory;
import edu.olivet.foundations.google.SpreadService;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.model.Spreadsheet;
import lombok.Getter;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * Data source spreadsheet selection dialog
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/26/2017 8:51 AM
 */
public class ChooseSheetDialog extends BaseDialog {
    private static final long serialVersionUID = -1L;

    private final List<File> spreads;
    private final SpreadService spreadService;

    private JList<String> spreadList;
    private JList<String> sheetNameList;

    @Getter private Spreadsheet selectedSheets;

    public ChooseSheetDialog(List<File> spreads, SpreadService spreadService) {
        super(null, true);
        this.spreadService = spreadService;
        this.spreads = spreads;
        this.initComponents();
        this.setResizable(false);
    }

    private void initComponents() {
        final JPanel spreadPane = new JPanel();
        final JScrollPane spreadScrollPane = new JScrollPane();
        spreadList = new JList<>();
        sheetNameList = new JList<>();

        final JPanel sheetPane = new JPanel();
        final JScrollPane sheetScrollPane = new JScrollPane();
        this.initButtons();

        JButton clearCacheBtn = new JButton(UIText.label("datasource.clear"));
        clearCacheBtn.setToolTipText(UIText.tooltip("tooltip.clear.sheetcache"));
        clearCacheBtn.addActionListener(e -> {
            // Clear cache and reload
            spreadService.clearCache();
            formsValueChanged();
        });

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(UIText.title("title.select.sheet"));
        spreadPane.setBorder(BorderFactory.createTitledBorder(UIText.title("title.spread")));

        spreadList.addListSelectionListener(evt -> {
            if (evt.getValueIsAdjusting()) {
                formsValueChanged();
            }
        });
        String[] strings = new String[spreads.size()];
        for (int i = 0; i < spreads.size(); i++) {
            File spread = spreads.get(i);
            strings[i] = spread.getName() + "(Last Update: " + Dates.toDateTime(spread.getModifiedTime().getValue()) + ")";
        }

        this.spreadList.setListData(strings);
        this.spreadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    ok();
                }
            }
        };
        sheetNameList.addMouseListener(mouseListener);
        spreadScrollPane.setViewportView(spreadList);

        GroupLayout spreadLayout = new GroupLayout(spreadPane);
        spreadPane.setLayout(spreadLayout);
        spreadLayout.setHorizontalGroup(
            spreadLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(spreadScrollPane));
        spreadLayout.setVerticalGroup(
            spreadLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(spreadScrollPane));

        sheetPane.setBorder(BorderFactory.createTitledBorder(UIText.title("title.sheet")));
        sheetScrollPane.setViewportView(sheetNameList);
        GroupLayout sheetLayout = new GroupLayout(sheetPane);
        sheetPane.setLayout(sheetLayout);
        sheetLayout.setHorizontalGroup(
            sheetLayout.createParallelGroup(Alignment.LEADING).addComponent(sheetScrollPane)
        );
        sheetLayout.setVerticalGroup(
            sheetLayout.createParallelGroup(Alignment.LEADING).addComponent(sheetScrollPane)
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        layout.setHorizontalGroup(layout
            .createParallelGroup(Alignment.TRAILING)
            .addComponent(spreadPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addComponent(sheetPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(clearCacheBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                .addGap(10)
                .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                .addGap(10)
                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
            layout.createSequentialGroup()
                .addComponent(spreadPane, 250, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(sheetPane, 300, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(clearCacheBtn).addComponent(cancelBtn).addComponent(okBtn))));

        getContentPane().setLayout(layout);
        getRootPane().setDefaultButton(okBtn);
        pack();
    }

    private void formsValueChanged() {
        int selected = spreadList.getSelectedIndex();
        if (selected < 0) {
            return;
        }

        try {
            File spread = this.spreads.get(selected);
            List<String> sheetNames = spreadService.getSheetNames(spread.getId(), Constants.RND_EMAIL);
            int count = sheetNames.size();
            if (count == 0) {
                this.sheetNameList.setListData(new String[0]);
                return;
            }

            this.sheetNameList.setListData(sheetNames.toArray(new String[sheetNames.size()]));
        } catch (IllegalStateException | BusinessException e) {
            UITools.error(UIText.message("message.error.readsheet", Strings.getExceptionMsg(e)));
        }
    }

    @Override
    public void ok() {
        int spreadIndex = this.spreadList.getSelectedIndex();
        int sheetIndex = this.sheetNameList.getSelectedIndex();
        if (spreadIndex >= 0 && sheetIndex >= 0) {
            File spread = spreads.get(spreadIndex);
            List<String> sheetNames = this.sheetNameList.getSelectedValuesList();
            selectedSheets = new Spreadsheet(spread.getId(), spread.getName(), sheetNames);
            this.setVisible(false);
        } else {
            UITools.error(UIText.message("message.error.nosheetselected"), UIText.title("title.conf_error"));
        }
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();

        SpreadService spreadService = ApplicationContext.getBean(SpreadService.class);
        String dataSourceId = new DataSource(DataSourceCategory.Order).id();
        String accSid = args.length > 0 ? args[0] : "18";
        Country country = args.length > 1 ? Country.valueOf(args[1]) : Country.US;

        List<File> spreads = spreadService.getAvailableSheets(accSid, country, dataSourceId, Constants.RND_EMAIL);
        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreads, spreadService));
        System.out.println(dialog.getSelectedSheets());
    }
}
