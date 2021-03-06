package edu.olivet.harvester.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/3/17 2:00 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditCard {
    private String accountEmail;
    private String cardNo;
    private String cvv;

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (StringUtils.isBlank(accountEmail)) {
            errors.add("Account email not provided");
        }
        //todo validate card no
        if (StringUtils.isBlank(cardNo)) {
            errors.add("Credit card no not provided");
        }

        if (StringUtils.isBlank(cvv)) {
            errors.add("CVV not provided");
        }

        return errors;
    }

    public String lastDigits() {
        cardNo = cardNo.replaceAll("\\s","");
        return cardNo.substring(cardNo.length() - 4);
    }
}
