package edu.olivet.harvester.ui;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.fulfill.model.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.model.OrderEnums;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 做单范围高级设置对话框
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Oct 21, 2014 3:08:49 PM
 */
public class SelectRangeDialog extends BaseDialog {
    private static final long serialVersionUID = -4173627453729748332L;
    private FocusListener selectAll;

    public boolean continueToSubmit = false;
    public boolean continueToMarkStatus = false;

    /**
     * 按下Tab键定位到textfield的时候默认全部选中，方便输入
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 15, 2014 11:01:46 AM
     */
    public static class FocusTextField extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            if (e.getSource() instanceof JTextField) {
                ((JTextField) e.getSource()).selectAll();
            }
        }
    }

    private static final FocusTextField FOCUS_LISTENER = new FocusTextField();

    public SelectRangeDialog(Frame parent, boolean modal, AdvancedSubmitSetting settings) {
        super(parent, modal);

        selectAll = new FocusTextField();
        this.initComponents();
        UITools.addListener2Textfields(this.getContentPane());
        this.settings = settings;
        if (this.settings != null) {
            this.initSettings();
        }
    }

    private void initSettings() {
        if (this.settings.getSubmitRange() == ConfigEnums.SubmitRange.ALL) {
            this.allBtn.setSelected(true);
        } else if (this.settings.getSubmitRange() == ConfigEnums.SubmitRange.LimitCount) {
            this.limitCountBtn.setSelected(true);
            this.limitCountTxt.setEnabled(true);
            this.limitCountTxt.setText(String.valueOf(this.settings.getCountLimit()));
        } else if (this.settings.getSubmitRange() == ConfigEnums.SubmitRange.SINGLE) {
            this.singleBtn.setSelected(true);
            this.singleTxt.setEnabled(true);
            this.singleTxt.setText(String.valueOf(this.settings.getSingleRowNo()));
        } else if (this.settings.getSubmitRange() == ConfigEnums.SubmitRange.SCOPE) {
            this.scopeBtn.setSelected(true);
            this.startRowNo.setEnabled(true);
            this.endRowNo.setEnabled(true);
            if (this.settings.getStartRowNo() > 0) {
                this.startRowNo.setText(String.valueOf(this.settings.getStartRowNo()));
            }
            if (this.settings.getEndRowNo() > 0) {
                this.endRowNo.setText(String.valueOf(this.settings.getEndRowNo()));
            }
        } else {
            this.multiBtn.setSelected(true);
            this.multiRowsTxt.setEnabled(true);
            this.multiRowsTxt.setText(this.settings.getMultiRows());
        }

        if (settings.getStatusFilterValue() == null) {
            this.typeAll.setSelected(true);
        } else {
            switch (settings.getStatusFilterValue()) {
                case PrimeSeller:
                    this.typePrime.setSelected(true);
                    break;
                case CommonSeller:
                    this.typePt.setSelected(true);
                    break;
                case International:
                    this.typeIntl.setSelected(true);
                    break;
                case SellerIsBetterWorld:
                    this.typeBw.setSelected(true);
                    break;
                case SellerIsHalf:
                    this.typeHalf.setSelected(true);
                    break;
                case BuyAndTransfer:
                    this.typeMhzy.setSelected(true);
                    break;
                default:
                    break;
            }
        }

        this.autoLoop.setSelected(settings.isAutoLoop());
        this.loopInterval.setEnabled(settings.isAutoLoop());
        this.loopInterval.setText(String.valueOf(settings.getLoopInterval()));
    }

    private AdvancedSubmitSetting settings;

    public AdvancedSubmitSetting getSettings() {
        return settings;
    }

    private void switchRadioBtn() {
        if (allBtn.isSelected()) {
            singleTxt.setEnabled(false);
            startRowNo.setEnabled(false);
            endRowNo.setEnabled(false);
            multiRowsTxt.setEnabled(false);
            limitCountTxt.setEnabled(false);
        } else if (limitCountBtn.isSelected()) {
            singleTxt.setEnabled(false);
            startRowNo.setEnabled(false);
            endRowNo.setEnabled(false);
            multiRowsTxt.setEnabled(false);
            limitCountTxt.setEnabled(true);
        } else if (singleBtn.isSelected()) {
            singleTxt.setEnabled(true);
            startRowNo.setEnabled(false);
            endRowNo.setEnabled(false);
            multiRowsTxt.setEnabled(false);
            limitCountTxt.setEnabled(false);
        } else if (scopeBtn.isSelected()) {
            startRowNo.setEnabled(true);
            endRowNo.setEnabled(true);
            singleTxt.setEnabled(false);
            multiRowsTxt.setEnabled(false);
            limitCountTxt.setEnabled(false);
        } else if (multiBtn.isSelected()) {
            multiRowsTxt.setEnabled(true);
            singleTxt.setEnabled(false);
            startRowNo.setEnabled(false);
            endRowNo.setEnabled(false);
            limitCountTxt.setEnabled(false);
        }
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });

        setTitle(UIText.label("title.range.customize"));
        setResizable(false);

        this.initScopePane();

        okBtn = new JButton();
        cancelBtn = new JButton();

        JPanel typePanel = new JPanel();
        typeGroup = new ButtonGroup();
        typeAll = new JRadioButton();
        typePrime = new JRadioButton();
        typePt = new JRadioButton();
        typeIntl = new JRadioButton();
        typeBw = new JRadioButton();
        typeMhzy = new JRadioButton();
        typeHalf = new JRadioButton();

        JPanel loopPanel = new JPanel();
        autoLoop = new JCheckBox();
        loopInterval = new JTextField();
        loopInterval.setEnabled(false);
        JLabel lblLoopInterval = new JLabel();
        JLabel lblTimeUnit = new JLabel();

        okBtn.setText(UIText.label("label.ok"));
        okBtn.addActionListener(evt -> ok());

        cancelBtn.setText(UIText.label("label.cancel"));
        cancelBtn.addActionListener(evt -> cancelBtnActionPerformed(evt));

        markStatusButton = new JButton();
        markStatusButton.setText("Mark Status");
        markStatusButton.setIcon(UITools.getIcon("status.png"));
        submitButton = new JButton();
        submitButton.setText("Submit");
        submitButton.setIcon(UITools.getIcon("start.png"));


        markStatusButton.addActionListener(evt -> {
            continueToMarkStatus = true;
            ok();
        });

        submitButton.addActionListener(evt -> {
            continueToSubmit = true;
            ok();
        });
        typePanel.setBorder(BorderFactory.createTitledBorder(UIText.title("title.filter.ordertype")));

        typeGroup.add(typeAll);
        typeAll.setSelected(true);
        typeAll.setText(UIText.label("label.filter.ordertype.all"));

        typeGroup.add(typePrime);
        typePrime.setText(UIText.label("label.filter.ordertype.prime"));

        typeGroup.add(typePt);
        typePt.setText(UIText.label("label.filter.ordertype.pt"));

        typeGroup.add(typeIntl);
        typeIntl.setText(UIText.label("label.filter.ordertype.intl"));

        typeGroup.add(typeBw);
        typeBw.setText(UIText.label("label.filter.ordertype.bw"));

        typeGroup.add(typeMhzy);
        typeMhzy.setText(UIText.label("label.filter.ordertype.buytransfer"));

        typeGroup.add(typeHalf);
        typeHalf.setText(UIText.label("label.filter.ordertype.half"));

        GroupLayout typePanelLayout = new GroupLayout(typePanel);
        typePanel.setLayout(typePanelLayout);
        typePanelLayout.setHorizontalGroup(
                typePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(typePanelLayout.createSequentialGroup()
                                .addGroup(typePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(typePanelLayout.createSequentialGroup()
                                                .addComponent(typeHalf)
                                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(typePanelLayout.createSequentialGroup()
                                                .addGroup(typePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(typeAll)
                                                        .addComponent(typePt)
                                                        .addComponent(typeBw))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 80, 80)
                                                .addGroup(typePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(typeIntl)
                                                        .addComponent(typePrime)
                                                        .addComponent(typeMhzy)))))
        );
        typePanelLayout.setVerticalGroup(
                typePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(typePanelLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(typePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typePrime)
                                        .addComponent(typeAll))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(typePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typePt)
                                        .addComponent(typeIntl))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(typePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typeMhzy)
                                        .addComponent(typeBw))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(typeHalf))
        );

        loopPanel.setBorder(BorderFactory.createTitledBorder(UIText.label("title.settings.scheduling")));

        autoLoop.setText(UIText.label("label.submit.loop.enable"));
        autoLoop.setToolTipText(UIText.tooltip("tooltip.submit.loop.enable"));
        autoLoop.addActionListener(evt -> autoLoopActionPerformed(evt));

        loopInterval.setToolTipText(UIText.tooltip("tooltip.submit.loop.interval"));
        loopInterval.addFocusListener(selectAll);
        lblLoopInterval.setText(UIText.label("label.submit.loop.interval"));
        lblTimeUnit.setText(UIText.label("label.timeunit.min"));

        GroupLayout loopLayout = new GroupLayout(loopPanel);
        loopPanel.setLayout(loopLayout);
        loopLayout.setHorizontalGroup(
                loopLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(loopLayout.createSequentialGroup()
                                .addGroup(loopLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(autoLoop)
                                        .addGroup(loopLayout.createSequentialGroup()
                                                .addComponent(lblLoopInterval)
                                                .addGap(18, 18, 18)
                                                .addComponent(loopInterval, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblTimeUnit)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        loopLayout.setVerticalGroup(
                loopLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(loopLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(autoLoop)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(loopLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(loopInterval, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblLoopInterval)
                                        .addComponent(lblTimeUnit)))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addGap(10)
                                .addComponent(markStatusButton)
                                .addGap(10)
                                .addComponent(submitButton)
                                .addGap(10)
                                .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)


                                .addContainerGap())
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(scopePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(typePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(loopPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scopePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loopPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cancelBtn)
                                        .addComponent(markStatusButton)
                                        .addComponent(submitButton)
                                        .addComponent(okBtn)


                                )
                                .addContainerGap())
        );
        getRootPane().setDefaultButton(okBtn);
        pack();
    }

    public void initScopePane() {
        scopePanel = new JPanel();
        scopeBtnGroup = new ButtonGroup();
        allBtn = new JRadioButton();
        allBtn.addActionListener(e -> switchRadioBtn());

        limitCountBtn = new JRadioButton();
        limitCountTxt = new JTextField();
        limitCountBtn.addActionListener(e -> switchRadioBtn());
        limitCountTxt.setEnabled(false);
        limitCountTxt.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!limitCountTxt.isEnabled()) {
                    limitCountTxt.setEnabled(true);
                    limitCountBtn.setSelected(true);
                    switchRadioBtn();
                }
            }
        });

        singleBtn = new JRadioButton();
        singleBtn.addActionListener(e -> switchRadioBtn());
        singleTxt = new JTextField();
        singleTxt.setEnabled(false);
        singleTxt.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!singleTxt.isEnabled()) {
                    singleTxt.setEnabled(true);
                    singleBtn.setSelected(true);
                    switchRadioBtn();
                }
            }
        });


        scopeBtn = new JRadioButton();
        scopeBtn.addActionListener(e -> switchRadioBtn());
        startRowNo = new JTextField();
        endRowNo = new JTextField();
        startRowNo.setEnabled(false);
        endRowNo.setEnabled(false);

        startRowNo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!startRowNo.isEnabled()) {
                    startRowNo.setEnabled(true);
                    endRowNo.setEnabled(true);
                    scopeBtn.setSelected(true);
                    switchRadioBtn();
                }
            }
        });
        endRowNo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!endRowNo.isEnabled()) {
                    startRowNo.setEnabled(true);
                    endRowNo.setEnabled(true);
                    scopeBtn.setSelected(true);
                    switchRadioBtn();
                }
            }
        });


        multiBtn = new JRadioButton();
        multiBtn.addActionListener(e -> switchRadioBtn());
        multiRowsTxt = new JTextField();
        multiRowsTxt.setEnabled(false);
        multiRowsTxt.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!multiRowsTxt.isEnabled()) {
                    multiRowsTxt.setEnabled(true);
                    multiBtn.setSelected(true);
                    switchRadioBtn();
                }
            }
        });

        scopePanel.setBorder(BorderFactory.createTitledBorder(UIText.label("title.range.define")));

        scopeBtnGroup.add(allBtn);
        allBtn.setText(UIText.label("label.range.all"));

        scopeBtnGroup.add(limitCountBtn);
        limitCountBtn.setText(UIText.label("label.select.range.limitcount"));
        limitCountTxt.setToolTipText(UIText.label("tooltip.select.range.limitcount"));

        scopeBtnGroup.add(singleBtn);
        singleBtn.setText(UIText.label("label.select.range.singlerow"));

        scopeBtnGroup.add(scopeBtn);
        scopeBtn.setText(UIText.label("label.select.range.scope"));
        scopeBtn.setSelected(true);

        scopeBtnGroup.add(multiBtn);
        multiBtn.setText(UIText.label("label.select.range.multiple"));
        multiRowsTxt.setToolTipText(UIText.label("tooltip.select.range.multiple"));

        int fieldWidth = 120;
        GroupLayout layout = new GroupLayout(scopePanel);
        scopePanel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(allBtn).addComponent(multiBtn).addComponent(singleBtn).addComponent(scopeBtn).addComponent(limitCountBtn))
                                .addGap(20)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(singleTxt, fieldWidth, fieldWidth, fieldWidth))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(limitCountTxt, fieldWidth, fieldWidth, fieldWidth))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(multiRowsTxt, fieldWidth, fieldWidth, fieldWidth))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(startRowNo, fieldWidth, fieldWidth, fieldWidth)
                                                .addComponent(endRowNo, fieldWidth, fieldWidth, fieldWidth))
                                )));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(allBtn)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(limitCountBtn)
                                        .addComponent(limitCountTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(singleBtn)
                                        .addComponent(singleTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(scopeBtn)
                                        .addComponent(startRowNo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(endRowNo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(multiBtn)
                                        .addComponent(multiRowsTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
    }

    private void autoLoopActionPerformed(ActionEvent evt) {
        this.loopInterval.setEnabled(this.autoLoop.isSelected());
    }

    /**
     * 输入的数字必须是数字且限定在4位以内
     */
    private boolean validateNo(String txt) {
        boolean match = StringUtils.isNotBlank(txt) && txt.matches("[0-9]{1,4}");
        return match && Integer.parseInt(txt) > 0;
    }

    public void ok() {
        AdvancedSubmitSetting ass = new AdvancedSubmitSetting();

        List<String> validateErrors = new ArrayList<>();
        if (this.allBtn.isSelected()) {
            ass.setSubmitRange(ConfigEnums.SubmitRange.ALL);
        } else if (this.limitCountBtn.isSelected()) {
            String text = this.limitCountTxt.getText().trim();
            if (!validateNo(text)) {
                validateErrors.add(UIText.message("message.error.rowno", UIText.label("label.count")));
            } else {
                ass.setSubmitRange(ConfigEnums.SubmitRange.LimitCount);
                ass.setCountLimit(Integer.parseInt(text));
            }
        } else if (this.singleBtn.isSelected()) {
            String text = this.singleTxt.getText().trim();
            if (!validateNo(text)) {
                validateErrors.add(UIText.message("message.error.rowno", UIText.label("label.rowno.single")));
            } else {
                ass.setSubmitRange(ConfigEnums.SubmitRange.SINGLE);
                ass.setSingleRowNo(Integer.parseInt(text));
            }
        } else if (this.scopeBtn.isSelected()) {
            String text = this.startRowNo.getText().trim(), text2 = this.endRowNo.getText().trim();
            boolean valid1 = validateNo(text), valid2 = validateNo(text2);
            if (!valid1) {
                validateErrors.add(UIText.message("message.error.rowno", UIText.label("label.rowno.start")));
            }
            if (!valid2) {
                validateErrors.add(UIText.message("message.error.rowno", UIText.label("label.rowno.end")));
            }

            if (valid1 && valid2) {
                ass.setSubmitRange(ConfigEnums.SubmitRange.SCOPE);
                ass.setStartRowNo(Integer.parseInt(text));
                ass.setEndRowNo(Integer.parseInt(text2));
            }
        } else if (this.multiBtn.isSelected()) {
            String text = this.multiRowsTxt.getText().trim();
            if (StringUtils.isBlank(text) || !text.matches("[0-9|,]+")) {
                validateErrors.add(UIText.message("message.error.rowno.multi"));
            } else {
                ass.setSubmitRange(ConfigEnums.SubmitRange.MULTIPLE);
                ass.setMultiRows(this.multiRowsTxt.getText().trim());
            }
        }

        if (this.typeAll.isSelected()) {
            ass.setStatusFilterValue(null);
        } else if (this.typeBw.isSelected()) {
            ass.setStatusFilterValue(OrderEnums.Status.SellerIsBetterWorld);
        } else if (this.typeHalf.isSelected()) {
            ass.setStatusFilterValue(OrderEnums.Status.SellerIsHalf);
        } else if (this.typeIntl.isSelected()) {
            ass.setStatusFilterValue(OrderEnums.Status.International);
        } else if (this.typeMhzy.isSelected()) {
            ass.setStatusFilterValue(OrderEnums.Status.BuyAndTransfer);
        } else if (this.typePrime.isSelected()) {
            ass.setStatusFilterValue(OrderEnums.Status.PrimeSeller);
        } else if (this.typePt.isSelected()) {
            ass.setStatusFilterValue(OrderEnums.Status.CommonSeller);
        }

        if (this.autoLoop.isSelected()) {
            ass.setAutoLoop(true);
            String text = this.loopInterval.getText().trim();
            if (!validateNo(text)) {
                validateErrors.add(UIText.message("message.error.loopinterval"));
            } else {
                ass.setLoopInterval(Integer.parseInt(text));
            }
        } else {
            ass.setAutoLoop(false);
            ass.setLoopInterval(0);
        }

        if (CollectionUtils.isNotEmpty(validateErrors)) {
            UITools.error(StringUtils.join(validateErrors, StringUtils.LF), UIText.title("title.conf_error"));
        } else {
            this.settings = ass;
            this.setVisible(false);
        }


        RuntimeSettings runtimeSettings = RuntimeSettings.load();
        runtimeSettings.setAdvancedSubmitSetting(settings);
        runtimeSettings.save();


    }

    private void cancelBtnActionPerformed(ActionEvent evt) {
        this.dispose();
        this.setVisible(false);
    }

    private JButton cancelBtn;
    private JButton okBtn;
    private JCheckBox autoLoop;
    private JTextField loopInterval;
    private JTextField singleTxt;
    private JTextField startRowNo;
    private JTextField endRowNo;
    private JTextField multiRowsTxt;
    private ButtonGroup scopeBtnGroup;
    private JRadioButton allBtn;
    private JRadioButton limitCountBtn;
    private JTextField limitCountTxt;
    private JRadioButton singleBtn;
    private JRadioButton scopeBtn;
    private JRadioButton multiBtn;
    private ButtonGroup typeGroup;
    private JRadioButton typeAll;
    private JRadioButton typeBw;
    private JRadioButton typeHalf;
    private JRadioButton typeIntl;
    private JRadioButton typeMhzy;
    private JRadioButton typePrime;
    private JRadioButton typePt;
    private JPanel scopePanel;
    private JButton markStatusButton;
    private JButton submitButton;

    public static void main(String[] args) {

        UIText.setLocale(Language.current());
        UITools.setTheme();

        File file = new File(Harvester.RUNTIME_SETTINGS_FILE_PATH);
        RuntimeSettings settings = RuntimeSettings.load();
        AdvancedSubmitSetting advancedSubmitSetting = settings.getAdvancedSubmitSetting();
        UITools.setDialogAttr(new SelectRangeDialog(null, true, advancedSubmitSetting), true);
    }
}
