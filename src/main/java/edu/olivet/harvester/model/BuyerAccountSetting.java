package edu.olivet.harvester.model;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/5/18 10:09 AM
 */
@Data
public class BuyerAccountSetting {
    private Account buyerAccount;
    private List<Country> countries;
    private String type;
    private String primeBuyer;
}
