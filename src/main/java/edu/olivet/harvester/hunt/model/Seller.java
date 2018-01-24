package edu.olivet.harvester.hunt.model;

import com.google.common.base.Objects;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.SellerEnums.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:24 AM
 */
@Data
public class Seller {

    public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");

    /*寻找Seller过程中所得分数*/
    private Integer score = 0;

    /*Seller所属亚马逊国家，比如是英国Seller还是美国Seller */
    /**
     * Seller 有两个国家属性，第一个是seller所在offerlisting国家，第二个是seller描述里面的实际发货国家。
     * 这个country是offerlisting所在国家。
     */
    private Country offerListingCountry;
    /**
     * Seller编号，主要用于日志显示
     */
    private String sortId;
    /**
     * 在Seller列表页面的序号
     */
    private int index;
    /**
     * Seller类型
     */
    private SellerType type;
    /**
     * Seller名称
     */
    private String name;
    /**
     * Seller UUID
     */
    private String uuid;
    /**
     * 当前定价
     */
    private Money price;
    /**
     * 综合花费，包含了运费、收税等累计结果
     */
    private Money estimatedCost;
    /**
     * 网页上面显示的运费
     */
    private Money shippingFee;
    /**
     * 用于计算参考利润的运费，可能不同于{@link #shippingFee}
     */
    private Money shippingFee4Profit;
    /**
     * 产品新旧情况
     */
    private Condition condition;
    /**
     * 产品新旧情况详情，一般Used的产品需要关注其详情
     */
    private String conditionDetail;
    /**
     * 当前Seller销售产品是否为AddOn形式
     */
    private boolean addOn;

    /**
     * 是否需要Prime买家提前登陆才能加入购物车
     */
    private boolean needPreLogin;


    /**
     * 库存状态
     */
    private StockStatus stockStatus;
    /**
     * 库存文本内容
     */
    private String stockText;
    /**
     * 是否支持快递
     */
    private boolean expeditedAvailable;
    /**
     * 是否支持国际运输
     */
    private boolean intlShippingAvailable;
    /**
     * 发货地所在州
     */
    private String shippingFromState;
    /**
     * 发货地所在国, 这个国家大部分时候和seller所在卖场一致，但是少部分时候，seller描述中说明是此卖场以外的国家发货的
     * 这个参数会很大地影响发货时间，所以需要考虑
     */
    private Country shipFromCountry;


    private Date latestDeliveryDate;

    /**
     * 显示在当前页面的综合年度Rating
     */
    private int rating;
    /**
     * 显示在当前页面的Rating总数
     */
    private int ratingCount;
    /**
     * 当前Seller的rating详情页面链接地址
     */
    private String ratingUrl;
    /**
     * rating情况
     */
    private Map<RatingType, Rating> ratings;


    /**
     * Seller 成本计算系数
     */
    private float sellerVariable = 0f;

    /**
     * Rating 安全值
     */
    private float ratingVariable = 0f;

    /**
     * Shipping 成本计算系数
     */
    private float shippingVariable = 0f;


    public float getTotalForCalculation() {
        return getTotalPriceInUSD() + sellerVariable + ratingVariable + shippingVariable;
    }

    /**
     * 获取当前Seller的ID(名称+UUID)
     */
    public String id() {
        String sellerName = StringUtils.defaultString(this.name);
        return StringUtils.isNotBlank(this.uuid) ? String.format("%s(ID:%s)", sellerName, this.uuid) : sellerName;
    }

    /**
     * Seller 有两个国家属性，
     * 第一个是seller所在offerlisting国家，country_OfferListing
     * 第二个是seller描述里面的实际发货国家。country_ShippingFrom
     */
    public Country getShippingFromCountry() {
        if (shipFromCountry == null) {
            shipFromCountry = this.offerListingCountry;
        }

        return shipFromCountry;
    }

    public void setStockStatusFromText(String deliveryText) {
        stockText = deliveryText;
        stockStatus = StockStatus.parseFromText(deliveryText);
    }

    public boolean isInStock() {
        return stockStatus == StockStatus.InStock || stockStatus == StockStatus.WillBeInStockSoon;
    }

    public boolean isPrime() {
        return SellerType.isPrime(type);
    }

    public boolean isPt() {
        return !isPrime();
    }

    public boolean isAP() {
        return type.isAP();
    }

    public boolean isExpeditedAvailable() {
        if (SellerType.isPrime(type)) {
            return true;
        }

        return expeditedAvailable;

    }

    public boolean isIntlShippingAvailable() {
        if (type == SellerType.AP) {
            return true;
        }

        return intlShippingAvailable;
    }

    public boolean canDirectShip(Country orderCountry) {

        if (shipFromCountry == orderCountry) {
            return true;
        }

        return isIntlShippingAvailable();
    }

    /**
     * 如果seller offer listing 所在国家 和 订单 邮寄国家 不一致，
     * 视为国际 seller，计算的时候 要加国际邮费
     */
    public boolean isIntlSeller(Order order) {
        return !CountryStateUtils.getInstance().getCountryCode(order.ship_country)
                .equalsIgnoreCase(offerListingCountry.code());
    }

    public String getRatingUrl() {
        if (StringUtils.isNotBlank(ratingUrl) && !StringUtils.startsWithIgnoreCase(ratingUrl, "http")) {
            ratingUrl = offerListingCountry.baseUrl() + ratingUrl;
        }

        return ratingUrl;
    }


    public Float getTotalPriceInUSD() {
        return price.toUSDAmount().floatValue() + shippingFee.toUSDAmount().floatValue();
    }

    public Rating getRatingByType(RatingType type) {
        if (ratings == null) {
            return null;
        }
        return ratings.getOrDefault(type, null);
    }

    /**
     * 将部分可能为null的字符串属性设为空白字符串
     */
    public void autoCorrect() {
        this.uuid = StringUtils.defaultString(this.uuid);
        this.conditionDetail = StringUtils.defaultString(this.conditionDetail);
    }

    //@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((condition == null) ? 0 : condition.hashCode());
        result = prime * result + ((offerListingCountry == null) ? 0 : offerListingCountry.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((sortId == null) ? 0 : sortId.hashCode());
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    //@Override
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

        Seller other = (Seller) obj;

        return Objects.equal(condition, other.condition) &&
                Objects.equal(name, other.name) &&
                Objects.equal(type, other.type) &&
                Objects.equal(uuid, other.uuid) &&
                Objects.equal(offerListingCountry, other.offerListingCountry);
    }


    /**
     * 获取Seller基本信息文本
     */
    public String toString() {
        return this.offerListingCountry.name() + ", " + this.name + ", " + this.getTotalForCalculation() + ", " + this.uuid + ", " +
                this.price.usdText() + ", " + this.shippingFee.usdText() + ", " +
                this.condition + ", " + this.type.abbrev() + ", " +
                this.rating + "%, " + this.ratingCount + ", " +
                getRatingByType(RatingType.Last30Days) + ", " + getRatingByType(RatingType.Last12Month) + ", " +
                sellerVariable + ", " + ratingVariable + ", " + shippingVariable;

    }
}
