package edu.olivet.harvester.fulfill.model;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.spreadsheet.utils.SheetUtils;
import edu.olivet.harvester.utils.common.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/11/17 1:45 PM
 */
@Data
@Table(value = "order_submission_tasks_by_buyer_accounts")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderSubmissionBuyerAccountTask extends PrimaryKey implements ArrayConvertable {
    @Name
    private String id;

    @Column
    private String taskId;
    @Column
    private String marketplaceName;
    @Column
    private String spreadsheetId;
    @Column
    private String spreadsheetName;

    @Column
    private String sheetName;
    @Column
    String buyerAccount;
    @Column
    String fulfillmentCountry;


    @Column
    String orders;

    @Column
    String summary;
    @Column
    String status;
    @Column
    int totalOrders;
    @Column
    int success;
    @Column
    int failed;
    @Column
    String timeTaken;
    @Column
    Date dateCreated;
    @Column
    Date dateStarted;
    @Column
    Date dateEnded;


    @Override
    public String getPK() {
        return id;
    }


    public List<Order> getOrderList() {
        if (StringUtils.isBlank(orders)) {
            return null;
        }
        return JSON.parseArray(orders, Order.class);
    }


    public static final String[] COLUMNS = {"Date Created", "MKTPL", "Type", "Buyer", "Country", "T", "S", "F", "Status"};

    public static final int[] WIDTHS = {70, 35, 35, 70, 30, 20, 20, 20, 60};

    @Override
    public Object[] toArray() {
        return new Object[] {DateFormat.DATE_TIME_SHORT.format(this.dateCreated),
                marketplaceName,
                SheetUtils.getTypeFromSpreadsheetName(spreadsheetName),
                buyerAccount,
                fulfillmentCountry,
                totalOrders, success, failed,
                status};
    }


    public void setTaskStatus(OrderTaskStatus taskStatus) {
        status = taskStatus.name();
    }

    public OrderTaskStatus taskStatus() {
        return OrderTaskStatus.valueOf(status);
    }


    public String getSummary() {
        if (totalOrders == 0) {
            return "No valid orders found";
        }
        return "";
    }


}
