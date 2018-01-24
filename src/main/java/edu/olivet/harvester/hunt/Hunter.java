package edu.olivet.harvester.hunt;


import com.google.inject.Inject;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.service.HuntVariableService;
import edu.olivet.harvester.hunt.service.SellerService;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/19/2018 2:50 PM
 */
public class Hunter {

    @Inject
    SellerService sellerService;
    @Inject
    HuntVariableService huntVariableService;

    public Seller huntForOrder(Order order) {
        List<Seller> sellers = sellerService.getSellersForOrder(order);
        sellers.forEach(it -> huntVariableService.setHuntingVariable(it, order));
        return null;
    }

    public static void main(String[] args) {
        Hunter hunter = ApplicationContext.getBean(Hunter.class);

        Order order = new Order();
        order.sales_chanel = "Amazon.com";
        order.ship_country = "United States";
        order.sku = "BKXXX";
        order.price = "51.00";
        order.shipping_fee = "2.99";
        order.isbn = "0323377033";
        order.original_condition = "New - New";

        hunter.huntForOrder(order);

    }
}
