package edu.olivet.harvester.hunt;


import com.google.inject.Inject;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.SellerService;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/19/2018 2:50 PM
 */
public class Hunter {

    @Inject SellerService sellerService;
    public Seller huntForOrder(Order order) {
        List<Seller> sellers = sellerService.getSellersForOrder(order);

        return null;
    }
}
