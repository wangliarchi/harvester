package edu.olivet.harvester.feeds.service;

import com.google.inject.Inject;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.harvester.feeds.helper.InventoryUpdateTypeHelper.UpdateType;
import edu.olivet.harvester.feeds.model.InventoryUpdateRecord;
import edu.olivet.harvester.feeds.model.OrderInventoryUpdateLog;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.nutz.dao.Cnd;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 1:58 PM
 */
public class OrderInventoryUpdateLogService {
    @Inject DBManager dbManager;

    public boolean updated(String orderId, String sku, UpdateType updateType) {
        List<OrderInventoryUpdateLog> result = dbManager.query(OrderInventoryUpdateLog.class,
                Cnd.where("orderId", "=", orderId)
                        .and("sku", "=", sku)
                        .and("updateType", "=", updateType.name()));

        return CollectionUtils.isNotEmpty(result);
    }

    public OrderInventoryUpdateLog saveFromRecord(InventoryUpdateRecord record) {
        OrderInventoryUpdateLog log = new OrderInventoryUpdateLog();
        log.setOrderId(record.getOrderId());
        log.setSku(record.getSku());
        log.setUpdateType(record.getType().name());
        log.setId(DigestUtils.sha256Hex(log.toString()));
        dbManager.insertOrUpdate(log, OrderInventoryUpdateLog.class);

        return log;
    }
}
