package edu.olivet.harvester.finance.model;

import edu.olivet.foundations.db.PrimaryKey;
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
@Table(value = "buyer_invoices")
@Data
@EqualsAndHashCode(callSuper = false)

public class BuyerOrderInvoice extends PrimaryKey {

    @Name
    @Column
    private String orderId;

    @Column
    private String buyerEmail;
    @Column
    private String country;
    @Column
    private String cardNo;
    @Column
    private float orderTotal;
    @Column
    private Date purchaseDate;
    @Column
    private Date dateDownloaded;

    @Override
    public String getPK() {
        return orderId;
    }
}
