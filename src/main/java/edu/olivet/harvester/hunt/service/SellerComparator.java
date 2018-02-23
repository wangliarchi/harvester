package edu.olivet.harvester.hunt.service;

import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;

import java.util.Comparator;

/**
 * 亚马逊商家价格比较器：按照总价升序、Condition降序、好评率降序、总评数降序排序
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Jan 2, 2015 4:01:19 PM
 */
public class SellerComparator implements Comparator<Seller> {

    private static final SellerComparator instance = new SellerComparator();


    public static SellerComparator getInstance() {
        return instance;
    }

    @Override
    public int compare(Seller seller, Seller seller2Compare) {

        // 首先比价格
        int rc = Float.compare(seller.getTotalForCalculation(), seller2Compare.getTotalForCalculation());
        if (rc != 0) {
            return rc;
        }

        // 然后比condition
        rc = -Integer.compare(seller.getCondition().score(), seller2Compare.getCondition().score());
        if (rc != 0) {
            return rc;
        }

        // 然后比rating
        rc = -Double.compare(seller.getRatingByType(RatingType.Last30Days).score(),
                seller2Compare.getRatingByType(RatingType.Last30Days).score());

        if (rc != 0) {
            return rc;
        }

        // 然后比count
        return -Double.compare(seller.getRatingByType(RatingType.Last12Month).score(),
                seller2Compare.getRatingByType(RatingType.Last12Month).score());
    }

}