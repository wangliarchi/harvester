package edu.olivet.harvester.fulfill.service.shipping;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.ui.panel.BuyerPanel;
import edu.olivet.harvester.utils.JXBrowserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/14/17 11:08 AM
 */
public class ShipOptionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipOptionUtils.class);


    public static void selectShipOption(BuyerPanel buyerPanel) {
        Browser browser = buyerPanel.getBrowserView().getBrowser();
        Order order = buyerPanel.getOrder();

        //get shipping options on webpage
        List<DOMElement> options = JXBrowserHelper.selectElementsByCssSelector(browser, ".shipping-speed.ship-option");

        //parse data
        List<ShippingOption> shippingOptions = listAllOptions(browser, buyerPanel.getCountry());

        //get valid option - edd, expedited, price...
        ShippingOption bestOne = ShippingHandlerFactory.getHandler(order).determineShipOption(order, shippingOptions);
        //set order shipping speed;
        order.shippingSpeed = bestOne.getShippingSpeed();
        DOMElement option = options.get(bestOne.getIndex());

        LOGGER.debug("{} shipping options - {},{} is chosen.", shippingOptions.size(), shippingOptions, bestOne);

        option.click();
        WaitTime.Shortest.execute();


    }

    public static List<ShippingOption> listAllOptions(Browser browser, Country country) {
        List<DOMElement> options = JXBrowserHelper.selectElementsByCssSelector(browser, ".shipping-speed.ship-option");

        List<ShippingOption> shippingOptions = new ArrayList<>();
        int index = 0;
        for (DOMElement option : options) {
            index++;
            String eddText;
            String priceText;
            String fullTxt = option.getInnerText().trim();

            try {
                eddText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-success").getInnerText().trim();
            } catch (Exception e) {
                eddText = fullTxt;
            }
            try {
                priceText = JXBrowserHelper.selectElementByCssSelector(option, ".a-color-secondary").getInnerText().trim();

            } catch (Exception e) {
                //LOGGER.error("Error fetch shipping option price {}", option.getInnerHTML());
                priceText = "";
                //continue;
            }


            try {
                ShippingOption shippingOption = new ShippingOption(fullTxt, eddText, priceText, country);
                shippingOption.setIndex(index - 1);
                shippingOptions.add(shippingOption);
            } catch (Exception e) {
                LOGGER.error("Error parse shipping option {}", fullTxt, e);
            }

        }

        return shippingOptions;
    }


}
