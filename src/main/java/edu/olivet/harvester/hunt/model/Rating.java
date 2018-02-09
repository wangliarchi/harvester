package edu.olivet.harvester.hunt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:29 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rating {

    /**
     * 好评率
     */
    private int positive;
    /**
     * feedback总数
     */
    private int count;

    private RatingType type;


    public enum RatingType {
        Last30Days(1),
        Last90Days(2),
        Last12Month(3),
        Lifetime(4);

        private int index;

        RatingType(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * AP实际没有Rating Count，为了方便用于比较，将其设置为一个虚拟、高值
     */
    public static final int AP_COUNT = 999999;
    /**
     * AP实际没有好评率，为了方便用于比较，将其设置为100%
     */
    public static final int AP_POSITIVE = 100;


    public Rating(RatingType type) {
        this.type = type;
    }

    public static Map<RatingType, Rating> apRatings() {
        Map<RatingType, Rating> apRatings = new HashMap<>();
        for (RatingType type : RatingType.values()) {
            apRatings.put(type, new Rating(AP_POSITIVE, AP_COUNT, type));
        }
        return apRatings;
    }

    /**
     * 判定一个Rating是否无效(具体表现为该列网页显示结果实际可能为"-"，解析为0)
     */
    public boolean invalid() {
        return positive == 0 && count == 0;
    }


    @Override
    public String toString() {
        return this.positive + "%, " + this.count;
    }

    /**
     * <summary >
     * Ratings
     * (Lower bound of Wilson score confidence interval for a Bernoulli parameter)
     * </summary>
     * <param name="positive">Positive ratings</param>
     * <param name="negative">Negative ratings</param>
     * <returns></returns>
     */
    public double ratingScore(int positive, int negative) {
        return (((positive + 1.9208) / (positive + negative) -
                1.96 * Math.sqrt(((positive * negative) / (positive + negative)) + 0.9604) / (positive + negative)) / (1 + 3.8416 / (positive + negative)));
    }

    public double score() {
        int positiveCount = (int) ((float) count * (float) positive / 100f);
        int negativeCount = count - positiveCount;
        return ratingScore(positiveCount, negativeCount);
    }

    public static void main(String[] args) {
        Rating rating = new Rating(99, 194, RatingType.Last30Days);
        System.out.println(rating.score());

        rating = new Rating(95, 39, RatingType.Last30Days);
        System.out.println(rating.score());
    }
}
