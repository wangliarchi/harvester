package edu.olivet.harvester.finance.model;

import edu.olivet.foundations.amazon.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/1/2018 1:37 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadParams {
    List<Account> buyerAccounts;
    Date fromDate;
    Date toDate;
    boolean taskMode = false;
}
