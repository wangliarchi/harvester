package edu.olivet.harvester.selforder.model;

/*
  @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/11/17 8:46 AM
 */

import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.harvester.utils.common.DateFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;

@SuppressWarnings("DefaultAnnotationParam")
@Table(value = "self_orders")
@Data
@EqualsAndHashCode(callSuper = false)
public class OrderFulfillmentRecord extends PrimaryKey implements ArrayConvertable {
    @Name
    private String id;
    @Column
    private String sheetName;
    @Column
    private String seller;
    @Column
    private String sellerId;
    @Column
    private String sellerCode;
    @Column
    private String asin;
    @Column
    private String country;
    @Column
    private String cost;

    @Column
    private String primoCode;
    @Column
    private String orderNumber;
    @Column
    private String buyerAccount;

    @Column
    private Date fulfillDate;

    @Column
    private String fulfilledAddress = "";


    @Override
    public String getPK() {
        return this.id;
    }

    @Override
    public Object[] toArray() {
        return new Object[] {DateFormat.DATE_TIME.format(this.fulfillDate),
                sheetName, sellerCode, seller, buyerAccount, orderNumber, cost, fulfilledAddress};
    }


    public static final String[] COLUMNS = {"Fulfill Date", "sheetName", "SellerCode", "seller",
            "buyerAccount", "orderNumber", "cost", "fulfilledAddress"};

    public static final int[] WIDTHS = {100, 50, 50, 120, 120, 70, 50, 250};

}
