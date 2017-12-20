package edu.olivet.harvester.model;

import com.google.common.base.Preconditions;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * 订单批注，通常包含特定的业务意义
 * 约定: 读取需要支持中英文，写入只支持英文，如果存在多条记录，取最后一条（最通用、完整的文本）
 * </pre>
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
public enum Remark {
    /**
     * 直寄标识前缀，用于检查非法直寄标识
     */
    SWITCH_COUNTRY(new String[]{"直寄"}, new String[]{"Shipment"}, false),

    /**
     * 美国直寄
     */
    FULFILL_FROM_US(new String[]{"US做单", "US直寄"}, new String[]{"US Shipment"}, true),
    /**
     * 加拿大直寄
     */
    FULFILL_FROM_CA(new String[]{"CA做单", "CA直寄"}, new String[]{"CA Shipment"}, true),
    /**
     * 英国直寄
     */
    FULFILL_FROM_UK(new String[]{"UK做单", "UK直寄"}, new String[]{"UK Shipment"}, true),
    /**
     * 法国直寄
     */
    FULFILL_FROM_FR(new String[]{"FR做单", "FR直寄"}, new String[]{"FR Shipment"}, true),
    /**
     * 德国直寄
     */
    FULFILL_FROM_DE(new String[]{"DE做单", "DE直寄"}, new String[]{"DE Shipment"}, true),
    /**
     * 意大利直寄
     */
    FULFILL_FROM_IT(new String[]{"IT做单", "IT直寄"}, new String[]{"IT Shipment"}, true),
    /**
     * 西班牙直寄
     */
    FULFILL_FROM_ES(new String[]{"ES做单", "ES直寄"}, new String[]{"ES Shipment"}, true),

    /**
     * 买回转运，也即先买回到US Warehouse，再发货给客户
     */
    PURCHASE_BACK(new String[]{"买回", "US买回"}, new String[]{"Purchase It Back", "US FWD"}, true),
    /**
     * 快递买回转运，也即先快递买回到US Warehouse，再发货给客户
     */
    PURCHASE_BACK_EX(new String[]{"快递买回", "US快递买回"}, new String[]{"US FWD Expedited"}, true),

    /**
     * UK转运类型，也即先买到UK Warehouse，再发货给客户
     */
    UK_TRANSFER(new String[]{"UK转运"}, new String[]{"UK FWD"}, true),


    /**
     * 自买单
     */
    SELF_ORDER(new String[]{"自買單"}, new String[]{"self order", "self-order"}, true),

    /**
     * UK转运中Seller为AP，需要直接寄到美国LA WareHouse的特定类型，仅用于多单一做场景，注意，这里无需进行US直寄也即切换到美国亚马逊做单的操作
     */
    UK_SWITCH_TO_US(new String[]{"UKAP直寄美国"}, new String[]{"UK AP Ship To USLA"}, true),

    /**
     * 强行指定运输方式为快递
     */
    FAST_SHIPPING(new String[]{"kuaidi", "快递"}, new String[]{"Expedited Shipping"}, true),
    /**
     * 强行指定运输方式为普递
     */
    STANDARD_SHIPPING(new String[]{"pudi", "普递"}, new String[]{"Standard Shipping"}, true),

    /**
     * 部分Prime Seller要求先登录之后才可加入购物车
     */
    PRIME_LOGIN_FIRST(new String[]{"PrLogin"}, new String[]{"PrLogin"}, true),

    /**
     * 标识当前产品为AddOn，不能独立下单
     */
    ADD_ON(new String[]{"AddOn"}, new String[]{"AddOn"}, true),
    /**
     * 标识当前产品为电子产品
     */
    Electronic(new String[]{"Electronic"}, new String[]{"Electronic"}, true),

    /**
     * 无视亏损做单
     */
    FORCE_FULFILL(new String[]{"zuoba", "做吧"}, new String[]{"Please Place The Order"}, true),
    /**
     * 跳过运费检查
     */
    SKIP_SHIPPING_FEE(new String[]{"跳过运费", "跳过运费检查"}, new String[]{"Skip ShippingFee Check"}, true),

    /**
     * 标识当前产品需要先登录Ebates，跳转到亚马逊后做单，以获得少许返现
     */
    EBATES(new String[]{"Ebates"}, new String[]{"Ebates"}, true),

    //----------订单特殊状态标识，通常为客服人员标识，以便制止继续做单---------//
    /**
     * 客户取消了订单, 此时不能再继续做单
     */
    BUYER_CANCELLED(new String[]{"Buyer Cancel", "Buyer Canceled", "Buyer Cancelled"},
            new String[]{"Buyer Cancel", "Buyer Canceled", "Buyer Cancelled"}, true),
    /**
     * 普通Cancel标识，可能是客户取消，或是Supplier取消
     */
    CANCELLED(new String[]{"Cancel"}, new String[]{"Cancel"}, true),
    /**
     * 程序找单结束后，标识需要人工检查
     */
    TO_BE_CHECKED(new String[]{"daicheck", "dai check"}, new String[]{"CHCK"}, true),
    /**
     * 订单被Supplier取消了
     */
    SELLER_CANCELED(new String[]{"Seller Cancel", "Seller Canceled"}, new String[]{"Seller Cancel", "Seller Canceled"}, true),
    /**
     * 客人修改了寄货地址，此时订单需要重做或调整寄送地址等
     */
    ADDRESS_CHANGED(new String[]{"改变地址", "修改地址"}, new String[]{"Change Shipping Address"}, true),
    /**
     * 标识只退还运费
     */
    REFUND_SHIPPING_FEE_ONLY(new String[]{"只退运费"}, new String[]{"Refund ShippingFee"}, true),

    //----------库存更新相关标识---------//
    /**
     * 产品断货标识
     */
    INVALID_ITEM(new String[]{"断货"}, new String[]{"Invalid Item"}, true),
    /**
     * 合并点标识
     */
    MERGED_LISTING(new String[]{"合并点"}, new String[]{"Merged Listing"}, true),
    /**
     * 产品图片不符标识
     */
    WRONG_PICTURE(new String[]{"图片不符"}, new String[]{"Picture Not Matching"}, true),
    /**
     * 产品错误标识
     */
    WRONG_LISTING(new String[]{"错点"}, new String[]{"Wrong Listing"}, true),
    /**
     * 因为其他原因需要删除当前产品标识
     */
    MISC_LISTING_TO_DELETE(new String[]{"其他删点"}, new String[]{"Inappropriate Listing To Delete"}, true),

    //----------订单提交失败错误原因概述---------//
    NO_FAST_SHIPPING(new String[]{"Seller无快递"}, new String[]{"Supplier Does Not Provide Expedited Service"}, false),
    CANNOT_SHIP_TO(new String[]{"寄不到/限购/无货"}, new String[]{"Cannot Ship To/Restriction/Supplier Has No Stock"}, false),
    SHIPPING_FEE_TOO_HIGH(new String[]{"运费过高"}, new String[]{"Shipping Fee Too High:"}, false),
    UNKNOWN_SUBMIT_RESULT(new String[]{"提交结果未知"}, new String[]{"Pending Order with Supplier"}, false),

    FORBIDDEN_SUPPLIER_USED(new String[]{"禁选Seller"}, new String[]{"Forbidden Supplier"}, false),
    BOOK_TITLE_DIFFERENT(new String[]{"书名不一致"}, new String[]{"Different Book Title"}, false),
    SELLER_DISAPPEAR(new String[]{"Seller消失"}, new String[]{"Supplier Disappeared"}, false),
    CONDITION_NOT_ACCEPTABLE(new String[]{"Acceptable"}, new String[]{"Acceptable"}, false),
    SELLER_OUT_OF_STOCK(new String[]{"Seller无货"}, new String[]{"Supplier Has No Stock"}, false),
    SELLER_PRICE_HIGHER_THAN_ORIGINAL(new String[]{"价格比原价高:"}, new String[]{"Price Higher Than Original:"}, false),
    SELLER_PRICE_RISE(new String[]{"Seller涨价到"}, new String[]{"Supplier Lifted Price to:"}, false),
    SELLER_ADJUST_PRIME(new String[]{"Seller实际为Prime"}, new String[]{"Supplier Is Prime"}, false),
    SELLER_ADJUST_PT(new String[]{"Seller可能为Pt"}, new String[]{"Supplier Might Be Non-Prime"}, false),
    CONDITION_SWITCHED(new String[]{"Used找不到,尝试New"}, new String[]{"Try New. Used Not Found."}, false),
    SELLER_TEMPORARILY_OUT_OF_STOCK(new String[]{"Seller暂时无货"}, new String[]{"Supplier Temporarily Has No Stock"}, false),
    /**
     * ASIN对应产品在某一国家亚马逊已不存在
     */
    ITEM_UNAVAILABLE(new String[]{"无货"}, new String[]{" Is Out of Stock"}, false),

    NO_GIFT_OPTION(new String[]{"无GiftOption,差价过高"}, new String[]{"No Gift Option. Big Price Difference."}, false),
    EMAIL_SELLER_NO_INVOICE(new String[]{"写信去Invoice"}, new String[]{"Email Supplier To Remove Invoice"}, false),
    MULTI_ITEMS_NOT_PLACED_COMPLETELY(new String[]{"只买了%s本,还需买%s本"}, new String[]{"Only Completed %s, Still Need %s More"}, false),

    RESTORE_FROM_LOG(new String[]{"从日志回写"}, new String[]{"Retrieved from Log"}, false),

    BY_ORDER_PORTER(new String[]{"By", "Order", "Porter"}, new String[]{"By", "Order", "Porter"}, false),

    MULTI_ORDER(new String[]{"多单"}, new String[]{"MultiItems"}, false),
    LETTER_INVALID(new String[]{"例信模板不存在或不符合规范"}, new String[]{"Non-Existing/Invalid Notification Letter Template"}, false),

    BLACKLIST_BUYER(new String[]{"黑名单买家"}, new String[]{"memo - Blacklist Buyer"}, false);
    /**
     * 中文文本
     */
    private final String[] chinese;
    /**
     * 英文文本
     */
    private final String[] english;
    /**
     * 是否在下拉框可选项之列
     */
    private final boolean inDropdown;

    Remark(String[] chinese, String[] english, boolean inDropdown) {
        this.chinese = chinese;
        this.english = english;
        this.inDropdown = inDropdown;
    }

    /**
     * 获取当前批注对应全部文本描述
     */
    public String[] texts() {
        return ArrayUtils.addAll(this.chinese, this.english);
    }

    /**
     * 用于写入订单批注的最终文本: 最后一个英文文本
     */
    public String text2Write() {
        return this.english[this.english.length - 1];
    }

    private static String removeSpaceAndPunctuation(String source) {
        return StringUtils.defaultString(source).replaceAll(Regex.BLANK.val(), StringUtils.EMPTY)
                .replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY);
    }

    /**
     * <pre>
     * 检查目标文本是否包含当前批注关键词
     * 英语环境下通常单词之间会有空格隔开，为此需要去掉空格、标点符号之后进行比较，以减少少许输入误差带来的问题
     * </pre>
     *
     * @param text 目标原始文本, 通常为订单的remark(S列)
     */
    public boolean isContainedBy(String text) {
        // 对特殊情况进行分别处理
        if (this == Remark.MULTI_ITEMS_NOT_PLACED_COMPLETELY) {
            return quantityDiffered(text);
        }

        String converted = removeSpaceAndPunctuation(text);
        for (String s : this.texts()) {
            if (Strings.containsAnyIgnoreCase(converted, removeSpaceAndPunctuation(s))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查目标文本中的任何一个是否包含当前批注关键词
     *
     * @param texts 目标文本, 通常应当包含一个以上字符串, 对应着订单数据的cost、orderNumber等列
     */
    public boolean isContainedByAny(String... texts) {
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(texts), "Invalid source texts provided.");

        for (String text : texts) {
            if (this.isContainedBy(text)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 部分批注为着醒目起见的目的，需要附加在最前面。其他批注则是附加在最后面
     */
    private static final Remark[] APPEND_BEFORE = {RESTORE_FROM_LOG, UNKNOWN_SUBMIT_RESULT, TO_BE_CHECKED};

    /**
     * 附加批注，如果目标文本已经包含当前批注则略过
     *
     * @param target 目标文本
     */
    public String appendTo(String target, Object... params) {
        if (this == Remark.MULTI_ITEMS_NOT_PLACED_COMPLETELY) {
            throw new IllegalArgumentException("Remark of 'Purchased quantity is less than expected' is not supported to append directly.");
        }
        if (this.isContainedBy(target)) {
            return target;
        }
        if (ArrayUtils.contains(APPEND_BEFORE, this)) {
            return this.text2Write() + getSeparator(target) + target;
        }
        return target + getSeparator(target) + this.text2Write() + StringUtils.join(params, ", ");
    }

    /**
     * 做单成功之后，原先存在的异常提醒批注需要一并清除
     */
    private static final Remark[] NEED_REMOVE_AFTER_SUCCESS = {
            Remark.NO_FAST_SHIPPING, Remark.CANNOT_SHIP_TO,
            Remark.SELLER_OUT_OF_STOCK, Remark.SELLER_DISAPPEAR,
            Remark.CONDITION_NOT_ACCEPTABLE, Remark.SHIPPING_FEE_TOO_HIGH,
            Remark.SELLER_PRICE_HIGHER_THAN_ORIGINAL, Remark.NO_GIFT_OPTION,
            Remark.SELLER_PRICE_RISE
    };

    /**
     * 从目标文本中移除当前批注
     *
     * @param target 目标文本
     */
    public String removeFrom(String target) {
        String result = StringUtils.defaultString(target);
        boolean removeAmt = ArrayUtils.contains(NEED_REMOVE_AMOUNT, this);
        for (String s : this.texts()) {
            if (removeAmt) {
                result = result.replaceAll(s + "([0-9]{1,3}(.[0-9]{2})?)?", StringUtils.EMPTY);
            } else {
                result = result.replace(s, StringUtils.EMPTY);
            }
        }
        return result;
    }

    /**
     * 附加批注时，如果原始文本不为空，添加空格分隔符使其易于阅读
     *
     * @param target 原始文本
     */
    private static String getSeparator(String target) {
        return StringUtils.isNotBlank(target) ? StringUtils.SPACE : StringUtils.EMPTY;
    }

    /**
     * 检查目标文本是否包含任一批注类型
     *
     * @param text    目标原始文本
     * @param remarks 批注类型，可能有多个
     */
    public static boolean matchAny(String text, Remark... remarks) {
        for (Remark remark : remarks) {
            if (remark.isContainedBy(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查目标文本是否包含<strong>所有</strong>批注类型
     *
     * @param text    目标原始文本
     * @param remarks 批注类型，可能有多个
     */
    public static boolean matchAll(String text, Remark... remarks) {
        for (Remark remark : remarks) {
            if (!remark.isContainedBy(text)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据订单批注判定对应的ASIN是否需要删除
     *
     * @param text 订单批注
     */
    public static boolean needASINDeletion(String text) {
        return matchAny(text, Remark.INVALID_ITEM, Remark.MERGED_LISTING,
                Remark.WRONG_PICTURE, Remark.WRONG_LISTING, Remark.MISC_LISTING_TO_DELETE);
    }

    /**
     * 变量标识，部分文本需要变量填充
     */
    private static final String PARAM_INDICATOR = "%s";
    /**
     * 购买数量正则表达式
     */
    private static final String QUANTITY_REGEX = "[0-9]{1,2}";

    /**
     * 多单做单场景下，如果实际提交产品数量少于期待数量，需添加对应批注
     */
    public static String appendPurchasedQuantityNotEnough(String text, int purchased, int left) {
        String result = removePurchasedQuantityNotEnough(text);
        return String.format(Remark.MULTI_ITEMS_NOT_PLACED_COMPLETELY.text2Write(), purchased, left) + getSeparator(result) + result;
    }

    /**
     * 移除多单做单场景下，实际提交产品数量少于期待数量时添加的批注
     */
    public static String removePurchasedQuantityNotEnough(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        String result = text;
        for (String s : Remark.MULTI_ITEMS_NOT_PLACED_COMPLETELY.texts()) {
            String regex = s.replace(PARAM_INDICATOR, QUANTITY_REGEX);
            result = result.replaceAll(regex, StringUtils.EMPTY);
        }
        return result;
    }

    /**
     * 部分批注移除时，也需要将附加的金额信息一并移除
     */
    private static final Remark[] NEED_REMOVE_AMOUNT = {
            Remark.SHIPPING_FEE_TOO_HIGH,
            Remark.SELLER_PRICE_HIGHER_THAN_ORIGINAL,
            Remark.NO_GIFT_OPTION,
            Remark.SELLER_PRICE_RISE
    };

    /**
     * 从目标文本中移除指定的多种批注
     *
     * @param target  目标文本
     * @param remarks 待移除的批注
     */
    public static String removeAll(String target, Remark... remarks) {
        if (StringUtils.isBlank(target)) {
            return StringUtils.EMPTY;
        }
        String result = target;
        for (Remark remark : remarks) {
            result = remark.removeFrom(result);
        }
        return result;
    }

    /**
     * 订单提交成功之后，将原先仍残留的失败Remark信息悉数清除
     */
    public static String removeFailedRemark(String target) {
        return removeAll(target, NEED_REMOVE_AFTER_SUCCESS);
    }

    /**
     * 判定文本是否包含实际购买数量小于预期购买数量的内容，这会决定后续操作比如标灰
     *
     * @param text 订单批注文本
     */
    public static boolean quantityDiffered(String text) {
        for (String s : Remark.MULTI_ITEMS_NOT_PLACED_COMPLETELY.texts()) {
            if (RegexUtils.containsRegex(text, s.replace(PARAM_INDICATOR, QUANTITY_REGEX))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取需要加入下拉框、供用户选择的批注集合, 旨在减少用户的输入错误几率从而降低程序的判定错误几率
     */
    public static List<String> getDropdownOptions(Language lang) {
        List<String> list = new ArrayList<>();
        for (Remark remark : Remark.values()) {
            if (!remark.inDropdown) {
                continue;
            }
            if (lang == Language.EN_US) {
                list.add(remark.english[remark.english.length - 1]);
            } else {
                list.add(remark.chinese[remark.chinese.length - 1]);
            }
        }
        list.addAll(Arrays.asList(GRAY_REMARKS));
        return list;
    }

    /**
     * 通过文本分析获取直寄国家
     *
     * @param text 订单批注文本
     */
    public static Country getDirectShipFromCountry(String text) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("Remark cannot be empty");
        }

        if (Remark.FULFILL_FROM_US.isContainedBy(text)) {
            return Country.US;
        }
        if (Remark.FULFILL_FROM_CA.isContainedBy(text)) {
            return Country.CA;
        }
        if (Remark.FULFILL_FROM_UK.isContainedBy(text)) {
            return Country.UK;
        }
        if (Remark.FULFILL_FROM_FR.isContainedBy(text)) {
            return Country.FR;
        }
        if (Remark.FULFILL_FROM_DE.isContainedBy(text)) {
            return Country.DE;
        }
        if (Remark.FULFILL_FROM_IT.isContainedBy(text)) {
            return Country.IT;
        }
        if (Remark.FULFILL_FROM_ES.isContainedBy(text)) {
            return Country.ES;
        }

        throw new IllegalArgumentException("Failed to get fulfill country via analyzing text: " + text);
    }


    /**
     * 通过订单批注判定该条订单是否需要切换到其他国家直寄做单, DE Shipment, FR Shipment...
     *
     * @param text 订单批注文本
     */
    public static boolean isDirectShip(String text) {
        try {
            getDirectShipFromCountry(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * <pre>
     * 判定订单批注是否包含买回转运标识
     * <strong>如果批注中包含了直寄，则先不视为买回转运</strong>
     * </pre>
     *
     * @param text 订单批注
     */
    public static boolean purchaseBack(String text) {
        return matchAny(text, Remark.PURCHASE_BACK, Remark.PURCHASE_BACK_EX) && !Remark.isDirectShip(text);
    }

    public static boolean ukFwd(String text) {
        return matchAny(text, Remark.UK_TRANSFER);
    }

    /**
     * 根据给定的国家获取对应的直寄批注文本
     *
     * @param country 给定国家
     */
    public static String getFulfillText(Country country) {
        switch (country) {
            case US:
                return Remark.FULFILL_FROM_US.text2Write();
            case CA:
                return Remark.FULFILL_FROM_CA.text2Write();
            case UK:
                return Remark.FULFILL_FROM_UK.text2Write();
            case FR:
                return Remark.FULFILL_FROM_FR.text2Write();
            case DE:
                return Remark.FULFILL_FROM_DE.text2Write();
            case IT:
                return Remark.FULFILL_FROM_IT.text2Write();
            case ES:
                return Remark.FULFILL_FROM_ES.text2Write();
            default:
                throw new IllegalArgumentException("Currently it is not supported to fulfill order in Amazon" + country.name());
        }
    }

    /**
     * 判定批注中是否包含快递信息，注意无效的信息需要排除掉，比如"Seller无快递并不标识要用快递做"
     *
     * @param remark 订单批注
     */
    public static boolean fastShipping(String remark) {
        String converted = Remark.NO_FAST_SHIPPING.removeFrom(remark);
        return matchAny(converted, Remark.FAST_SHIPPING, Remark.PURCHASE_BACK_EX);
    }

    /**
     * 判定批注中是否包含用普递做的信息
     *
     * @param remark 订单批注
     */
    public static boolean stdShipping(String remark) {
        return Remark.STANDARD_SHIPPING.isContainedBy(remark);
    }

    /**
     * 与标灰相关的Remark
     */
    private static final String[] GRAY_REMARKS = {"ng", "hp", "ph", "kp", "wc", "lw", "eq", "rc", "src", "drc", "bc", "cancel", "lw"};

    public static boolean isGrey(String remark) {
        return isGrey(remark, Language.current());
    }

    /**
     * <pre>
     * 判定订单对应的remark是否表明该行订单为灰条
     * 英文文本重叠的几率较高，需要通过精确匹配而非包含来判定
     * 汉语文本重叠几率较低，只需通过包含判定即可
     * </pre>
     *
     * @param remark 订单批注
     */
    public static boolean isGrey(String remark, Language lang) {
        if (StringUtils.isBlank(remark)) {
            return false;
        }
        // 如果当前语种不是英语，包含即可
        if (lang != Language.EN_US) {
            return Strings.containsAnyIgnoreCase(remark, GRAY_REMARKS);
        }
        String converted = StringUtils.defaultString(remark).replaceAll(Regex.NON_ALPHA_LETTERS.val(), StringUtils.EMPTY).toLowerCase();
        return ArrayUtils.contains(GRAY_REMARKS, converted);
    }

    public static boolean isDN(String remark) {
        return !StringUtils.isBlank(remark) && remark.contains("dn");

    }

    public static void main(String[] args) {
        for (Country country : Country.values()) {
            if (country.ordinal() >= Country.JP.ordinal()) {
                continue;
            }
            String c = country.name();
            System.out.println("/** " + country.label() + "直寄 */");
            System.out.println(String.format("FULFILL_FROM_%s(new String[] {\"%s做单\", \"%s直寄\"}, " +
                    "new String[] {\"%s Shipment\"}, true),", c, c, c, c));
        }

        System.out.println("var REMARKS_EN_US = [");
        for (String s : Remark.getDropdownOptions(Language.EN_US)) {
            System.out.println(String.format("'%s',", s));
        }
        System.out.println("]");
        System.out.println("var REMARKS_ZH_CN = [");
        for (String s : Remark.getDropdownOptions(Language.ZH_CN)) {
            System.out.println(String.format("'%s',", s));
        }
        System.out.println("];");

        System.out.println("代码\t中文特征词\t英文特征词\t出现于下拉框");
        for (Remark r : Remark.values()) {
            System.out.println(r.name() + "\t" + StringUtils.join(r.chinese, ",") + "\t" +
                    StringUtils.join(r.english, ",") + "\t" + (r.inDropdown ? "是" : "否"));
        }
        for (String s : Remark.GRAY_REMARKS) {
            System.out.println(s + "\t" + s + "\t" + s + "\t" + "是");
        }

        int index = 1;
        String format = "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>";
        for (Remark r : Remark.values()) {
            if (r.inDropdown) {
                System.out.println(String.format(format, index++,
                        StringUtils.join(r.chinese, "<br>"), StringUtils.join(r.english, "<br>"), "Yes"));
            }
        }
        for (String s : Remark.GRAY_REMARKS) {
            System.out.println(String.format(format, index++, s, s, "Yes"));
        }
        for (Remark r : Remark.values()) {
            if (!r.inDropdown) {
                System.out.println(String.format(format, index++,
                        StringUtils.join(r.chinese, "<br>"), StringUtils.join(r.english, "<br>"), "No"));
            }
        }
    }
}
