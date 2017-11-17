package edu.olivet.harvester.model;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Directory;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/7/17 11:09 AM
 */
public class ConfigEnums {
    /**
     * 物品类型: 书类、CD或产品
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Aug 25, 2015 2:56:55 PM
     */
    public enum ItemType {
        BookCD,
        Product
    }

    /**
     * Dropshipping业务类型，比如书类/CD、产品以及二者混合
     *
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
     *
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
     *
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
     *
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
     *
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

        public String format(Object... params) {
            return UIText.label(format, params);
        }

        public String desc() {
            return UIText.tooltip(desc);
        }
    }

    public static final long ZERO_BYTES = 0L;

    /**
     * 当前系统日志类型枚举
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Nov 17, 2014 11:32:55 AM
     */
    public enum Log {
        Error("error.log"),
        OrderMan("orderman.%s.log"),
        Success("success.log"),
        FailReason("failreason.%s.log"),
        Profile("profile.%s.log"),
        Hunt("hunter.%s.log"),
        Transfer("transfer.%s.csv"),
        ISBN("isbn.txt"),
        ASINDeletion("asin.%s.log"),
        ASINHistory("asinhistory.txt"),
        InventoryLoader("inv.%s.log"),
        Statistic("statistic.log"),
        Service("service.%s.log"),
        Deploy("deploy.log"),
        MailMan("mailman.%s.log"),
        ReturnRequestFollowUp("returnreq.%s.log"),
        Infringements("infringements.txt");

        private String fileName;

        Log(String fileName) {
            this.fileName = fileName;
        }

        public String fileName() {
            return this.fileName;
        }

        public File file() {
            return new File(this.filePath());
        }

        public boolean valid() {
            File log = this.file();
            return log.exists() && log.length() > ZERO_BYTES;
        }

        /**
         * 获取当前(当天)日志文件所在路径，按照yyyy-M-d的日期约定替换文件名中占位符
         */
        public String filePath() {
            String path = Directory.Log.path() + File.separator + this.fileName;
            return path.replace("%s", Dates.nowAsFileName());
        }

        public String desc() {
            return UIText.label("label.log." + this.name().toLowerCase());
        }
    }


}
