package edu.olivet.harvester.selforder.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Objects;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.Remark;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/1/2018 10:38 AM
 */
@Data
@NoArgsConstructor
public class SelfOrder {
    public String ownerAccountCode;
    public String ownerAccountStoreName;
    public String ownerAccountSellerId;
    public String country;
    public String asin;
    public String promoCode;
    public String buyerAccountCode;
    public String buyerAccountEmail;
    public String orderNumber;
    public String cost;
    public String recipientName;
    public String shippingAddress1;
    public String shippingAddress2;
    public String shippingCity;
    public String shippingState;
    public String shippingCountry;
    public String shippingZipCode;
    public String shippingPhoneNumber;
    public String carrier;
    public String uniqueCode;
    public String trackingNumber;
    public String sheetName;
    public String spreadsheetId;
    public int row;

    public boolean fulfilled() {
        return StringUtils.isNotBlank(orderNumber) && Regex.AMAZON_ORDER_NUMBER.isMatched(orderNumber);
    }

    public boolean asinAdded() {
        if (fulfilled()) {
            return true;
        }

        if (StringUtils.isBlank(ownerAccountSellerId) && StringUtils.isBlank(ownerAccountStoreName)) {
            return false;
        }

        return true;
    }

    @JSONField(serialize = false)
    public boolean equalsSuperLite(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SelfOrder order = (SelfOrder) o;
        return Objects.equal(ownerAccountCode, order.ownerAccountCode) &&
                Objects.equal(country, order.country) &&
                Objects.equal(sheetName, order.sheetName) &&
                Objects.equal(recipientName, order.recipientName) &&
                Objects.equal(StringUtils.stripStart(asin, "0"), StringUtils.stripStart(order.asin, "0"));
    }
}
