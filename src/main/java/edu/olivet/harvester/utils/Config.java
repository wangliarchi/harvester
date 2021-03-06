package edu.olivet.harvester.utils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 1:37 PM
 */
public enum Config {
    AddressAbbrev("abbrev.properties", "地址缩写"),
    CountryCode("country-codes", "国家代码"),
    CountryName("country-names", "国家"),
    USStates("us-states", "US States"),
    CAProvinces("ca-provinces", "CA Provinces"),
    EUCountry("eu-countries", "欧盟国家"),
    AmazonAddressRule("addressrule", "亚马逊地址转换规则"),
    ConditionLevel("condition-level", "产品新旧等级设定"),
    ProfileList("profileList.properties", "性能监控列表"),
    Condition("conditions.properties", "Condition转换表"),
    ForbiddenSellers("forbidden_sellers.json", "Forbidden Sellers"),
    StopWords("stopwords.json", "Stopwords"),
    USForwardAddress("usBookTransAddr.json", "US FWD Address"),
    UKForwardAddress("ukTransAddr.json", "UK FWD Address"),
    ShippingFeeLimit("shipping-limit.json", "Shipping Fee Limit"),
    Names("names", "Common used name list"),
    Translation("translations.properties", ""),
    Stock("stock.json", "库存检查规则");

    private String fileName;
    private String desc;

    public String fileName() {
        return fileName;
    }

    public String desc() {
        return desc;
    }

    Config(String fileName, String desc) {
        this.fileName = fileName;
        this.desc = desc;
    }
}
