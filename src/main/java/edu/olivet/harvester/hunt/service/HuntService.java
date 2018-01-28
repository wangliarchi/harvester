package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.logger.SellerHuntingLogger;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 3:55 PM
 */
public class HuntService {
    private static final SellerHuntingLogger LOGGER = SellerHuntingLogger.getInstance();

    @Inject SellerService sellerService;
    @Inject HuntVariableService huntVariableService;


    public Seller huntForOrder(Order order) {
        Long start = System.currentTimeMillis();
        //find sellers on amazon
        List<Seller> sellers = sellerService.getSellersForOrder(order);

        if (CollectionUtils.isEmpty(sellers)) {
            throw new BusinessException("No seller found");
        }

        //set calculation variables
        sellers.forEach(it -> huntVariableService.setHuntingVariable(it, order));

        //sort sellers
        sellers.sort(SellerComparator.getInstance());
        LOGGER.info(order, "total {} valid sellers found  - \n{}\n",
                sellers.size(), StringUtils.join(sellers, "\n")
        );

        Seller seller = sellers.get(0);
        LOGGER.info(order, "found seller [{}], take {} - {}\n", seller.getName(), Strings.formatElapsedTime(start), seller);

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
