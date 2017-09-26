package edu.olivet.harvester.ui;

import com.google.api.services.drive.model.File;
import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.google.DataSource;
import edu.olivet.foundations.google.DataSource.DataSourceCategory;
import edu.olivet.foundations.google.SpreadService;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;


import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data source spreadsheet selection dialog
 *
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2014年10月14日 下午4:51:29
 */
public class ChooseSheetDialog extends BaseDialog {
    private static final long serialVersionUID = -1L;

    private final List<Spreadsheet> spreadsheets;

    private JList<String> jSpreadList;
    private JList<String> jSheetList;

    private List<Worksheet> selectedWorksheets;

    private AppScript appScript;

    public ChooseSheetDialog(List<Spreadsheet> spreadsheets,AppScript appScript) {
        super(null, true);

        this.spreadsheets = spreadsheets;
        this.appScript = appScript;

        this.initComponents();
        this.setResizable(false);
    }




    private void initComponents() {


        final JPanel spreadPane = new JPanel();
        final JScrollPane spreadScrollPane = new JScrollPane();

        jSpreadList = new JList<>();
        jSheetList = new JList<>();

        final JPanel sheetPane = new JPanel();
        final JScrollPane sheetScrollPane = new JScrollPane();
        this.initButtons();

        JButton clearCacheBtn = new JButton(UIText.label("datasource.refresh"));
        clearCacheBtn.setToolTipText(UIText.tooltip("tooltip.clear.sheetcache"));

        //refresh selected spreadsheet
        clearCacheBtn.addActionListener(e -> {
            //find selected spreadsheet
            int spreadIndex = this.jSpreadList.getSelectedIndex();
            Spreadsheet spreadsheet = spreadsheets.get(spreadIndex);
            spreadsheet = appScript.reloadSpreadsheet(spreadsheet.getSpreadsheetId());
            spreadsheets.set(spreadIndex,spreadsheet);

            formsValueChanged();

        });

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(UIText.title("title.select.sheet"));
        spreadPane.setBorder(BorderFactory.createTitledBorder(UIText.title("title.spread")));

        jSpreadList.addListSelectionListener(evt -> {
            if (evt.getValueIsAdjusting()) {
                formsValueChanged();
            }
        });
        String[] strings = new String[spreadsheets.size()];


        for (int i = 0; i < spreadsheets.size(); i++) {
            Spreadsheet spreadsheet = spreadsheets.get(i);
            strings[i] = spreadsheet.getTitle();
        }

        this.jSpreadList.setListData(strings);
        this.jSpreadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);



        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                System.out.println(mouseEvent.getClickCount());
                if (mouseEvent.getClickCount() == 2) {
                    ok();
                }
            }
        };


        jSheetList.addMouseListener(mouseListener);
        spreadScrollPane.setViewportView(jSpreadList);

        GroupLayout spreadLayout = new GroupLayout(spreadPane);
        spreadPane.setLayout(spreadLayout);
        spreadLayout.setHorizontalGroup(
                spreadLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(spreadScrollPane));
        spreadLayout.setVerticalGroup(
                spreadLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(spreadScrollPane));

        sheetPane.setBorder(BorderFactory.createTitledBorder(UIText.title("title.sheet")));

        sheetScrollPane.setViewportView(jSheetList);

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
                .addComponent(spreadPane, 400, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(sheetPane, 400, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(clearCacheBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addGap(10)
                        .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addGap(10)
                        .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)));


        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(

                layout.createSequentialGroup()
                        .addComponent(spreadPane, 150, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(sheetPane, 300, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(clearCacheBtn).addComponent(cancelBtn).addComponent(okBtn))));

        getContentPane().setLayout(layout);
        getRootPane().setDefaultButton(okBtn);
        pack();



        //set first spreadsheet selected by defaut
        this.jSpreadList.setSelectedIndex(0);
        formsValueChanged();
    }

    private void formsValueChanged() {

        int selected = jSpreadList.getSelectedIndex();

        if (selected < 0) {
            return;
        }

        Spreadsheet spread = this.spreadsheets.get(selected);
        List<String> sheetNames = spread.getOrderSheetNames();

        int count = sheetNames.size();

        if (count == 0) {
            this.jSheetList.setListData(new String[0]);
            return;
        }

        this.jSheetList.setListData(sheetNames.toArray(new String[sheetNames.size()]));


        //default to select today's sheet
        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd");
        LocalDate localDate = LocalDate.now();
        String todaySheetName =  df.format(localDate);

        int index = sheetNames.indexOf(todaySheetName);
        this.jSheetList.setSelectedIndex(index);
//        this.jSheetList.setSelectionInterval(0, count - 1);

    }

    @Override
    public void ok() {
        int spreadIndex = this.jSpreadList.getSelectedIndex();
        int sheetIndex = this.jSheetList.getSelectedIndex();

        selectedWorksheets = new ArrayList<>();

        if (spreadIndex >= 0 && sheetIndex >= 0) {
            Spreadsheet spreadsheet = spreadsheets.get(spreadIndex);
            List<String> sheetNames = this.jSheetList.getSelectedValuesList();

            for(String sheetName : sheetNames) {
                selectedWorksheets.add( new Worksheet(spreadsheet,sheetName));
            }

            //selectedSheets = new Spreadsheet(spread.getSpreadsheetId(), spread.getTitle(), sheetNames);
            this.setVisible(false);

            ok = true;
        } else {
            UITools.error(UIText.message("message.error.nosheetselected"), UIText.title("title.conf_error"));
        }
    }

    public List<Worksheet> getSelectedSheets() {
        return this.selectedWorksheets;
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
        System.out.println(dialog.getSelectedSheets());
    }
}
