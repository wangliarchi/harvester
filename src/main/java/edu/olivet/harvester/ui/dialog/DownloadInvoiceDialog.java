package edu.olivet.harvester.ui.dialog;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.model.BuyerAccountSetting;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.export.model.OrderExportParams;
import edu.olivet.harvester.finance.model.DownloadParams;
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
public class DownloadInvoiceDialog extends BaseDialog {
    private static final long serialVersionUID = -1L;
    private JList<Account> buyerList;
    @Getter
    private List<Account> selectedBuyerAccounts;


    public DownloadInvoiceDialog() {
        super(null, true);

        this.initComponents();
        this.initEvents();
        this.setResizable(false);
    }


    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;

    private void initComponents() {

        final LocalDate today = LocalDate.now();
        final JPanel spreadPane = new JPanel();
        final JScrollPane spreadScrollPane = new JScrollPane();

        buyerList = new JList<>();

        this.initButtons();


        setTitle(UIText.title("Select Marketplace"));


        List<BuyerAccountSetting> buyerAccountSettings = BuyerAccountSettingUtils.load().getAccountSettings();
        Account[] buyers = new Account[buyerAccountSettings.size()];
        int[] indexes = new int[buyers.length];
        for (int i = 0; i < buyerAccountSettings.size(); i++) {
            Account buyer = buyerAccountSettings.get(i).getBuyerAccount();
            buyers[i] = buyer;
            indexes[i] = i;
        }

        spreadPane.setBorder(BorderFactory.createTitledBorder(UIText.title("Select Marketplaces")));
        this.buyerList.setListData(buyers);
        this.buyerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.buyerList.setSelectedIndices(indexes);

        JLabel fromLabel = new JLabel();
        fromLabel.setText("From Date");
        DatePickerSettings fromDateSettings = new DatePickerSettings();
        fromDateSettings.setAllowEmptyDates(false);

        fromDatePicker = new DatePicker(fromDateSettings);
        fromDatePicker.setDate(today.minusDays(10));

        JLabel toLabel = new JLabel();
        toLabel.setText("To Date");
        DatePickerSettings toDateSettings = new DatePickerSettings();
        toDateSettings.setAllowEmptyDates(false);
        toDatePicker = new DatePicker(toDateSettings);
        toDatePicker.setDate(today.minusDays(3));

        spreadScrollPane.setViewportView(buyerList);

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
                        .addComponent(fromDatePicker, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addContainerGap()
                )
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(toLabel, 120, 120, Short.MAX_VALUE)
                        .addComponent(toDatePicker, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
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
                                .addComponent(fromDatePicker)
                        )
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(toLabel)
                                .addComponent(toDatePicker)
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

        buyerList.addMouseListener(mouseListener);

    }

    @Getter
    private DownloadParams downloadParams;

    @Override
    public void ok() {

        selectedBuyerAccounts = this.buyerList.getSelectedValuesList();
        LocalDate fromDate = fromDatePicker.getDate();
        LocalDate toDate = toDatePicker.getDate();

        if (selectedBuyerAccounts.size() == 0) {
            UITools.error(UIText.message("No buyer accounts selected"), UIText.title("title.conf_error"));
            return;
        }
        if (fromDate == null) {
            UITools.error("Please choose from date!");
            return;
        }


        if (toDate == null) {
            UITools.error("Please choose to date!");
            return;
        }


        downloadParams = new DownloadParams();
        downloadParams.setBuyerAccounts(selectedBuyerAccounts);


        Instant instant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Date from = Date.from(instant);

        if (from.after(DateUtils.addMinutes(new Date(), -5))) {
            UITools.error("From time should be at least 5 minutes before current time");
            return;
        }

        Instant toInstant = toDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Date to = Date.from(toInstant);

        if (to.after(DateUtils.addMinutes(new Date(), -5))) {
            UITools.error("To time should be at least 5 minutes before current time");
            return;
        }

        if (from.after(to)) {
            UITools.error("From time should be before to time.");
            return;
        }
        downloadParams.setFromDate(from);
        downloadParams.setToDate(to);


        this.setVisible(false);
        ok = true;
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();

        DownloadInvoiceDialog dialog = UITools.setDialogAttr(new DownloadInvoiceDialog());
        dialog.pack();
        System.out.println(dialog.getDownloadParams());

    }
}
