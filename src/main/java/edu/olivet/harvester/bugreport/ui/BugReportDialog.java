package edu.olivet.harvester.bugreport.ui;

import com.alibaba.fastjson.JSON;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.BaseDialog;
import edu.olivet.foundations.ui.HtmlEditorPanel;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.bugreport.model.Bug;
import edu.olivet.harvester.bugreport.model.IssueCategory;
import edu.olivet.harvester.bugreport.model.Priority;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/28/17 9:53 AM
 */
public class BugReportDialog extends BaseDialog {
    private static final long serialVersionUID = 214346846183121036L;

    public BugReportDialog(Bug bug) {
        super(null, true);
        this.setResizable(false);
        initComponents();
        this.load(bug);
    }

    @Getter
    @Setter
    private Bug bug;

    @Override
    public void ok() {
        String html = null;
        try {
            html = htmlEditorPanel.getHtmlText();
            html = Jsoup.parse(html).body().html();
        } catch (Exception e) {
            //ignore
        }

        if (StringUtils.isBlank(subject.getText()) && StringUtils.isBlank(html)) {
            UITools.error(UIText.message("error.bug.detail"));
            return;
        }
        Bug bug = new Bug();
        // 只获取innerHtml，以便插入模板(BugReport.ftl)中
        bug.setDetail(html);
        bug.setIssueCategory((IssueCategory) function.getSelectedItem());
        bug.setTitle(StringUtils.defaultString(subject.getText()));
        bug.setPriority((Priority) priority.getSelectedItem());
        bug.setCountry((Country) marketplace.getSelectedItem());
        bug.setTeamviewerId(teamviewer.getText());
        this.setBug(bug);
        this.doClose();
    }

    private void initComponents() {
        final JPanel jPanel = new JPanel();
        final JLabel lblFunction = new JLabel(UIText.labelWithColon("label.bug.function"));
        function = new JComboBox<>(new DefaultComboBoxModel<>(IssueCategory.values()));

        final JLabel lblSubject = new JLabel(UIText.labelWithColon("label.bug.subject"));
        subject = new JTextField();

        final JLabel lblDetail = new JLabel(UIText.labelWithColon("label.bug.detail"));
        JScrollPane jScrollPane = new JScrollPane();
        htmlEditorPanel = new HtmlEditorPanel(652, 492);
        jScrollPane.setViewportView(htmlEditorPanel);

        this.initButtons();
        String title = UIText.title("title.bug.report");
        this.setTitle(title);
        jPanel.setBorder(UITools.createTitledBorder(title));

        final JLabel lblPriority = new JLabel(UIText.labelWithColon("label.bug.priority"));
        priority = new JComboBox<>(new DefaultComboBoxModel<>(Priority.values()));
        priority.setSelectedIndex(1);
        priority.setToolTipText(((Priority) priority.getSelectedItem()).tooltip());
        priority.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                priority.setToolTipText(((Priority) priority.getSelectedItem()).tooltip());
            }
        });


        final JLabel marketplaceLabel = new JLabel("Marketplace");
        marketplace = new JComboBox<>();
        List<Country> countries = Settings.load().listAllCountries();
        marketplace.setModel(new DefaultComboBoxModel<>(countries.toArray(new Country[countries.size()])));

        final JLabel teamviewerLabel = new JLabel("Teamviewer ID");
        teamviewer = new JTextField();


        GroupLayout panelLayout = new GroupLayout(jPanel);
        jPanel.setLayout(panelLayout);


        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplaceLabel)
                                                        .addComponent(lblFunction)
                                                        .addComponent(lblPriority)
                                                        .addComponent(teamviewerLabel)
                                                        .addComponent(lblSubject)
                                                        .addComponent(lblDetail)
                                                )
                                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(marketplace)
                                                        .addComponent(function)
                                                        .addComponent(priority)
                                                        .addComponent(teamviewer)
                                                        .addComponent(subject)
                                                        .addComponent(jScrollPane, 660, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                                )
                                        )
                                )
                                .addContainerGap()
                        )
        );


        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(marketplaceLabel)
                                        .addComponent(marketplace, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblFunction)
                                        .addComponent(function, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblPriority)
                                        .addComponent(priority, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(teamviewerLabel)
                                        .addComponent(teamviewer, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblSubject)
                                        .addComponent(subject, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblDetail)
                                        .addComponent(jScrollPane, 400, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                        )
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(okBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addGap(10)
                                .addComponent(cancelBtn, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH, UITools.BUTTON_WIDTH)
                                .addContainerGap())
                        .addComponent(jPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(okBtn).addComponent(cancelBtn)))
        );
        // 此处不要设置按下回车键之后点击确定按钮，避免对文本编辑产生影响
        pack();
    }

    private HtmlEditorPanel htmlEditorPanel;
    private JComboBox<Country> marketplace;
    private JComboBox<IssueCategory> function;
    private JComboBox<Priority> priority;
    private JTextField subject;
    private JTextField teamviewer;

    public void load(Bug bug) {
        if (bug == null) {
            return;
        }
        if (bug.getCountry() != null) {
            this.marketplace.setSelectedItem(bug.getCountry());
        }
        if (bug.getIssueCategory() != null) {
            this.function.setSelectedItem(bug.getIssueCategory());
        }
        if (bug.getPriority() != null) {
            this.priority.setSelectedItem(bug.getPriority());
        }
        if (StringUtils.isNotBlank(bug.getTitle())) {
            this.subject.setText(bug.getTitle());
        }
        this.htmlEditorPanel.setHtmlText(bug.getDetail());
        if (StringUtils.isNotBlank(bug.getTeamviewerId())) {
            teamviewer.setText(bug.getTeamviewerId());
        }
    }

    public static void main(String[] args) {
        UIText.setLocale(Language.current());
        UITools.setTheme();
        Bug bug = JSON.parseObject(Tools.readFileToString(new File("src/test/resources/data4test/bug-sample.json")), Bug.class);
        BugReportDialog dialog = UITools.setDialogAttr(new BugReportDialog(bug), false);
        System.out.println(dialog.getBug().getDetail());
    }
}

