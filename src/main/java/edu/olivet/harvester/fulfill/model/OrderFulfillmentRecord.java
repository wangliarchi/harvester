package edu.olivet.harvester.fulfill.model;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/11/17 8:46 AM
 */

import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.harvester.utils.DateFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;

@Table(value = "order_fulfillment_record")
@Data
@EqualsAndHashCode()
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

    @Override
    public String getPK() {
        return this.id;
    }

    @Override
    public Object[] toArray() {
        return new Object[]{DateFormat.DATE_TIME.format(this.fulfillDate), orderId, sku, quantityPurchased, quantityBought, orderNumber, cost, buyerAccount, lastCode, purchaseDate, isbn, seller, character, condition, spreadsheetId, sheetName};
    }

    public static final String[] COLUMNS = {"Fulfill Date", "OrderId", "sku", "quantityPurchased", "quantityBought", "orderNumber", "cost", "buyerAccount",
            "lastCode", "purchaseDate", "isbn", "seller", "character", "condition", "spreadsheetId", "sheetName"};

    //public static final int[] WIDTHS = {250, 150};
}

/**
 * CREATE TABLE IF NOT EXISTS order_fulfillment_record (
 * id         VARCHAR(40) PRIMARY KEY NOT NULL,
 * orderId    VARCHAR(25)             NOT NULL DEFAULT '',
 * sku    VARCHAR(100)             NOT NULL DEFAULT '',
 * sheetName    VARCHAR(25)             NOT NULL DEFAULT '',
 * spreadsheetId VARCHAR(100)             NOT NULL DEFAULT '',
 * purchaseDate    VARCHAR(25)             NOT NULL DEFAULT '',
 * isbn   VARCHAR(10)             NOT NULL DEFAULT '',
 * seller   VARCHAR(100)             NOT NULL DEFAULT '',
 * sellerId   VARCHAR(30)             NOT NULL DEFAULT '',
 * sellerPrice   VARCHAR(10)             NOT NULL DEFAULT '',
 * condition   VARCHAR(20)             NOT NULL DEFAULT '',
 * character   VARCHAR(10)             NOT NULL DEFAULT '',
 * remark   VARCHAR(100)             NOT NULL DEFAULT '',
 * quantityPurchased INTEGER(3) NOT NULL DEFAULT 0,
 * quantityBought INTEGER(3) NOT NULL DEFAULT 0,
 * cost   VARCHAR(10)             NOT NULL DEFAULT '',
 * orderNumber    VARCHAR(25)             NOT NULL DEFAULT '',
 * buyerAccount    VARCHAR(100)             NOT NULL DEFAULT '',
 * lastCode    VARCHAR(10)             NOT NULL DEFAULT '',
 * fulfillDate    DATETIME                NOT NULL
 * );
 */