package edu.olivet.harvester.common.model;

import edu.olivet.foundations.amazon.Account;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

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
    private boolean validPrime = true;
    private Date lastPrimeCheck;

    public boolean isPrime() {
        return StringUtils.equalsAnyIgnoreCase(primeBuyer, "Prime", "Both");
    }
}
