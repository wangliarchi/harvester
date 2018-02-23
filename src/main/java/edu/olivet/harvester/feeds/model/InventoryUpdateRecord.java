package edu.olivet.harvester.feeds.model;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.feeds.helper.InventoryUpdateTypeHelper.UpdateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.recycler.Recycler.C;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 1:56 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryUpdateRecord {

    private String sku;
    private String asin;
    private UpdateType type;
    private String orderId;

    /**
     * 订单利润较好，补点数量: {@value}
     */
    static final int QUANTITY_TO_ADD = 3;

    static final int LEAD_TIME_TO_SHIP = 7;

    static final int LEAD_TIME_TO_SHIP_IN = 20;

    public int getQty() {
        if (type == UpdateType.AddQuantity) {
            return QUANTITY_TO_ADD;
        }

        return 0;
    }

    public int getLeadTime(Country country) {
        if (country == Country.IN) {
            return LEAD_TIME_TO_SHIP_IN;
        }

        if (country == Country.US) {
            return 1;
        }

        return LEAD_TIME_TO_SHIP;
    }
}
