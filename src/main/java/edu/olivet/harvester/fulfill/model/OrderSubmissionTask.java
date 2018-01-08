package edu.olivet.harvester.fulfill.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.model.setting.AdvancedSubmitSetting;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator.SkipValidation;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.model.OrderRange;
import edu.olivet.harvester.spreadsheet.utils.SheetUtils;
import edu.olivet.harvester.utils.common.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/11/17 1:45 PM
 */
@Data
@Table(value = "order_submission_tasks_new")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderSubmissionTask extends PrimaryKey implements ArrayConvertable {
    @Name
    private String id;

    @Column
    private String sid;
    @Column
    private String marketplaceName;
    @Column
    private String spreadsheetId;
    @Column
    private String spreadsheetName;

    private OrderRange orderRange;
    @Column
    private String orderRangeCol;
    @Column
    private String lostLimit = "5";
    @Column
    private String priceLimit = "3";
    @Column
    private String eddLimit = "7";
    @Column
    private String noInvoiceText = "{No Invoice}";
    @Column
    private String finderCode = "";

    private SkipValidation skipValidation = SkipValidation.None;
    @Column
    String skipValidationCol;


    @Column
    String orders;
    @Column
    String invalidOrders;
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

    @Column
    String buyerAccount;
    @Column
    String primeBuyerAccount;

    @Override
    public String getPK() {
        return id;
    }


    public OrderRange getOrderRange() {
        if (orderRange == null && StringUtils.isNotBlank(orderRangeCol)) {
            orderRange = JSON.parseObject(orderRangeCol, OrderRange.class);
        }
        return orderRange;
    }

    public SkipValidation getSkipValidation() {
        if (skipValidation == null && StringUtils.isNotBlank(skipValidationCol)) {
            skipValidation = SkipValidation.valueOf(skipValidationCol);
        }
        return skipValidation;
    }

    public List<Order> getOrderList() {
        if (StringUtils.isBlank(orders)) {
            return null;
        }
        return JSON.parseArray(orders, Order.class);
    }

    public List<String> getInvalidOrders() {
        if (StringUtils.isBlank(invalidOrders)) {
            return null;
        }
        return Lists.newArrayList(StringUtils.split(invalidOrders, "\n"));
    }

    public RuntimeSettings convertToRuntimeSettings() {
        RuntimeSettings runtimeSettings = new RuntimeSettings();
        //private
        orderRange = getOrderRange();
        skipValidation = getSkipValidation();
        runtimeSettings.setSid(sid);
        runtimeSettings.setMarketplaceName(marketplaceName);
        runtimeSettings.setSpreadsheetName(spreadsheetName);
        runtimeSettings.setSpreadsheetId(spreadsheetId);
        runtimeSettings.setSheetName(orderRange.getSheetName());
        runtimeSettings.setLostLimit(lostLimit);
        runtimeSettings.setPriceLimit(priceLimit);
        runtimeSettings.setEddLimit(eddLimit);
        runtimeSettings.setNoInvoiceText(noInvoiceText);
        runtimeSettings.setFinderCode(finderCode);
        runtimeSettings.setSkipValidation(skipValidation);

        AdvancedSubmitSetting advancedSubmitSetting = new AdvancedSubmitSetting();
        advancedSubmitSetting.setAutoLoop(false);
        advancedSubmitSetting.setCountLimit(0);
        if (orderRange.getBeginRow() == null && orderRange.getEndRow() == null) {
            advancedSubmitSetting.setSubmitRange(ConfigEnums.SubmitRange.ALL);
            advancedSubmitSetting.setStartRowNo(0);
            advancedSubmitSetting.setEndRowNo(0);
            advancedSubmitSetting.setSingleRowNo(0);
        } else if (orderRange.getBeginRow() != null && orderRange.getEndRow() == null) {
            advancedSubmitSetting.setSubmitRange(ConfigEnums.SubmitRange.SINGLE);
            advancedSubmitSetting.setStartRowNo(0);
            advancedSubmitSetting.setEndRowNo(0);
            advancedSubmitSetting.setSingleRowNo(orderRange.getBeginRow());
        } else if (orderRange.getBeginRow() == null && orderRange.getEndRow() != null) {
            advancedSubmitSetting.setSubmitRange(ConfigEnums.SubmitRange.SCOPE);
            advancedSubmitSetting.setStartRowNo(3);
            advancedSubmitSetting.setEndRowNo(orderRange.getEndRow());
            advancedSubmitSetting.setSingleRowNo(0);
        } else {
            advancedSubmitSetting.setSubmitRange(ConfigEnums.SubmitRange.SCOPE);
            advancedSubmitSetting.setStartRowNo(orderRange.getBeginRow());
            advancedSubmitSetting.setEndRowNo(orderRange.getEndRow());
            advancedSubmitSetting.setSingleRowNo(0);
        }

        runtimeSettings.setAdvancedSubmitSetting(advancedSubmitSetting);
        return runtimeSettings;
    }


    public static final String[] COLUMNS = {"Date Created", "MKTPL", "Type", "Range", "T", "S", "F", "Status", ""};

    public static final int[] WIDTHS = {70, 35, 35, 70, 20, 20, 20, 60, 65};

    @Override
    public Object[] toArray() {
        return new Object[]{DateFormat.DATE_TIME_SHORT.format(this.dateCreated),
                marketplaceName,
                SheetUtils.getTypeFromSpreadsheetName(spreadsheetName),
                getOrderRange().getSheetName() + " " + convertToRuntimeSettings().getAdvancedSubmitSetting(),
                totalOrders, success, failed,
                status, "delete"};
    }


    public String getSummary() {
        if (totalOrders == 0) {
            return "No valid orders found";
        }


        return "";
    }

    public OrderSubmissionTask copy() {
        OrderSubmissionTask task = new OrderSubmissionTask();
        task.sid = sid;
        task.marketplaceName = marketplaceName;
        task.spreadsheetId = spreadsheetId;
        task.spreadsheetName = spreadsheetName;
        task.orderRangeCol = orderRangeCol;
        task.lostLimit = lostLimit;
        task.priceLimit = priceLimit;
        task.eddLimit = eddLimit;
        task.noInvoiceText = noInvoiceText;
        task.skipValidationCol = skipValidationCol;
        task.finderCode = finderCode;
        task.totalOrders = totalOrders;
        task.buyerAccount = buyerAccount;
        task.primeBuyerAccount = primeBuyerAccount;
        task.dateCreated = new Date();

        return task;

    }


    public static void main(String[] args) {
        DBManager dbManager = ApplicationContext.getBean(DBManager.class);
        List<OrderSubmissionTask> list = dbManager.query(OrderSubmissionTask.class,
                Cnd.where("status", "=", "Scheduled")
                        .desc("dateCreated"));
        RuntimeSettings runtimeSettings = list.get(0).convertToRuntimeSettings();
        System.out.println(runtimeSettings);
    }

}
