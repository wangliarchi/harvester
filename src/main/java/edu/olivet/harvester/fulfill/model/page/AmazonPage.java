package edu.olivet.harvester.fulfill.model.page;

import edu.olivet.foundations.ui.UIText;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 11:07 AM
 */
public enum AmazonPage {
    //---------------------------亚马逊买家前台----------------------------//
    Logout("sign-out"),
    OfferingList("gp/offer-listing"),
    Checkout("gp/huc"),
    EditShoppingCart("gp/cart"),
    ShoppingCart("gp/cart/view.html"),
    Login("ap/signin"),
    EditQuantity("/gp/ordering/checkout/item/select.html/ref=ox_spc_change_quantity"),
    EnterShippingAddress("/gp/buy/addressselect/handlers/display.html"),
    EnterShippingAddressPop("buy/spc/handlers"),
    Recommendation("buy/shipaddressselect"),
    CannotShipTo("buy/shipaddressselect"),
    ItemSelect("buy/itemselect"),
    EditShipment("/gp/orders-v2/edit-shipment"),
    AddressValidation("buy/addressvalidation"),
    ConfirmShippingAddress("buy/addressselect"),
    GiftOption("buy/gift"),
    SelectShippingOption("buy/shipoptionselect"),
    SelectPayment("/gp/buy/payselect/handlers/display.html"),
    OrderReview("buy/spc"),
    PlaceOrder("buy/spc"),
    Advertise("primeinterstitial"),
    ForcePlaceOrder("buy/duplicate-order"),
    OrderNumber("buy/thankyou"),
    OrderList("gp/css/order-history"),
    /**
     * 在亚马逊第一次采购Prime Seller货品时，会在支付页面之后弹出是否升级为Prime买家的提示
     */
    PrimeSuggestion("gp/buy/primeinterstitial"),
    AddtoCart("/gp/product/handle-buy-box/ref=dp_start-bbf_1_glance"),

    //---------------------------买家前台客服相关----------------------------//
    ContactSeller("/gp/help/contact-seller/contact-seller.html"),
    LeaveSellerFeedback("/gp/feedback/leave-customer-feedback.html"),
    ReturnOrReplaceItems("/gp/orc/returns/items/select/ref=od_aui_return_items"),
    ViewReturnLabelAndInstructions("/returns/label/"),
    ViewReturnRefundStatus("/gp/orc/returns/items/select/ref=od_aui_return_status"),
    ReturnReason("/returns/order"),
    SubmitReturnRequest("/returns/resolution"),
    ReturnMethod("/returns/method/"),
    ReturnSummary("/returns/confirmation"),

    //---------------------------亚马逊卖家后台----------------------------//
    SellerEmailConfirm("/gp/orders-v2/list"),
    ShipmentConfirm("/gp/orders-v2/confirm-shipment"),
    SellerRefund("/gp/orders-v2/refund"),
    SellerCancel("/gp/orders-v2/cancel"),
    SellerRating("/gp/seller-rating/pages/performance-summary.html/ref=ag_srsumprf_anav_srnavbar"),
    SellerHealthSummary("/gp/customer-experience/summary.html/ref=sm_custmetric_cont_cxperftime"),
    SellerPaymentReport("/gp/payments-account/settlement-summary.html/ref=sm_payments_dnav_home_"),
    SellerInventoryHome("/hz/inventory/ref=sm_invmgr_dnav_xx_"),
    SellerPerformanceNotification("/gp/customer-experience/perf-notifications.html/ref=ag_cxperform_dnav_srsumprf_"),
    SellerShippingSettings("/gp/shipping/dispatch.html/ref=ag_shipset_dnav_home_"),
    OrderStatistic("/gp/homepage/orders-widget-internals.html?t=yo");


    /**
     * URL特征标识，可以据此判定当前页面是否处于预期页面
     */
    private String urlMark;

    AmazonPage(String urlMark) {
        this.urlMark = urlMark;
    }

    public String urlMark() {
        return urlMark;
    }

    public String desc() {
        return UIText.label("label.amazonpage." + this.name().toLowerCase());
    }

    @Override
    public String toString() {
        return this.desc();
    }
}

