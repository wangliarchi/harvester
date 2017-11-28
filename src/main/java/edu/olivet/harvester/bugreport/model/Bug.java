package edu.olivet.harvester.bugreport.model;

import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.foundations.ui.ListModel;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Tools;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Collections;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/28/17 9:58 AM
 */
@Table(value = "bug")
public class Bug extends PrimaryKey implements ArrayConvertable {
    @Getter
    @Setter
    @Name
    private String id;
    @Getter
    @Setter
    @Column
    private String issue;
    @Getter
    @Setter
    @Column
    private String level;
    @Getter
    @Setter
    @Column
    private String title;
    @Getter
    @Setter
    @Column
    private String orderId = "";
    @Getter
    @Setter
    @Column
    private Date reportTime;

    @Getter
    private IssueCategory issueCategory;
    @Getter
    private Priority priority;
    @Getter
    @Setter
    private String detail;
    @Getter
    @Setter
    private String context;
    @Getter
    @Setter
    private String version;
    @Getter
    @Setter
    private String emailContent;
    @Getter
    @Setter
    private String progressMessage;
    @Getter
    @Setter
    private String reporterEmail;
    @Getter
    @Setter
    private String accountEmails;
    @Getter
    @Setter
    private String spreadSheets;

    @Setter
    @Getter
    private Country country;

    @Setter
    @Getter
    private String teamviewerId = "";

    public boolean valid() {
        return this.issueCategory != null && this.priority != null && StringUtils.isNotBlank(detail);
    }

    public void setIssueCategory(IssueCategory issueCategory) {
        this.issueCategory = issueCategory;
        this.issue = issueCategory.name();
    }

    public void setPriority(Priority priority) {
        this.level = priority.name();
        this.priority = priority;
    }

    @Override
    public String getPK() {
        return id;
    }

    @Override
    public Object[] toArray() {
        return new Object[]{this.id, this.orderId, this.issueCategory.toString(), this.priority.toString(),
                this.title, Dates.toDateTime(this.reportTime)};
    }

    public static final int[] WIDTHS = {100, 150, 60, 60, 240, 120};

    public static final String[] COLUMNS = {
            UIText.label("label.id"),
            UIText.label("label.orderid2"),
            UIText.label("label.bug.function"),
            UIText.label("label.bug.priority"),
            UIText.label("label.bug.subject"),
            UIText.label("label.bug.time"),
    };

    public static void main(String[] args) {
        Bug bug = new Bug();
        bug.setId("1");
        bug.setIssueCategory(IssueCategory.OrderConfirmation);
        bug.setOrderId(Tools.generateDummyOrderNumber());
        bug.setPriority(Priority.High);
        bug.setTitle("Error path selection");
        bug.setReportTime(new Date());
        ApplicationContext.getBean(DBManager.class).insert(bug, Bug.class);

        UIText.setLocale(Language.current());
        UITools.setTheme();
        ListModel<Bug> listModel = new ListModel<>(UIText.title("title.bug.history"), Collections.singletonList(bug),
                Bug.COLUMNS, null, Bug.WIDTHS);
        UITools.displayListDialog(listModel);
        System.exit(0);
    }
}