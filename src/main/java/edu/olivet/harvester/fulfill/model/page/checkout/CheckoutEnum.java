package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.utils.pagehelper.GiftOptionHelper;
import edu.olivet.harvester.utils.JXBrowserHelper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/2/17 3:42 PM
 */
public class CheckoutEnum {
    public enum CheckoutStep {
        ShippingAddress,
        PaymentMethod,
        ShippingMethod,
        OrderReview
    }


    public enum CheckoutPage {
        ShippingAddress(".checkout.checkout-as"),
        ShippingAddressOnePage(".a-container.page-container #shipaddress #add-new-address-popover-link"),
        AddressVerification("#AVS"),
        CantShipToAddressPage(
                "#changeQuantityFormId .alertMessage,#changeQuantityFormId .lineitem-error-message,#changeQuantityFormId  a.pipeline-link"),
        PaymentMethod(".checkout.pay"),
        PaymentMethodOnePage(".a-container.page-container #payment #cc-popover-link"),
        ShippingMethod("#shippingOptionFormId"),
        ShippingMethodOnePage(".a-container.page-container #spc-orders"),
        OrderReview(".checkout.spc"),
        AmazonPrimeAd("#mom-no-thanks,#checkout-student-signup-form #student_form_container,.prime-nothanks-button,#prime-no-thanks"),
        AmazonPrimeAdAfterPlaceOrderBtnClicked("#prime-piv-steps-container"),
        OrderPlacedSuccessPage("#a-page .a-box.a-alert.a-alert-success"),
        OrderDetailPage("#orderDetails"),
        LoginPage("#ap_email,#ap_password"),
        GiftOptionPage(GiftOptionHelper.continueBtnSelector);

        @Getter
        private String idSelector;

        CheckoutPage(String idSelector) {
            this.idSelector = idSelector;
        }

        public static CheckoutPage detectPage(Browser browser) {
            JXBrowserHelper.waitUntilNotFound(browser, ".section-overwrap");
            JXBrowserHelper.waitUntilNotFound(browser, "#spinner-anchor");
            JXBrowserHelper.waitUntilNotFound(browser, ".loading-img-text");


            DOMElement primeAd = JXBrowserHelper.selectVisibleElement(browser, "#prime-acquisition-spc-popover-no-thanks");
            if (primeAd != null) {
                primeAd.click();
                WaitTime.Shorter.execute();
                return detectPage(browser);
            }

            for (CheckoutPage page : CheckoutPage.values()) {
                for (String selector : StringUtils.split(page.getIdSelector(), ",")) {
                    DOMElement element = JXBrowserHelper.selectVisibleElement(browser, selector.trim());
                    if (element != null) {
                        return page;
                    }
                }
            }
            return null;

        }
    }

    public enum CheckoutPageType {
        OnePage,
        MultiPage
    }
}
