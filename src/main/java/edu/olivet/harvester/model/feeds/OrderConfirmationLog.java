package edu.olivet.harvester.model.feeds;


import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.harvester.utils.DateFormat;
import edu.olivet.harvester.utils.DatetimeHelper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;

@Table(value = "order_confirmation_record")
@Data
@EqualsAndHashCode(callSuper = false)

public class OrderConfirmationLog extends PrimaryKey implements ArrayConvertable {
    @Name
    private String id;
    @Column
    private String context;
    @Column private Date uploadTime;
    @Column private String result;

    @Override
    public String getPK() {
        return this.id;
    }

    @Override
    public Object[] toArray() {
        return new Object[] {String.format("%s.txt", this.id), DateFormat.DATE_TIME.format(this.uploadTime), this.result};
    }

    public static final String[] COLUMNS = {"Feed File", "Upload Time", "Batch Execution Result"};

    public static final int[] WIDTHS = {150, 150, 500};
}


