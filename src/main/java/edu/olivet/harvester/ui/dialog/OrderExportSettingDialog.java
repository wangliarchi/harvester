package edu.olivet.harvester.ui.dialog;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.export.model.OrderExportParams;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import org.apache.commons.lang3.time.DateUtils;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Data source spreadsheet selection dialog
 *
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2014年10月14日 下午4:51:29
 */
public class OrderExportSettingDialog extends BaseDialog {
    private static final long serialVersionUID = -1L;
    private JList<Country> marketplaceList;
    @Getter
    private List<Country> selectedMarketplaceNames;


    public OrderExportSettingDialog() {
        super(null, true);
        this.initComponents();
        this.initEvents();
        this.setResizable(false);
    }

    @Getter
    private DateTimePicker fromDateTimePicker;
    @Getter
    private DateTimePicker toDateTimePicker;

    private void initComponents() {

        final LocalDate today = LocalDate.now();
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
        this.marketplaceList.setSelectedIndices(indexes);

        JLabel fromLabel = new JLabel();
        fromLabel.setText("From Date");
        DatePickerSettings fromDateSettings = new DatePickerSettings();
        TimePickerSettings fromTimeSettings = new TimePickerSettings();
        fromDateSettings.setAllowEmptyDates(false);
        fromTimeSettings.initialTime = LocalTime.of(7, 0);
        fromDateTimePicker = new DateTimePicker(fromDateSettings, fromTimeSettings);
        fromDateTimePicker.datePicker.setDate(today.minusDays(1));

        JLabel toLabel = new JLabel();
        toLabel.setText("To Date");
        DatePickerSettings toDateSettings = new DatePickerSettings();
        TimePickerSettings toTimeSettings = new TimePickerSettings();
        toDateSettings.setAllowEmptyDates(false);
        toTimeSettings.initialTime = LocalTime.now().minusMinutes(5);
        toDateTimePicker = new DateTimePicker(toDateSettings, toTimeSettings);
        toDateTimePicker.datePicker.setDateToToday();

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
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(fromLabel, 120, 120, Short.MAX_VALUE)
                        .addComponent(fromDateTimePicker, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addContainerGap()
                )
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(toLabel, 120, 120, Short.MAX_VALUE)
                        .addComponent(toDateTimePicker, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addContainerGap()
                )
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                        .addContainerGap()
                )
        );


        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(spreadPane, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(fromLabel)
                                .addComponent(fromDateTimePicker)
                        )
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(toLabel)
                                .addComponent(toDateTimePicker)
                        )
                        .addPreferredGap(ComponentPlacement.UNRELATED)
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

    @Getter
    private OrderExportParams orderExportParams;

    @Override
    public void ok() {

        selectedMarketplaceNames = this.marketplaceList.getSelectedValuesList();
        LocalDate fromDate = fromDateTimePicker.getDatePicker().getDate();
        LocalTime fromTime = fromDateTimePicker.getTimePicker().getTime();
        LocalDate toDate = toDateTimePicker.getDatePicker().getDate();
        LocalTime toTime = toDateTimePicker.getTimePicker().getTime();

        if (selectedMarketplaceNames.size() == 0) {
            UITools.error(UIText.message("No marketplace selected"), UIText.title("title.conf_error"));
            return;
        }
        if (fromDate == null) {
            UITools.error("Please choose from date!");
            return;
        }

        if (fromTime == null) {
            UITools.error("Please choose from time!");
            return;
        }

        if (toDate == null) {
            UITools.error("Please choose to date!");
            return;
        }

        if (toTime == null) {
            UITools.error("Please choose to time!");
            return;
        }


        orderExportParams = new OrderExportParams();
        orderExportParams.setMarketplaces(selectedMarketplaceNames);


        Instant instant = fromTime.atDate(LocalDate.of(fromDate.getYear(), fromDate.getMonth(), fromDate.getDayOfMonth()))
                .atZone(ZoneId.systemDefault()).toInstant();
        Date from = Date.from(instant);

        if (from.after(DateUtils.addMinutes(new Date(), -5))) {
            UITools.error("From time should be at least 5 minutes before current time");
            return;
        }

        Instant toInstant = toTime.atDate(LocalDate.of(toDate.getYear(), toDate.getMonth(), toDate.getDayOfMonth()))
                .atZone(ZoneId.systemDefault()).toInstant();
        Date to = Date.from(toInstant);

        if (to.after(DateUtils.addMinutes(new Date(), -5))) {
            UITools.error("To time should be at least 5 minutes before current time");
            return;
        }

        if (from.after(to)) {
            UITools.error("From time should be before to time.");
            return;
        }
        orderExportParams.setFromDate(from);
        orderExportParams.setToDate(to);


        this.setVisible(false);
        ok = true;
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();

        OrderExportSettingDialog dialog = UITools.setDialogAttr(new OrderExportSettingDialog());
        dialog.pack();
        System.out.println(dialog.getOrderExportParams());

    }
}
