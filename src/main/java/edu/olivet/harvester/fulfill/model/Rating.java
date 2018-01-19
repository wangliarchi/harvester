package edu.olivet.harvester.fulfill.model;

import edu.olivet.foundations.ui.UIText;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:29 AM
 */
public class Rating {
    /**
     * 必选Seller的年度Rating数基准
     */
    private static final int BEST_SELLER_YEAR_RATING_COUNT = 1000;
    /**
     * 可选Seller的年度Rating数基准
     */
    private static final int GOOD_SELLER_YEAR_RATING_COUNT = 60;
    /**
     * 必选、可选Seller的月Rating数基准
     */
    private static final int BEST_GOOD_MONTH_RATING_COUNT = 2;

    /**
     * 欧洲Seller的月Rating数基准
     */
    private static final int EUACC_GOOD_MONTH_RATING_COUNT = 10;

    /**
     * 必选Seller的年度Rating基准
     */
    private static final int BEST_SELLER_YEAR_RATING = 98;
    /**
     * 必选Seller的月Rating基准
     */
    private static final int BEST_SELLER_MONTH_RATING = 98;

    /**
     * 欧洲Seller的月Rating数基准
     */
    private static final int EUACC_SELLER_MONTH_RATING = 97;

    /**
     * 可选Seller的年度Rating基准
     */
    private static final int GOOD_SELLER_YEAR_RATING = 95;
    /**
     * 可选Seller的月Rating基准
     */
    private static final int GOOD_SELLER_MONTH_RATING = 95;

    /**
     * 可选产品Seller的年度Rating基准
     */
    private static final int GOOD_PRODUCT_SELLER_YEAR_RATING = 90;
    /**
     * 可选产品Seller的年度Rating基准
     */
    private static final int GOOD_PRODUCT_SELLER_MONTH_RATING = 90;

    /**
     * 必选Seller的月Rating底线：即便其在Seller库中是必选Seller，其Rating也不能低于底限
     */
    public static final int BEST_SELLER_LOWEST_MONTH_RATING = 90;

    /**
     * Rating基准，主要用于判定必选、可选Seller
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Jan 6, 2015 8:27:44 AM
     */
    public static class Standard {
        /**
         * 必选书类/CD及产品Seller标准
         */
        public static final Standard BEST =
                new Standard(BEST_SELLER_YEAR_RATING_COUNT, BEST_SELLER_YEAR_RATING, BEST_GOOD_MONTH_RATING_COUNT, BEST_SELLER_MONTH_RATING);
        /**
         *可选新书/CD Seller标准
         */
        public static final Standard GOOD =
                new Standard(GOOD_SELLER_YEAR_RATING_COUNT, GOOD_SELLER_YEAR_RATING, BEST_GOOD_MONTH_RATING_COUNT, GOOD_SELLER_MONTH_RATING);
        /**
         *可选旧书/CD Seller标准
         */
        public static final Standard GOOD_USED =
                new Standard(GOOD_SELLER_YEAR_RATING_COUNT, GOOD_SELLER_YEAR_RATING - 2, BEST_GOOD_MONTH_RATING_COUNT, GOOD_SELLER_MONTH_RATING - 2);
        /**
         *可选产品Seller标准
         */
        public static final Standard GOOD_PRODUCT =
                new Standard(GOOD_SELLER_YEAR_RATING_COUNT, GOOD_PRODUCT_SELLER_YEAR_RATING, BEST_GOOD_MONTH_RATING_COUNT, GOOD_PRODUCT_SELLER_MONTH_RATING);
        /**
         *AP或者WareHouse的Dummy Standard(通常无视Standard)
         */
        public static final Standard AP_OR_WAREHOUSE =
                new Standard(Rating.AP_COUNT, Rating.AP_POSITIVE, Rating.AP_COUNT, Rating.AP_POSITIVE);

        /**
         *欧洲acceptable Seller标准
         */
        public static final Standard EU_ACC =
                new Standard(BEST_SELLER_YEAR_RATING_COUNT, BEST_SELLER_YEAR_RATING, EUACC_GOOD_MONTH_RATING_COUNT, EUACC_SELLER_MONTH_RATING);

        public Standard() {
        }

        /**
         *是否高于某一指定标准线
         *
         *@param std 待比较标准线
         */
        public boolean pass(Standard std) {
            return this.yearCount >= std.yearCount && this.yearRating >= std.yearRating &&
                    this.monthCount >= std.monthCount && this.monthRating >= std.monthRating;
        }

        public Standard(int yearCount, int yearRating, int monthCount, int monthRating) {
            this.yearCount = yearCount;
            this.yearRating = yearRating;
            this.monthCount = monthCount;
            this.monthRating = monthRating;
        }

        /**
         *年度Rating数标准
         */
        int yearCount;
        /**
         *年度Rating标准
         */
        int yearRating;
        /**
         *当月Rating数标准
         */
        int monthCount;
        /**
         *当月Rating标准
         */
        int monthRating;

        //@Override
        public String toString() {
            return UIText.text("supplier.rating.standard", yearRating, yearCount, monthRating, monthCount);
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

    /**
     * 判定一个Rating是否无效(具体表现为该列网页显示结果实际可能为"-"，解析为0)
     */
    public boolean invalid() {
        return positive == 0 && neutral == 0 && negative == 0 && count == 0;
    }

    public Rating(int[] positive, int[] neutral, int[] negative, int count) {
        super();
        this.positive = positive[0];
        this.positiveCount = positive[1];
        this.neutral = neutral[0];
        this.neutralCount = neutral[1];
        this.negative = negative[0];
        this.negativeCount = negative[1];
        this.count = count;
    }

    public static void main(String[] args) {
        System.out.println(new Rating(new int[] {91, 192}, new int[] {1, 3}, new int[] {7, 15}, 210));
    }

    public Rating() {
    }

    public Rating(int positive, int neutral, int negative, int count) {
        super();
        this.positive = positive;
        this.neutral = neutral;
        this.negative = negative;
        this.count = count;
    }

    /**
     * 好评率
     */
    private int positive;
    /**
     * 中评率
     */
    private int neutral;
    /**
     * 差评率
     */
    private int negative;
    /**
     * feedback总数
     */
    private int count;
    /**
     * 好评数
     */
    private int positiveCount;
    /**
     * 中评数
     */
    private int neutralCount;
    /**
     * 差评数
     */
    private int negativeCount;

    public int getPositive() {
        return positive;
    }

    public void setPositive(int positive) {
        this.positive = positive;
    }

    public int getNeutral() {
        return neutral;
    }

    public void setNeutral(int neutral) {
        this.neutral = neutral;
    }

    public int getNegative() {
        return negative;
    }

    public void setNegative(int negative) {
        this.negative = negative;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + count;
        result = prime * result + negative;
        result = prime * result + negativeCount;
        result = prime * result + neutral;
        result = prime * result + neutralCount;
        result = prime * result + positive;
        result = prime * result + positiveCount;
        return result;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Rating other = (Rating) obj;
        if (count != other.count) {
            return false;
        }
        if (negative != other.negative) {
            return false;
        }
        if (negativeCount != other.negativeCount) {
            return false;
        }
        return neutral == other.neutral && neutralCount == other.neutralCount && positive == other.positive && positiveCount == other.positiveCount;
    }

    @Override
    public String toString() {
        return UIText.text("supplier.rating.summary", this.positive, this.positiveCount, this.neutral, this.neutralCount,
                this.negative, this.negativeCount, this.count);
    }

    public String abbrevCount() {
        return String.format("%s/%s/%s/%s", this.count, this.positiveCount, this.neutralCount, this.negativeCount);
    }

    public int getPositiveCount() {
        return positiveCount;
    }

    public void setPositiveCount(int positiveCount) {
        this.positiveCount = positiveCount;
    }

    public int getNeutralCount() {
        return neutralCount;
    }

    public void setNeutralCount(int neutralCount) {
        this.neutralCount = neutralCount;
    }

    public int getNegativeCount() {
        return negativeCount;
    }

    public void setNegativeCount(int negativeCount) {
        this.negativeCount = negativeCount;
    }
}
