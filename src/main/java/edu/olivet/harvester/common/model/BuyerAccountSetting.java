package edu.olivet.harvester.common.model;

import edu.olivet.foundations.amazon.Account;
import lombok.Data;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/5/18 10:09 AM
 */
@Data
public class BuyerAccountSetting {
    private Account buyerAccount;
    private String countryName = "All";
    private String type;
    private String primeBuyer;
    private String accountNo = "0";
}
