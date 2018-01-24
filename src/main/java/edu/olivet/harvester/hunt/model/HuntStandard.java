package edu.olivet.harvester.hunt.model;

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


    Rating monthlyRating;
    Rating yearlyRating;

    //可选新书类/CD，本年好评率93%，本年rating数60，本月好评率93%，本月好评数1。
    //可选旧书类/CD，本年好评率90%，本年rating数60，本月好评率90%，本月好评数1。

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
        if (rating == null) {
            return false;
        }

        return rating.getCount() >= monthlyRating.getCount() &&
                rating.getPositive() >= monthlyRating.getPositive();
    }

    public boolean yearlyRatingQualified(Rating rating) {
        if (rating == null) {
            return false;
        }

        return rating.getCount() >= yearlyRating.getCount() &&
                rating.getPositive() >= yearlyRating.getPositive();
    }
}
