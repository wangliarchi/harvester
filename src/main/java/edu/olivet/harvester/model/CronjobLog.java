package edu.olivet.harvester.model;

import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.harvester.utils.DateFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/13/17 9:25 PM
 */

@Table(value = "cronjob_record")
@Data
@EqualsAndHashCode()

public class CronjobLog extends PrimaryKey implements ArrayConvertable {
    @Name
    private String id;
    @Column
    private String jobName;
    @Column private Date runTime;

    @Override
    public String getPK() {
        return this.id;
    }

    @Override
    public Object[] toArray() {
        return new Object[] {String.format("%s.txt", this.id), DateFormat.DATE_TIME.format(this.runTime), this.jobName};
    }

    public static final String[] COLUMNS = {"Cronjob Name", "Run Time"};

    public static final int[] WIDTHS = {250, 150};
}