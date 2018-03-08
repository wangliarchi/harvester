package edu.olivet.harvester.finance.model;

import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.harvester.utils.common.DateFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/1/2018 3:03 PM
 */
@SuppressWarnings("DefaultAnnotationParam")
@Table(value = "invoice_downloading_tasks")
@Data
@EqualsAndHashCode(callSuper = false)

public class InvoiceTask extends PrimaryKey implements ArrayConvertable {

    @Name
    @Column
    private String id;
    @Column
    private String buyerAccount;
    @Column
    private String country;

    @Column
    private int startPage;
    @Column
    private int lastDownloadPage;
    @Column
    private Date fromDate;
    @Column
    private Date toDate;
    @Column
    private Date lastDownloadDate;
    @Column
    private String status;

    @Column
    private Date dateCreated;

    @Override
    public String getPK() {
        return id;
    }

    public static final String[] COLUMNS = {"Date Created", "Buyer", "Country", "From", "To", "LastDate", "Status"};

    public static final int[] WIDTHS = {80, 200, 70, 70, 70, 70, 60};

    @Override
    public Object[] toArray() {
        return new Object[] {DateFormat.DATE_TIME_SHORT.format(this.dateCreated),
                buyerAccount,
                country,
                DateFormat.FULL_DATE.format(fromDate),
                DateFormat.FULL_DATE.format(toDate),
                DateFormat.FULL_DATE.format(lastDownloadDate),
                status};
    }

}
