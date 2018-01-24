package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.model.Seller;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 3:55 PM
 */
public class HuntService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HuntService.class);

    @Inject SellerService sellerService;
    @Inject HuntVariableService huntVariableService;
    @Inject SellerFilter sellerFilter;

    public Seller huntForOrder(Order order) {
        Long start = System.currentTimeMillis();
        //find sellers on amazon
        List<Seller> sellers = sellerService.getAllSellersForOrder(order);

        //remove unqualified sellers
        sellers.removeIf(seller -> !sellerFilter.isPreliminaryQualified(seller, order));

        //todo swingworker
        //get seller ratings on seller profile page
        sellers.forEach(seller -> sellerService.getSellerRatings(seller));

        //remove unqualified sellers
        sellers.removeIf(seller -> !sellerFilter.isQualified(seller, order));

        if (CollectionUtils.isEmpty(sellers)) {
            throw new BusinessException("No seller found for order" + order);
        }

        //set calculation variables
        sellers.forEach(it -> huntVariableService.setHuntingVariable(it, order));

        //sort sellers
        sellers.sort((Seller s1, Seller s2) -> s1.getTotalForCalculation() - s2.getTotalForCalculation() > 0 ? 1 : -1);

        Seller seller = sellers.get(0);
        LOGGER.info("found seller {} for order {}, take {}", seller, order, Strings.formatElapsedTime(start));

        return seller;
    }


    public static void main(String[] args) {
        HuntService hunter = ApplicationContext.getBean(HuntService.class);

        Order order = new Order();
        order.sales_chanel = "Amazon.com";
        order.ship_country = "United States";
        order.sku = "JiuUSTextbook2016-1229-C0093161";
        order.price = "51.48";
        order.shipping_fee = "3.99";
        order.isbn = "1938946103";
        order.original_condition = "Used - Good";

        hunter.huntForOrder(order);

    }
}
