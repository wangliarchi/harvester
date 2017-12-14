package edu.olivet.harvester.ui.dialog;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.spreadsheet.model.Spreadsheet;
import edu.olivet.harvester.spreadsheet.model.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data source spreadsheet selection dialog
 *
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2014年10月14日 下午4:51:29
 */
public class ChooseSheetDialog extends BaseDialog {
    private static final long serialVersionUID = -1L;

    private final List<Spreadsheet> spreadsheets;

    private JList<String> spreadList;
    private JList<String> sheetNameList;
    public boolean continueToNext = false;
    @Getter
    private List<Worksheet> selectedWorksheets;

    private AppScript appScript;

    public ChooseSheetDialog(List<Spreadsheet> spreadsheets, AppScript appScript) {
        super(null, true);

        this.spreadsheets = spreadsheets;
        this.appScript = appScript;

        this.initComponents();
        this.initEvents();
        this.setResizable(false);
    }

    JButton continueBtn;
    JButton clearCacheBtn;

    private void initComponents() {


        final JPanel spreadPane = new JPanel();
        final JScrollPane spreadScrollPane = new JScrollPane();

        spreadList = new JList<>();
        sheetNameList = new JList<>();

        final JPanel sheetPane = new JPanel();
        final JScrollPane sheetScrollPane = new JScrollPane();
        this.initButtons();

        clearCacheBtn = new JButton(UIText.label("datasource.refresh"));
        clearCacheBtn.setToolTipText(UIText.tooltip("tooltip.clear.sheetcache"));

        continueBtn = new JButton(UIText.label("Continue to Select Range"));
        clearCacheBtn.setToolTipText(UIText.tooltip("Continue to select range to mark status or submit."));


        setTitle(UIText.title("title.select.sheet"));
        spreadPane.setBorder(BorderFactory.createTitledBorder(UIText.title("title.spread")));
        String[] strings = new String[spreadsheets.size()];
        for (int i = 0; i < spreadsheets.size(); i++) {
            Spreadsheet spreadsheet = spreadsheets.get(i);
            strings[i] = spreadsheet.getTitle();
        }

        this.spreadList.setListData(strings);
        this.spreadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


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


        continueBtn.setVisible(false);

        layout.setHorizontalGroup(layout
                .createParallelGroup(Alignment.TRAILING)
                .addComponent(spreadPane, 480, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(sheetPane, 480, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearCacheBtn, UITools.BUTTON_WIDTH + 10, UITools.BUTTON_WIDTH + 10, UITools.BUTTON_WIDTH + 10)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)

                        .addComponent(continueBtn)
                ));


        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
                layout.createSequentialGroup()
                        .addComponent(spreadPane, 150, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(sheetPane, 300, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(clearCacheBtn).addComponent(cancelBtn).addComponent(okBtn).addComponent(continueBtn))));

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

        sheetNameList.addMouseListener(mouseListener);

        spreadList.addListSelectionListener(evt -> {
            if (evt.getValueIsAdjusting()) {
                formsValueChanged();
            }
        });

        //refresh selected spreadsheet
        clearCacheBtn.addActionListener(e -> {
            //find selected spreadsheet
            int spreadIndex = this.spreadList.getSelectedIndex();
            Spreadsheet spreadsheet = spreadsheets.get(spreadIndex);
            spreadsheet = appScript.reloadSpreadsheet(spreadsheet.getSpreadsheetId());
            spreadsheets.set(spreadIndex, spreadsheet);

            formsValueChanged();

        });

        continueBtn.addActionListener(e -> {
            continueToNext = true;
            ok();

        });
        //set first spreadsheet selected by default
        this.spreadList.setSelectedIndex(0);
        formsValueChanged();
    }

    public void setSheetNameListSelectionMode(int selectionMode) {
        this.sheetNameList.setSelectionMode(selectionMode);
    }

    public void setSelectedSpreadsheet(String spreadsheetTitle) {
        this.spreadList.setSelectedValue(spreadsheetTitle, true);
    }

    public void setSelectedSheet(String sheetName) {
        this.sheetNameList.setSelectedValue(sheetName, true);
    }

    private void formsValueChanged() {

        int selected = spreadList.getSelectedIndex();

        if (selected < 0) {
            return;
        }

        Spreadsheet spread = this.spreadsheets.get(selected);
        List<String> sheetNames = spread.getOrderSheetNames();

        int count = sheetNames.size();

        if (count == 0) {
            this.sheetNameList.setListData(new String[0]);
            return;
        }

        this.sheetNameList.setListData(sheetNames.toArray(new String[sheetNames.size()]));


        //default to select today's sheet
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd");
        LocalDate localDate = LocalDate.now();
        String todaySheetName = df.format(localDate);

        int index = sheetNames.indexOf(todaySheetName);
        this.sheetNameList.setSelectedIndex(index);

    }

    public void showContinueBtn(boolean show) {
        continueBtn.setVisible(show);
        continueBtn.invalidate();
    }

    @Override
    public void ok() {
        int spreadIndex = this.spreadList.getSelectedIndex();
        int sheetIndex = this.sheetNameList.getSelectedIndex();

        selectedWorksheets = new ArrayList<>();

        if (spreadIndex >= 0 && sheetIndex >= 0) {
            Spreadsheet spreadsheet = spreadsheets.get(spreadIndex);
            List<String> sheetNames = this.sheetNameList.getSelectedValuesList();

            for (String sheetName : sheetNames) {
                selectedWorksheets.add(new Worksheet(spreadsheet, sheetName));
            }

            this.setVisible(false);

            ok = true;
        } else {
            UITools.error(UIText.message("message.error.nosheetselected"), UIText.title("title.conf_error"));
        }
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();

        List<String> spreadsheetIds = Settings.load().listAllSpreadsheets();
        List<Spreadsheet> spreadsheets = new ArrayList<>();

        AppScript appScript = ApplicationContext.getBean(AppScript.class);

        for (String spreadsheetId : spreadsheetIds) {
            try {
                Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
                spreadsheets.add(spreadsheet);
            } catch (Exception e) {
                System.out.println(spreadsheetId + " is invalid." + e.getMessage());

            }
        }

        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));
        System.out.println(dialog.getSelectedWorksheets());

    }
}
