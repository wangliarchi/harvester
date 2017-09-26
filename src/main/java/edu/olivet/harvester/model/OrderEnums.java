package edu.olivet.harvester.model;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单相关枚举
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
public class OrderEnums {
    /**
     * google sheet中一行订单背景色定义
     * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
     */
    public enum OrderColor {
        /**
         * 灰条：比如seller消失，寄不到等程序无法处理的情况。此为程序标上的颜色，一般而言为黄褐色
         */
        InvalidByCode("#df7401"),
        /**
         * 人工标上的灰条，这是真的灰条，一般而言真的是灰色，可能为以下4种颜色之一:#999999, #b7b7b7, #cccccc, #d9d9d9
         */
        InvalidByHuman("#d9d9d9"),
        DarkGray3("#666666"),
        DarkGray2("#999999"),
        DarkGray1("#b7b7b7"),
        Gray("#cccccc"),
        LightGray1("#d9d9d9"),
        /**
         * 成功：一条订单成功提交完毕，数据写入google sheet之后。背景色一般为普通白色
         */
        Finished("#ffffff"),
        /**
         * 写入Seller，当程序找到可用Seller，数据写入google sheet之后，标上指定的颜色方便找单同事知晓
         */
        CommitSeller("#cfe2f3"),
        /**
         * 高优先级，需优先提交的订单，颜色一般为绿色
         */
        HighPriority("#00ff00"),
        /**
         * 操作失败，一般为红色
         */
        Fail("#ff0000");

        private final String code;

        OrderColor(String code) {
            this.code = code;
        }

        /**
         * 返回背景色枚举对应的颜色代码
         */
        public String code() {
            return this.code;
        }

        /**
         * 判定某一颜色代码是否为标灰色
         * @param colorCode 颜色代码，一般为订单所在行的背景色
         */
        public static boolean isGray(String colorCode) {
            return DarkGray3.code().equalsIgnoreCase(colorCode) ||
                DarkGray2.code().equalsIgnoreCase(colorCode) ||
                DarkGray1.code().equalsIgnoreCase(colorCode) ||
                Gray.code().equalsIgnoreCase(colorCode) ||
                LightGray1.code().equalsIgnoreCase(colorCode);
        }
    }

    /**
     * 订单各列枚举，基于Google Sheet各列转换而成
     * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
     */
    public enum OrderColumn {
        STATUS(1),
        ORDER_ID(2),
        RECIPIENT_NAME(3),
        PURCHASE_DATE(4),
        SKU_ADDRESS(5),
        SKU(6),
        PRICE(7),
        QUANTITY_PURCHASED(8),
        SHIPPING_FEE(9),
        SHIP_STATE(10),
        ISBN_ADDRESS(11),
        ISBN(12),
        SELLER(13),
        SELLER_ID(14),
        SELLER_PRICE(15),
        URL(16),
        CONDITION(17),
        CHARACTER(18),
        REMARK(19),
        REFERENCE(20),
        CODE(21),
        PROFIT(22),
        ITEM_NAME(23),
        SHIP_ADDRESS_1(24),
        SHIP_ADDRESS_2(25),
        SHIP_CITY(26),
        SHIP_ZIP(27),
        SHIP_PHONE_NUMBER(28),
        COST(29),
        ORDER_NUMBER(30),
        ACCOUNT(31),
        LAST_CODE(32),
        SHIP_COUNTRY(33),
        SID(34),
        SALES_CHANEL(35),
        ORIGINAL_CONDITION(36),
        SHIPPING_SERVICE(37),
        EXPECTED_SHIP_DATE(38),
        ESTIMATED_DELIVERY_DATE(39),
        TRACKING_NUMBER(43);

        private final int number;

        /**
         * 获取对应的列号
         */
        public int number() {
            return number;
        }

        public int index() {
            return number - 1;
        }

        OrderColumn(int number) {
            this.number = number;
        }

        public static @Nullable OrderColumn get(int number) {
            for (OrderColumn orderColumn : OrderColumn.values()) {
                if (orderColumn.number == number) {
                    return orderColumn;
                }
            }
            return null;
        }
    }

    /**
     * 订单状态枚举
     * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
     */
    public enum Status {
        /** 已成功提交 */
        Finish("finish"),
        /** Seller为Prime */
        PrimeSeller("p"),
        /** 普通Seller */
        CommonSeller("a"),
        /** 当前Seller为BetterWorld，做单时需切换到该网站上面 */
        SellerIsBetterWorld("bw"),
        /** 当前Seller为Half，做单时需切换到该网站上面 */
        SellerIsHalf("h"),
        /** 当前Seller为Ingram，做单时需切换到该网站上面 */
        SellerIsIngram("in"),
        /** 国际单 */
        International("gj"),
        /** 产品，Prime买回转运 */
        PrimeBuyAndTransfer("pm"),
        /** 买回转运 */
        BuyAndTransfer("m"),
        /** 初始、缺陷等状态，尚未开始做单或已是灰条 */
        Initial("n"),
        /** 找单同事标注f，表明该条需要略过 */
        Skip("f"),

        /** 找单部定义的各种发送例信代码 */
        NewToGood("ng"),
        DamagedNewToGood("dng"),
        HardCoverToPaperback("hp"),
        PaperbackToHardCover("ph"),
        WaitCancel("wc"),
        LetWait("lw"),
        Electronic("el"),
        ShippedRefundCancellation("src"),
        DamagedRefundCancellation("drc"),
        Refund("rc"),
        Cancel("c");

        private final String value;

        public String value() {
            return value;
        }

        Status(String value) {
            this.value = value;
        }

        public static Status get(String value) {
            List<String> statuses = new ArrayList<>(Status.values().length);
            Status result = null;

            for (Status status : Status.values()) {
                statuses.add(status.value());
                if (status.value().equalsIgnoreCase(value)) {
                    result = status;
                }
            }

            if (result != null) {
                return result;
            }

            String currentStatus = StringUtils.defaultString(value);
            String validStatuses = StringUtils.join(statuses, ",");
            throw new IllegalArgumentException(String.format("非法订单状态:%s，程序只接受以下状态:%s", currentStatus, validStatuses));
        }
    }

    /**
     * Seller类型枚举，包括Amazon和Amazon以外的Seller类型
     * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
     */
    public enum SellerType {
        AP("AP"),
        ImagePrime("tp_Pr"),
        Prime("Pr"),
        APWareHouse("Wr"),
        ImagePt("tp_pt"),
        Pt("pt"),
        BetterWorld("bw"),
        Half("h"),
        Ingram("in");

        private final String abbrev;

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
         * @param type Seller类型
         */
        public static boolean isPrime(SellerType type) {
            return type == AP || type == Prime || type == ImagePrime || type == APWareHouse;
        }

        /**
         * 判定当前Seller类型是否为普通Seller
         */
        public boolean isPt() {
            return this == Pt || this == ImagePt;
        }
    }


    public enum OrderItemType {
        BOOK,
        PRODUCT
    }
}