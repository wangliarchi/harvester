package edu.olivet.harvester.fulfill.model;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.model.Money;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:24 AM
 */
@Data
public class Seller {

    public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");

    /*寻找Seller过程中所得分数*/
    private Integer score = 0;


    /**
     * 获取Seller基本信息文本
     */
    public String toSimpleString() {
        return "[" + this.offerListingCountry.name() + ", " + this.name + ", " + this.uuid + ", " + DOUBLE_FORMAT.format(this.price) + ", " +
                DOUBLE_FORMAT.format(this.shippingFee) + ", " + this.condition + ", " + this.type.abbrev() + ", " +
                this.rating + "%, " + this.ratingCount + ", " + (this.determinedStatus == null ? StringUtils.EMPTY : this.determinedStatus.desc()) + "]";
    }

    /**
     * 获取当前Seller的ID(名称+UUID)
     */
    public String id() {
        String sellerName = StringUtils.defaultString(this.name);
        return StringUtils.isNotBlank(this.uuid) ? String.format("%s(ID:%s)", sellerName, this.uuid) : sellerName;
    }

    /* Seller所属亚马逊国家，比如是英国Seller还是美国Seller */
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
    private SellerEnums.SellerType type;
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
    private String condition;
    /**
     * 产品新旧情况详情，一般Used的产品需要关注其详情
     */
    private String conditionDetail;
    /**
     * 当前Seller销售产品是否为AddOn形式
     */
    private boolean addOn;
    /**
     * 是否为电子产品
     */
    private boolean electronic;
    /**
     * 是否需要Prime买家提前登陆才能加入购物车
     */
    private boolean needPreLogin;
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
     * 库存状态
     */
    private SellerEnums.StockStatus stockStatus;
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
    /**
     * 本月顾客反馈rating情况
     */
    private Rating monthRating;
    /**
     * 本季度顾客反馈rating情况
     */
    private Rating quarterRating;
    /**
     * 本年度顾客反馈rating情况
     */
    private Rating yearRating;
    /**
     * 上架以来顾客反馈rating情况
     */
    private Rating lifeTimeRating;
    /**
     * 考量评估之后该Seller的Status
     */
    private boolean lowquality;

    private float firstcycleprofit;

    private float secondcycleprofit;

    private boolean goodProfitSeller;
    private SellerEnums.SellerStatus determinedStatus;
    private boolean instock = true;
    private boolean needtoFWD = false;


    public boolean isExpeditedAvailable() {
        return expeditedAvailable;
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


    public void setShipFromCountry(String shipFromCountryString) {
        if (shipFromCountryString.contains("United States")) {
            this.shipFromCountry = Country.US;
        } else if (shipFromCountryString.contains("United Kingdom") || shipFromCountryString.contains("Vereinigtes Königreich")) {
            // Vereinigtes Königreich 是德语
            this.shipFromCountry = Country.UK;
        } else if (shipFromCountryString.contains("France")) {
            this.shipFromCountry = Country.FR;
        } else if (shipFromCountryString.contains("Germany") || shipFromCountryString.contains("Deutschland")) {
            this.shipFromCountry = Country.DE;
        } else if (shipFromCountryString.contains("Spanish")) {
            this.shipFromCountry = Country.ES;
        } else if (shipFromCountryString.contains("Italy")) {
            this.shipFromCountry = Country.IT;
        } else if (shipFromCountryString.contains("Japan")) {
            this.shipFromCountry = Country.JP;
        } else if (shipFromCountryString.contains("Canada")) {
            this.shipFromCountry = Country.CA;
        } else if (shipFromCountryString.contains("Mexico")) {
            this.shipFromCountry = Country.MX;
        } else if (shipFromCountryString.contains("Thailand")) {
            this.shipFromCountry = null;
        } else if (shipFromCountryString.length() > 1) {
            this.shipFromCountry = null;
        } else {
            // 如果没有任何发货国家描述，那应该是发货国家就是seller所在amazon
            this.shipFromCountry = this.offerListingCountry;
        }
    }


    public void setInstock(String deliveryStr) {
        if (!this.instock) {
            return;
        }

        // 德语， Currently not on stock
        if (deliveryStr.contains("Derzeit nicht auf Lager.")) {
            this.instock = false;
        }
    }


    /**
     * 将部分可能为null的字符串属性设为空白字符串
     */
    public void autoCorrect() {
        this.uuid = StringUtils.defaultString(this.uuid);
        this.condition = StringUtils.defaultString(this.condition);
        this.conditionDetail = StringUtils.defaultString(this.conditionDetail);
    }

    //	@Override
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
        if (condition == null) {
            if (other.condition != null) {
                return false;
            }
        } else if (!condition.equals(other.condition)) {
            return false;
        }
        if (offerListingCountry != other.offerListingCountry) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (sortId == null) {
            if (other.sortId != null) {
                return false;
            }
        } else if (!sortId.equals(other.sortId)) {
            return false;
        }
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }


    public boolean isAddOn() {
        return addOn;
    }

    public boolean isElectronic() {
        return electronic;
    }

    public boolean isNeedPreLogin() {
        return needPreLogin;
    }

    public boolean isIntlShippingAvailable() {
        return intlShippingAvailable;
    }

    public boolean isNeedtoFWD() {
        return needtoFWD;
    }
}
