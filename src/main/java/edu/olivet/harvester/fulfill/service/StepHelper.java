package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.service.flowcontrol.FlowState;
import edu.olivet.harvester.fulfill.service.flowcontrol.Step;
import edu.olivet.harvester.fulfill.service.steps.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.olivet.harvester.fulfill.model.page.checkout.CheckoutEnum.CheckoutPage;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 9:48 PM
 */
public class StepHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(StepHelper.class);

    @Inject private
    ReviewOrderChangeShippingAddress reviewOrderChangeShippingAddress;
    @Inject private
    ReviewOrderChangePaymentMethod reviewOrderChangePaymentMethod;
    @Inject private
    ReviewOrderUpdateQty reviewOrderUpdateQty;
    @Inject private
    ReviewOrderChangeShippingMethod reviewOrderChangeShippingMethod;

    @Inject private
    EnterShippingAddress enterShippingAddress;
    @Inject private
    SelectPaymentMethod selectPaymentMethod;
    @Inject private
    SelectShippingMethod selectShippingMethod;

    @Inject private
    UpdateQty updateQty;

    @Inject private
    ReviewOrder reviewOrder;

    @Inject private
    ReviewOrderMultiPage reviewOrderMultiPage;
    @Inject
    PlaceOrder placeOrder;

    @Inject private
    CantShipToAddress cantShipToAddress;
    @Inject private
    AmazonPrimeAd amazonPrimeAd;
    @Inject private
    AfterOrderPlaced afterOrderPlaced;

    @Inject private
    Login login;

    @Inject private
    ReadOrderDetails readOrderDetails;

    @Inject private GiftOption giftOption;


    public Step detectStep(FlowState state) {


        CheckoutPage page = detectCurrentPage(state);

        switch (page) {
            case AmazonPrimeAd:

            case OrderReview: //we are on order review page
                //if from checkout, which is the first step, then we need to change shipping address
                if (Checkout.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrderChangeShippingAddress;
                }
                //if from shipping address page, then change payment method
                if (EnterShippingAddress.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrderChangePaymentMethod;
                }

                //if from shipping address page, then change payment method
                if (SelectShippingMethod.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrderChangePaymentMethod;
                }

                //if from payment method page, then to update qty
                if (SelectPaymentMethod.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrderUpdateQty;
                }
                //if from update qty page, then to select shipping method
                if (ReviewOrderUpdateQty.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrderChangeShippingMethod;
                }
                if (ReviewOrderChangeShippingMethod.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrderMultiPage;
                }
                return null;
            case ShippingAddressOnePage:
            case ShippingAddress:
                return enterShippingAddress;
            case CantShipToAddressPage:
                return cantShipToAddress;
            case PaymentMethodOnePage:
            case PaymentMethod:
                return selectPaymentMethod;
            case ShippingMethod:
                //jumped here directly after checkout btn clicked, go back to enter shipping method...
                if (Checkout.class.getName().equals(state.getPrevStep().stepName)) {
                    return enterShippingAddress;
                }
                if (SelectShippingMethod.class.getName().equals(state.getPrevStep().stepName)) {
                    return updateQty;
                }
                if (UpdateQty.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrder;
                }
                return selectShippingMethod;
            case ShippingMethodOnePage:
                //jumped here directly after checkout btn clicked, go back to enter shipping method...
                if (Checkout.class.getName().equals(state.getPrevStep().stepName)) {
                    return enterShippingAddress;
                }
                if (UpdateQty.class.getName().equals(state.getPrevStep().stepName)) {
                    return selectShippingMethod;
                }
                if (SelectShippingMethod.class.getName().equals(state.getPrevStep().stepName)) {
                    return reviewOrder;
                }
                return updateQty;
            case AmazonPrimeAdAfterPlaceOrderBtnClicked:
                return amazonPrimeAd;
            case OrderPlacedSuccessPage:
                return afterOrderPlaced;
            case OrderDetailPage:
                return readOrderDetails;
            case LoginPage:
                return login;
            case GiftOptionPage:
                return giftOption;
            default:
                return null;

        }


    }

    public CheckoutPage detectCurrentPage(FlowState state) {
        Browser browser = state.getBuyerPanel().getBrowserView().getBrowser();

        for (int i = 0; i <= 10; i++) {
            try {
                browser = state.getBuyerPanel().getBrowserView().getBrowser();
                CheckoutPage page = CheckoutPage.detectPage(browser);
                if (page != null) {
                    return page;
                }
                WaitTime.Short.execute();
            } catch (Exception e) {
                //
            }

        }


        throw new BusinessException("Cant identify page " + browser.getTitle() + " - " + browser.getURL());

    }

}
