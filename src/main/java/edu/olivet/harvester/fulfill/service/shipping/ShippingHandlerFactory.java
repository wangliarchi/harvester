package edu.olivet.harvester.fulfill.service.shipping;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.common.model.Order;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/1/17 6:27 PM
 */
public class ShippingHandlerFactory {
    public static ShippingHandler getHandler(Order order) {
        Country fulfillmentCountry = OrderCountryUtils.getFulfillmentCountry(order);
        switch (fulfillmentCountry) {
            case CA:
                return CAHandler.getInstance();
            case UK:
            case DE:
            case FR:
            case ES:
            case IT:
                return UKHandler.getInstance();
            default:
                return DefaultHandler.getInstance();
        }
    }
}
