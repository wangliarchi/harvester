package edu.olivet.harvester.fulfill.utils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mchange.lang.FloatUtils;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.OrderFulfillmentRecord;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.model.Seller;
import edu.olivet.harvester.fulfill.service.ForbiddenSeller;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.model.Remark;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 2:56 PM
 */
public class OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderValidator.class);

    public enum Validator {
        IsFulfilled,
        IsNotFulfilled,
        IsSupplierHunted,
        IsUKForward,
        IsNotUKForward,
        IsUSForward,
        IsNotUSForward,
        AddressInfoValid,
        ValidZipCode,
        ItemInfoValid,
        ConditionValid,
        StatusIsInitial,
        StatusMarkedCorrectForSubmit,
        NotSelfOrder,
        NotGrayOrder,
        HasValidBuyerAccount,
        FulfillmentCountryIsValid,
        HasValidCreditCard,
        HasEnoughBudgetToFulfill,
        NotDuplicatedOrder,
        IsNotForbiddenSeller

    }


    /**
     * 跳过检查类型枚举
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
        //ShippingFee("label.skip.shippingfee"),
        Address("label.skip.address"),
        //RefundMultiCheck("label.skip.multirefund"),
        //OperationSuccessCheck("label.skip.opersuccess"),
        EDD("Skip EDD Check"),
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

    public static boolean skipCheck(SkipValidation skipValidation) {
        RuntimeSettings settings = RuntimeSettings.load();
        return settings.getSkipValidation() == skipValidation || settings.getSkipValidation() == SkipValidation.All;
    }

    public static boolean needCheck(SkipValidation skipValidation) {
        return !skipCheck(skipValidation);
    }

    public String isValid(Order order, FulfillmentEnum.Action scenario) {
        switch (scenario) {
            case UpdateStatus:
                return canMarkStatus(order);
            case SubmitOrder:
                return canSubmit(order);
        }

        return null;
    }


    public String canMarkStatus(Order order) {
        String result = basicInfoValidation(order);
        if (StringUtils.isNotBlank(result)) {
            return result;
        }

        return validWithValidators(order,
                Validator.NotGrayOrder,
                Validator.NotSelfOrder,
                Validator.IsNotUKForward,
                Validator.StatusIsInitial,
                Validator.IsSupplierHunted

        );

    }

    public String canSubmit(Order order) {
        String result = basicInfoValidation(order);
        if (StringUtils.isNotBlank(result)) {
            return result;
        }

        return validWithValidators(order,
                Validator.IsNotFulfilled,
                Validator.NotDuplicatedOrder,
                Validator.NotGrayOrder,
                Validator.NotSelfOrder,
                Validator.IsNotUKForward,
                Validator.IsSupplierHunted,
                Validator.StatusMarkedCorrectForSubmit,
                Validator.HasValidBuyerAccount,
                Validator.HasValidCreditCard,
                Validator.FulfillmentCountryIsValid,
                Validator.IsNotForbiddenSeller,
                Validator.HasEnoughBudgetToFulfill

        );

    }

    public String validWithValidators(Order order, Validator... validators) {
        for (Validator validator : validators) {
            Class[] args = new Class[1];
            args[0] = Order.class;
            try {
                String methodName = StringUtils.uncapitalize(validator.name());
                Method method = OrderValidator.class.getMethod(methodName, args);
                final Object result = method.invoke(this, order);
                if (StringUtils.isNotBlank(result.toString())) {
                    return result.toString();
                }
            } catch (NoSuchMethodException e) {
                LOGGER.error("", e);
                return "Validator " + validator.name() + " not found.";
            } catch (IllegalAccessException e) {
                //e.printStackTrace();
                LOGGER.error("", e);
                return "Validator " + validator.name() + " is not accessible.";
            } catch (InvocationTargetException e) {
                LOGGER.error("", e);
                return "Validator " + validator.name() + " is not valid.";
            }
        }

        return "";
    }

    public String validWithValidators(Order order, List<Validator> validators) {
        Validator[] vs = validators.toArray(new Validator[0]);
        return validWithValidators(order, vs);
    }

    public String basicInfoValidation(Order order) {
        List<Validator> validators = Lists.newArrayList(
                Validator.NotSelfOrder,
                Validator.AddressInfoValid, Validator.ValidZipCode, Validator.ItemInfoValid);
        return validWithValidators(order, validators);
    }

    @Inject
    OrderStatusUtils orderStatusUtils;

    public String statusMarkedCorrectForSubmit(Order order) {
        String status = orderStatusUtils.determineStatus(order);
        if (order.status.equals(status)) {
            return "";
        }

        return "Order status is not marked for submision.";
    }

    public String statusIsInitial(Order order) {
        if (OrderEnums.Status.Initial.value().equals(order.status)) {
            return "";
        }

        return "Order status is not " + OrderEnums.Status.Initial.value();

    }

    public String isFulfilled(Order order) {
        boolean fulfilled = StringUtils.containsIgnoreCase(order.status, "fi") &&
                StringUtils.isNotBlank(order.cost) && StringUtils.isNotBlank(order.order_number) && StringUtils.isNotBlank(order.account);

        if (fulfilled) {
            return "";
        }

        return "Order has not been fulfilled yet.";
    }

    public String isNotFulfilled(Order order) {
        String msg = isFulfilled(order);
        if (StringUtils.isBlank(msg)) {
            return "Order has been fulfilled already.";
        }
        return "";
    }

    public String notSelfOrder(Order order) {
        if (order.selfBuy()) {
            return "Order is self order.";
        }

        return "";
    }

    public String notGrayOrder(Order order) {
        if (order.colorIsGray()) {
            return "Order is gray labeled order";
        }
        return "";
    }

    public String isSupplierHunted(Order order) {
        //no seller or seller price info
        String errorMsg = "Order supplier has not been hunted yet.";
        if (StringUtils.isBlank(order.seller) || StringUtils.isBlank(order.seller_price)) {
            return errorMsg;
        }

        //not checked yet
        if (Remark.TO_BE_CHECKED.isContainedBy(order.remark)) {
            return errorMsg;
        }

        return "";
    }

    /**
     * UK FWD order must to meet following 3 conditions
     * 1. remark column contains both “UK” abd “FWD”，case insensitive。
     * 2. remark column dose not contain “直寄”、“已移表” and “CHCK”
     * 3. Order not fulfilled，which means cost，order_number，account columns should be empty，and status column is “n”，
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public String isUKForward(Order order) {
        String errorMsg = "Order is not UK FWD";
        if ("finish".equalsIgnoreCase(order.status)) {
            return errorMsg;
        }

        if (Strings.containsAnyIgnoreCase(order.remark, "Cancel")) {
            return errorMsg;
        }
        if (!Strings.containsAllIgnoreCase(order.remark, "uk", "fwd")) {
            return errorMsg;
        }

        return "";

    }

    public String isNotUKForward(Order order) {
        if (StringUtils.isBlank(isUKForward(order))) {
            return "Order is UK FWD";
        }

        return "";
    }

    public String isUSForward(Order order) {
        String errorMsg = "Order is not US FWD";
        if (Strings.containsAnyIgnoreCase(order.remark, "CHCK", "Cancel")) {
            return errorMsg;
        }

        if (StringUtils.isBlank(isNotFulfilled(order))) {
            return errorMsg;
        }

        if (StringUtils.isBlank(order.remark) || !Strings.containsAnyIgnoreCase(order.remark, "us")) {
            return errorMsg;
        }

        if (!Strings.containsAnyIgnoreCase(order.remark, "fwd", "买回", "转运")) {
            return errorMsg;
        }

        return "";

    }

    /**
     * zipcode 为4位的国家如下：除此之外，中国为6位，英国，加拿大zipcode 为字母形式，其他的国家都保持5位fortmat就可以了。
     * Argentina,
     * Australia,
     * Austria,
     * Belgium,
     * Cyprus,
     * Denmark,
     * Liechtenstein,
     * Luxembourg,
     * Norway,
     * New Zealand,
     * Switzerland
     **/
    public String validZipCode(Order order) {
        if (order.ship_zip.length() == 4 &&

                !order.ship_country.equalsIgnoreCase("Argentina") &&
                !order.ship_country.equalsIgnoreCase("Australia") &&
                !order.ship_country.equalsIgnoreCase("Austria") &&
                !order.ship_country.equalsIgnoreCase("Belgium") &&
                !order.ship_country.equalsIgnoreCase("Cyprus") &&
                !order.ship_country.equalsIgnoreCase("Denmark") &&
                !order.ship_country.equalsIgnoreCase("Liechtenstein") &&
                !order.ship_country.equalsIgnoreCase("Luxembourg") &&
                !order.ship_country.equalsIgnoreCase("Norway") &&
                !order.ship_country.equalsIgnoreCase("New Zealand") &&
                !order.ship_country.equalsIgnoreCase("Switzerland") &&
                !order.ship_country.equalsIgnoreCase("England") &&
                !order.ship_country.equalsIgnoreCase("Canada")) {
            order.ship_zip = "0" + order.ship_zip;
        }

        return "";

    }


    /**
     * 判定当前订单的地址信息是否有效：收件人地址至少需要有一个，目的地国家必须明确声明
     */
    public String addressInfoValid(Order order) {
        boolean result = StringUtils.isNotBlank(order.recipient_name) &&
                (StringUtils.isNotBlank(order.ship_address_1) || StringUtils.isNotBlank(order.ship_address_2)) &&
                StringUtils.isNotBlank(order.ship_country);

        if (!result) {
            return "Order address is not valid, please check.";
        }
        return "";
    }

    /**
     * 当前订单的产品信息是否有效：isbn、名称、购买数量和运费是否均有提供
     */
    public String itemInfoValid(Order order) {
        boolean result = StringUtils.isNotBlank(order.isbn) && StringUtils.isNotBlank(order.item_name) &&
                StringUtils.isNotBlank(order.quantity_purchased) && StringUtils.isNotBlank(order.shipping_fee);

        if (!result) {
            return "Order information is not valid, please check ISBN, item name, quantity purchased and shipping fee columns.";
        }

        return "";
    }

    public String isNotUSForward(Order order) {
        if (StringUtils.isBlank(isUSForward(order))) {
            return "Order is US FWD";
        }
        return "";
    }

    public String hasValidBuyerAccount(Order order) {
        try {
            Account buyer = OrderBuyerUtils.getBuyer(order);
        } catch (Exception e) {
            return "order buyer account not set properly.";
        }
        return "";
    }

    public String fulfillmentCountryIsValid(Order order) {
        try {
            Country country = OrderCountryUtils.getFulfillementCountry(order);
        } catch (Exception e) {
            return "order fulfillment country is not valid.";
        }

        return "";
    }

    public String hasValidCreditCard(Order order) {
        try {
            OrderBuyerUtils.getCreditCard(order);
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    /**
     * 判定当前订单的condition是否有效：不为空，在允许范围内
     */
    public String conditionValid(Order order) {
        if (StringUtils.isNotBlank(order.condition)) {

            String corrected = ConditionUtils.translateCondition(order.condition);
            try {
                ConditionUtils.getConditionLevel(corrected);
                return "";
            } catch (IllegalArgumentException e) {
                //ignore
            }
        }
        return String.format("Order condition %s is not valid.", order.condition);

    }

    @Inject
    DailyBudgetHelper dailyBudgetHelper;

    public String hasEnoughBudgetToFulfill(Order order) {
        try {
            String spreadsheetId = RuntimeSettings.load().getSpreadsheetId();
            dailyBudgetHelper.getRemainingBudget(spreadsheetId, new Date());
            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * <pre>
     *     Check duplication.
     *
     *     identification: order_id + sku + remark
     *
     *     1. if it's exported multiple times
     *     2. if is has fulfillment record in local db
     *
     *     for buyer canceled order, may have same order id + sku + qty, but different remark
     * </pre>
     */
    @Inject
    private DBManager dbManager;

    public String notDuplicatedOrder(Order order) {
        List<OrderFulfillmentRecord> list = dbManager.query(OrderFulfillmentRecord.class,
                Cnd.where("orderId", "=", order.order_id)
                        .and("sku", "=", order.sku)
                        .and("remark", "=", order.remark));
        if (list.size() > 0) {
            OrderFulfillmentRecord record = list.get(0);
            return String.format("Order fulfilled at %s with order number %s by buyer account %s. Please check if this order is a duplicated record. If not, please update remark.",
                    DateFormat.DATE_TIME.format(record.getFulfillDate()), record.getOrderNumber(), record.getBuyerAccount()
            );
        }

        return "";
    }

    @Inject
    ForbiddenSeller forbiddenSeller;

    public String isNotForbiddenSeller(Order order) {
        if (OrderValidator.skipCheck(SkipValidation.ForbiddenSupplier)) {
            return "";
        }

        Seller seller = new Seller();
        seller.setUuid(order.seller_id);
        seller.setOfferListingCountry(OrderCountryUtils.getFulfillementCountry(order));
        seller.setName(order.seller);
        if (forbiddenSeller.isForbidden(seller)) {
            return String.format("Seller %s (%s) is forbidden.", seller.getName(), seller.getUuid());
        }
        return "";
    }


    public static String sellerPriceChangeNotExceedConfiguration(Order order, Seller seller) {
        RuntimeSettings settings = RuntimeSettings.load();
        float maxAllowed = Float.parseFloat(settings.getPriceLimit());
        float priceRaised = seller.getPrice().toUSDAmount().floatValue() - Float.parseFloat(order.seller_price);
        if (maxAllowed < priceRaised) {
            return "Seller price raised " + String.format("%.2f", priceRaised) + " to " + seller.getPrice().usdText();
        }

        return "";
    }
}
