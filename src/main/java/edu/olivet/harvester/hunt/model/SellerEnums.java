package edu.olivet.harvester.hunt.model;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:26 AM
 */
public class SellerEnums {

    /**
     * Seller参考信息中的Seller状态
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Dec 27, 2014 9:50:33 AM
     */
    public enum SellerStatus {
        /**
         * 必选Seller，附加普递可以当做快递属性
         */
        ShallBeChosenAndFastShipping(1024, "label.seller.best.asfastshipping"),
        /**
         * 必选Seller
         */
        ShallBeChosen(512, "label.seller.best"),

        /**
         * 不在必选Seller库中的可选Seller，按照找单流程要求通过审核
         */
        CanBeChosen(256, "label.seller.good"),
        /**
         * 普递可以当做快递
         */
        AsFastShipping(128, "label.seller.asfastshipping"),
        /**
         * 无快递的Seller
         */
        NoFastShipping(0, "label.seller.nofastshipping"),
        /**
         * 绝对不能选的Seller
         */
        ShallNotBeChosen(-64, "label.seller.forbidden");

        /**
         * 某一种Seller状态对应的得分
         */
        private int score;
        /**
         * 某一种Seller状态对应描述
         */
        private String desc;

        public int score() {
            return score;
        }

        public String desc() {
            return UIText.label(this.desc);
        }

        SellerStatus(int score, String desc) {
            this.score = score;
            this.desc = desc;
        }

        public static boolean fastShipping(SellerStatus status) {
            return status == AsFastShipping || status == ShallBeChosenAndFastShipping;
        }

        public static boolean shallBeChosen(SellerStatus status) {
            return status == ShallBeChosen || status == ShallBeChosenAndFastShipping;
        }
    }

    /**
     * Seller类型枚举，包括Amazon和Amazon以外的Seller类型
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 15, 2014 12:40:03 PM
     */
    public enum SellerType {
        AP("AP"),
        Prime("Pr"),
        Pt("pt"),
        APWareHouse("Wr");

        private String abbrev;

        public String abbrev() {
            return abbrev;
        }

        SellerType(String abbrev) {
            this.abbrev = abbrev;
        }

        /**
         * 判定当前Seller类型是否为Prime
         */
        public boolean isPrime() {
            return SellerType.isPrime(this);
        }

        /**
         * 判定给出的Seller类型是否为Prime
         *
         * @param type Seller类型
         */
        public static boolean isPrime(SellerType type) {
            return type == AP || type == Prime || type == APWareHouse;
        }

        /**
         * 判定当前Seller类型是否为普通Seller
         */
        public boolean isPt() {
            return this == Pt;
        }

        public boolean isAP() {
            return this == AP;
        }

        /**
         * 获取目前所有seller类型的缩写连接字符串结果
         */
        static String abbrevs() {
            List<String> list = new ArrayList<>(SellerType.values().length);
            for (SellerType sellerType : SellerType.values()) {
                list.add(sellerType.abbrev);
            }
            return StringUtils.join(list, Constants.COMMA);
        }

        /**
         * 通过character获取对应的seller类型，如果不在指定的范围内，抛出{@link IllegalArgumentException}
         *
         * @param character 当前seller的character
         */
        public static SellerType getByCharacter(String character) {
            for (SellerType sellerType : SellerType.values()) {
                if (sellerType.abbrev().equalsIgnoreCase(character)) {
                    return sellerType;
                }
            }
            throw new IllegalArgumentException(UIText.message("error.character.illegal", character, abbrevs()));
        }
    }


    public enum SellerFullType {
        APDirect(SellerType.AP, true),
        APExport(SellerType.AP, false),
        PrimeDirect(SellerType.Prime, true),
        PrimeExport(SellerType.Prime, false),
        PtDirect(SellerType.Pt, true),
        PtExport(SellerType.Pt, false);

        private SellerType type;
        private boolean directShip;

        SellerFullType(SellerType type, boolean directShip) {
            this.type = type;
            this.directShip = directShip;
        }

        public static SellerFullType fromType(SellerType sellerType, boolean directShip) {
            for (SellerFullType type : values()) {
                if (sellerType == type.type && directShip == type.directShip) {
                    return type;
                }
            }

            throw new BusinessException("No seller type found for " + sellerType + " " + (directShip ? "direct" : "export"));
        }

    }

    /**
     * 库存状态，包括有货、无货、短期马上补货、长期才能补货
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 15, 2014 12:42:11 PM
     */
    public enum StockStatus {
        /**
         * 库存充足
         */
        InStock,
        /**
         * 已经无货
         * Temporarily out of stock. Order now and we'll deliver when available.
         */
        OutOfStock,
        /**
         * 已经无货但短期马上补货，也可接受
         * Back-ordered. Due in stock January 27 -- order now to reserve yours
         */
        WillBeInStockSoon,
        /**
         * 已经无货且短期不能补货，不可接受
         */
        WontBeInStockSoon;

        final static String[] outOfStockKeywords = new String[] {"out of stock"};
        final static String[] backOrderKeywords = new String[] {"Back-ordered"};

        public static StockStatus parseFromText(String deliveryText) {
            if (Strings.containsAnyIgnoreCase(deliveryText, outOfStockKeywords)) {
                return OutOfStock;
            }

            if (Strings.containsAllIgnoreCase(deliveryText, backOrderKeywords)) {
                return WillBeInStockSoon;
            }

            return InStock;
        }


    }


}
