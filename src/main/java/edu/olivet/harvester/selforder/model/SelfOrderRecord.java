package edu.olivet.harvester.selforder.model;

import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/10/2018 12:03 PM
 */
@Data
@NoArgsConstructor
public class SelfOrderRecord {
    public String orderDate;
    public String storeName;
    public String sellerId;
    public String country;
    public String asin;
    public String promoCode;
    public String buyerAccountCode;
    public String buyerAccountEmail;
    public String orderNumber;
    public String cost;
    public String feedbackLeft;
    public String feedback;
    public String feedbackDate;
    public String uniqueCode;


    public String sheetName;
    public String spreadsheetId;
    public int row;

    public boolean feedbackPosted() {
        return "Yes".equalsIgnoreCase(feedbackLeft);
    }
}
