package edu.olivet.harvester.hunt.model;

import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 11:42 AM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HuntStandard {

    public enum Type {
        UsedBook("Used Book"),
        NewBook("New Book"),
        Product("Product");

        String desc;

        Type(String desc) {
            this.desc = desc;
        }

        public static Type init(OrderItemType type, Condition condition) {
            if (type == OrderItemType.PRODUCT) {
                return Product;
            }
            if (condition.used()) {
                return UsedBook;
            } else {
                return NewBook;
            }
        }

        public String getDesc() {
            return desc;
        }
    }

    Rating monthlyRating;
    Rating yearlyRating;

    //可选新书类/CD，本年好评率93%，本年rating数60，本月好评率93%，本月好评数1。
    //可选旧书类/CD，本年好评率90%，本年rating数60，本月好评率90%，本月好评数1。

    public static HuntStandard getByType(Type type) {
        switch (type) {
            case NewBook:
                return newBookDefault();
            case UsedBook:
                return usedBookDefault();
            default:
                return newProductDefault();
        }

    }

    public static HuntStandard newBookDefault() {
        Rating monthlyRating = new Rating(93, 1, RatingType.Last30Days);
        Rating yearlyRating = new Rating(93, 60, RatingType.Last12Month);
        return new HuntStandard(monthlyRating, yearlyRating);
    }

    public static HuntStandard usedBookDefault() {
        Rating monthlyRating = new Rating(90, 1, RatingType.Last30Days);
        Rating yearlyRating = new Rating(90, 60, RatingType.Last12Month);
        return new HuntStandard(monthlyRating, yearlyRating);
    }

    public static HuntStandard newProductDefault() {
        Rating monthlyRating = new Rating(80, 1, RatingType.Last30Days);
        Rating yearlyRating = new Rating(85, 30, RatingType.Last12Month);
        return new HuntStandard(monthlyRating, yearlyRating);
    }


    public boolean monthlyRatingQualified(Rating rating) {
        return rating != null && rating.getCount() >= monthlyRating.getCount() && rating.getPositive() >= monthlyRating.getPositive();

    }

    public boolean yearlyRatingQualified(Rating rating) {
        return rating != null && rating.getCount() >= yearlyRating.getCount() && rating.getPositive() >= yearlyRating.getPositive();

    }
}
