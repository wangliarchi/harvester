package edu.olivet.harvester.finance.model;

import edu.olivet.harvester.common.model.Money;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 4/12/2018 10:27 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    private String orderId;
    private String sku;
    private Date date;
    private String paymentType;
    private String paymentDetail;
    private String transactionType;
    private Money amount;
    private String quantity;
}
