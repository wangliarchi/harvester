package edu.olivet.harvester.fulfill.service;

import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Remark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 4:15 PM
 */
public class BlacklistBuyer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlacklistBuyer.class);


    public boolean isBlacklist(String name, String email, Address address) {
        return false;
    }

    public void appendRemark(Order order) {
        order.remark = Remark.BLACKLIST_BUYER.appendTo(order.remark);
    }


}
