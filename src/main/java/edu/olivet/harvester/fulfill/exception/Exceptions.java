package edu.olivet.harvester.fulfill.exception;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/1/17 2:21 PM
 */
public class Exceptions {

    public static class OrderSubmissionException extends RuntimeException {
        public OrderSubmissionException(String errorMsg) {
            super(errorMsg);
        }

        public OrderSubmissionException(Throwable cause) {
            super(cause);
        }
    }

    public static class ClearCardAndTryAgainException extends RuntimeException {

        public ClearCardAndTryAgainException(String errorMsg) {
            super(errorMsg);
        }

        public ClearCardAndTryAgainException(Throwable cause) {
            super(cause);
        }
    }

    public static class BuyerAccountAuthenticationException extends OrderSubmissionException {

        public BuyerAccountAuthenticationException(String errorMsg) {
            super(errorMsg);
        }

        public BuyerAccountAuthenticationException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Supplier为禁选Seller异常定义
     */
    public static class ForbiddenSupplierUsedException extends RuntimeException {
        private static final long serialVersionUID = -7011281487392665327L;

        public ForbiddenSupplierUsedException(String errorMsg) {
            super(errorMsg);
        }
    }

    /**
     * 被亚马逊怀疑访问行为构成机器人抓取时抛出此异常
     */
    public static class RobotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1463638929748513302L;

        public RobotFoundException(String errorMsg) {
            super(errorMsg);
        }
    }

    /**
     * 地址无法寄送到异常定义
     */
    public static class CannotShipToException extends OrderSubmissionException {
        private static final long serialVersionUID = 5731139518478534873L;

        public CannotShipToException(String message) {
            super(message);
        }
    }

    /**
     * Seller可以找到，但是condition下降无法接受，比如从Used - Good变为Used - Acceptable
     */
    public static class LowerConditionException extends RuntimeException {
        private static final long serialVersionUID = 1402777696349943824L;

        public LowerConditionException(String message) {
            super(message);
        }
    }

    /**
     * 产品已经下架，不再可用，Seller列表、产品信息页面会直接返回404代码
     */
    public static class ItemNotAvailableException extends RuntimeException {
        private static final long serialVersionUID = 1314195920517735599L;

        public ItemNotAvailableException(String message) {
            super(message);
        }
    }

    /**
     * 访问Offer列表页面时出现服务器端错误
     */
    public static class ServerFailException extends RuntimeException {
        private static final long serialVersionUID = -8671119701737650739L;

        public ServerFailException(String message) {
            super(message);
        }
    }

    /**
     * 提交结果未知异常，发生场景：在Place your order发生异常，但是订单有可能已经真实提交了。为安全起见先将其标灰。
     */
    public static class SubmitUnknownException extends RuntimeException {
        private static final long serialVersionUID = 158762956501444267L;

        public SubmitUnknownException(String errorMsg) {
            super(errorMsg);
        }
    }

    /**
     * 当前订单Seller标注了快递寄送，但在实际做单时没有快递选项时抛出此异常
     */
    public static class NoExpeditedShippingException extends RuntimeException {
        private static final long serialVersionUID = 2501558170082003722L;

        public NoExpeditedShippingException(String message) {
            super(message);
        }
    }

    /**
     * Prime Seller但是没有Gift Option，且差价过高导致客户不能接受，此时抛出异常，停止该单
     */
    public static class NoGiftOptionException extends RuntimeException {
        private static final long serialVersionUID = -5357434926881683188L;

        public NoGiftOptionException(String errorMsg) {
            super(errorMsg);
        }
    }

    /**
     * gift card out of balance异常定义
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Oct 23, 2014 11:26:02 AM
     */
    public static class OutOfBalanceException extends RuntimeException {
        private static final long serialVersionUID = 2202090768559109865L;

    }

    /**
     * gift card out of balance异常定义
     */
    public static class OutOfBudgetException extends OrderSubmissionException {
        private static final long serialVersionUID = 548234872567663633L;

        public OutOfBudgetException(String message) {
            super(message);
        }
    }

    /**
     * gift card out of balance异常定义
     */
    public static class NoBudgetException extends OrderSubmissionException {
        private static final long serialVersionUID = 548234872567663633L;

        public NoBudgetException(String message) {
            super(message);
        }

        public NoBudgetException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Seller out of stock异常定义
     */
    public static class OutOfStockException extends RuntimeException {
        private static final long serialVersionUID = 5925565724007149981L;

        public OutOfStockException(String message) {
            super(message);
        }
    }


    /**
     * 价格过高异常，场景：寻找Seller时，找到时或找到之前价格已经过高，超出可以接受范围
     */
    public static class PriceTooHighException extends RuntimeException {
        private static final long serialVersionUID = 7066003043572867801L;

        public PriceTooHighException(String message) {
            super(message);
        }
    }

    public static class OrderFulfilledException extends OrderSubmissionException {
        private static final long serialVersionUID = 4834678253258681291L;

        public OrderFulfilledException(String message) {
            super(message);
        }
    }

    /**
     * Seller无法找到异常定义
     */
    public static class SellerNotFoundException extends OrderSubmissionException {
        private static final long serialVersionUID = 4834678253258681291L;

        public SellerNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Seller无法找到异常定义
     */
    public static class SellerEddTooLongException extends OrderSubmissionException {
        private static final long serialVersionUID = 4834678253258681291L;

        public SellerEddTooLongException(String message) {
            super(message);
        }
    }

    /**
     * Seller已经找到，但是价格涨得太厉害，超过可以接受上限
     */
    public static class SellerPriceRiseTooHighException extends OrderSubmissionException {
        private static final long serialVersionUID = -79978374204763171L;

        public SellerPriceRiseTooHighException(String message) {
            super(message);
        }
    }

    /**
     * 运费过高异常定义
     **/
    public static class ShippingFeeTooHighException extends RuntimeException {
        private static final long serialVersionUID = -4200500051507833411L;

        public ShippingFeeTooHighException(String message) {
            super(message);
        }
    }

    /**
     * 当前产品为AddOn，做单时需要略过
     **/
    public static class SkipAddOnException extends RuntimeException {
        private static final long serialVersionUID = 7688227399743757062L;

        public SkipAddOnException(String errorMsg) {
            super(errorMsg);
        }
    }

    /**
     * Seller暂时无货异常定义，此异常一般在Seller列表页面进行网页内容提取分析时抛出
     */
    public static class TemporarilyOutOfStockException extends RuntimeException {
        private static final long serialVersionUID = 5925565724007149981L;

        public TemporarilyOutOfStockException(String message) {
            super(message);
        }
    }


}
