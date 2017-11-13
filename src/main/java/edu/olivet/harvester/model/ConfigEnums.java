package edu.olivet.harvester.model;

import edu.olivet.foundations.ui.UIText;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/7/17 11:09 AM
 */
public class ConfigEnums {
    /**
     * 物品类型: 书类、CD或产品
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Aug 25, 2015 2:56:55 PM
     */
    public enum ItemType {
        BookCD,
        Product
    }
    /**
     * Dropshipping业务类型，比如书类/CD、产品以及二者混合
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Aug 24, 2015 2:24:21 PM
     */
    public enum BusinessType {
        Book,
        Product,
        BookAndProduct;

        @Override
        public String toString() {
            return UIText.label("label.dropshipping.type." + this.name().toLowerCase());
        }
    }

    /**
     * 日期区间枚举定义
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Aug 31, 2015 1:43:56 PM
     */
    public enum DateRange {
        Daily(1),
        Weekly(7),
        Monthly(30),
        Quarterly(90),
        HalfYear(180),
        Yearly(365);

        private int value;
        public int value() {
            return this.value;
        }
        DateRange(int range) {
            this.value = range;
        }
    }


    /**
     * 数量枚举定义
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Dec 31, 2014 1:19:33 PM
     */
    public enum Quantity {
        One(1),
        Two(2),
        Three(3),
        Four(4),
        Five(5),
        Six(6),
        Seven(7),
        Eight(8),
        Nine(9),
        Ten(10);

        private int value;

        Quantity(int value) {
            this.value = value;
        }

        public String toString() {
            return UIText.label("label.quantity." + this.name().toLowerCase());
        }
        public int value() {
            return this.value;
        }

        public static Quantity byValue(int value) {
            for (Quantity quantity : Quantity.values()) {
                if (quantity.value() == value) {
                    return quantity;
                }
            }

            throw new IllegalArgumentException("Illegal Quantity Value: " + value);
        }
    }

    /**
     * 自动重复次数枚举
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 17, 2014 5:52:27 PM
     */
    public enum Times {
        One(1),
        Two(2),
        Three(3),
        Four(4),
        Five(5);

        private int value;
        Times(int value) {
            this.value = value;
        }
        public String toString() {
            return UIText.label("label.times." + this.name().toLowerCase());
        }
        public int value() {
            return this.value;
        }
    }


    /**
     * 做单范围枚举
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 19, 2014 11:49:29 AM
     */
    public enum SubmitRange {
        ALL("label.range.all", "label.range.all"),
        LimitCount("label.range.limitcount", "tooltip.range.limitcount"),
        SINGLE("label.range.singlerow", "tooltip.range.singlerow"),
        SCOPE("label.range.scope", "tooltip.range.scope"),
        MULTIPLE("label.range.multirows", "tooltip.range.multirows");

        private String format;
        private String desc;

        SubmitRange(String format, String desc) {
            this.format = format;
            this.desc = desc;
        }

        public String format(Object...params) {
            return UIText.label(format, params);
        }
        public String desc() {
            return UIText.tooltip(desc);
        }
    }

    /**
     * 跳过检查类型枚举
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 17, 2014 1:10:35 PM
     */
    public enum SkipValidation {
        None("label.skip.none"),
        ItemName("label.skip.itemname"),
        ForbiddenSupplier("label.skip.forbidden.supplier"),
        SellerOutOfStock("label.skip.stock"),
        UrlNotMatch("label.skip.url"),
        SellerPrice("label.skip.price"),
        GiftOption("label.skip.giftoption"),
        Profit("label.skip.profit"),
        ShippingFee("label.skip.shippingfee"),
        Address("label.skip.address"),
        RefundMultiCheck("label.skip.multirefund"),
        OperationSuccessCheck("label.skip.opersuccess"),
        All("label.skip.all");

        private String label;
        SkipValidation(String label) {
            this.label = label;
        }
        @Override
        public String toString() {
            return UIText.label(this.label);
        }
    }


}
