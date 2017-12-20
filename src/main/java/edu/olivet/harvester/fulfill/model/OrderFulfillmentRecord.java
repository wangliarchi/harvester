package edu.olivet.harvester.fulfill.model;

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

@Table(value = "order_fulfillment_logs")
@Data
@EqualsAndHashCode(callSuper = false)
public class OrderFulfillmentRecord extends PrimaryKey implements ArrayConvertable {
    @Name
    private String id;
    @Column
    private String orderId;
    @Column
    private String sku;
    @Column
    private String sheetName;
    @Column
    private String spreadsheetId;
    @Column
    private String purchaseDate;
    @Column
    private String isbn;
    @Column
    private String seller;
    @Column
    private String sellerId;
    @Column
    private String sellerPrice;
    @Column
    private String condition;
    @Column
    private String character;
    @Column
    private String cost;
    @Column
    private String orderNumber;
    @Column
    private String buyerAccount;
    @Column
    private String lastCode;
    @Column
    private String remark;
    @Column
    private int quantityPurchased;
    @Column
    private int quantityBought;
    @Column
    private Date fulfillDate;
    @Column
    private String shippingAddress;
    @Column
    private String fulfilledAddress;
    @Column
    private String fulfilledASIN;


    @Override
    public String getPK() {
        return this.id;
    }

    @Override
    public Object[] toArray() {
        return new Object[] {DateFormat.DATE_TIME.format(this.fulfillDate), orderId, sku, quantityPurchased, quantityBought, orderNumber, cost, buyerAccount, lastCode, purchaseDate, isbn, seller, character, condition, remark, spreadsheetId, sheetName, shippingAddress, fulfilledAddress, fulfilledASIN};
    }


    public static final String[] COLUMNS = {"Fulfill Date", "OrderId", "sku", "quantityPurchased", "quantityBought", "orderNumber", "cost", "buyerAccount",
            "lastCode", "purchaseDate", "isbn", "seller", "character", "condition", "remark", "spreadsheetId", "sheetName", "shippingAddress", "fulfilledAddress", "fulfilledASIN"};

    public static final int[] WIDTHS = {100, 100, 50, 20, 20, 70, 50, 100, 30, 100, 60, 60, 10, 50, 100, 100, 50, 250, 250, 50};


}
