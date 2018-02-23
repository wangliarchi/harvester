package edu.olivet.harvester.feeds.model;

import edu.olivet.foundations.db.PrimaryKey;
import edu.olivet.foundations.ui.ArrayConvertable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 11:31 AM
 */

@SuppressWarnings("DefaultAnnotationParam")
@Table(value = "order_inventory_loader")
@Data
@EqualsAndHashCode(callSuper = false)
public class OrderInventoryUpdateLog  extends PrimaryKey implements ArrayConvertable {

    @Name private String id;
    @Column private String orderId;
    @Column private String sku;
    @Column private String updateType;

    @Override
    public String getPK() {
        return id;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }
}
