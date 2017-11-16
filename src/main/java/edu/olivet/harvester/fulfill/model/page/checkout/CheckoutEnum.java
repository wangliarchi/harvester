package edu.olivet.harvester.fulfill.model.page.checkout;

import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.utils.JXBrowserHelper;
import lombok.Getter;

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
        CantShipToAddressPage("#changeQuantityFormId"),
        PaymentMethod(".checkout.pay"),
        PaymentMethodOnePage(".a-container.page-container #payment #cc-popover-link"),
        ShippingMethod("#shippingOptionFormId"),
        ShippingMethodOnePage(".a-container.page-container #spc-orders"),
        OrderReview(".checkout.spc"),
        AmazonPrimeAd("#mom-no-thanks")
        ;

        @Getter
        private String idSelector;

        CheckoutPage(String idSelector) {
            this.idSelector = idSelector;
        }

        public static CheckoutPage detectPage(Browser browser) {
            //loading-spinner-blocker-doc
            for (CheckoutPage page : CheckoutPage.values()) {
                if (JXBrowserHelper.selectElementByCssSelector(browser, page.getIdSelector()) != null) {
                    return page;
                }
            }
            throw new BusinessException("Cant detect current page " + browser.getTitle() + " - " + browser.getURL());

        }
    }

    public enum CheckoutPageType {
        OnePage,
        MultiPage
    }
}
